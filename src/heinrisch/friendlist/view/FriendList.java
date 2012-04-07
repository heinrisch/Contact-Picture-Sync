package heinrisch.friendlist.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;

import com.facebook.android.Facebook;

public class FriendList extends Activity {

	Facebook facebook = new Facebook("424405194241987");

	ProgressDialog dialog;

	ArrayList<Friend> friends;

	FriendListAdapter friendListAdapter;
	ListView friendListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.friend_list);
		//Get facebook access info
		final Intent recieve_intent = getIntent();
		String access_token = recieve_intent.getStringExtra(Constants.bundle_Access_Token);
		long expires = recieve_intent.getLongExtra(Constants.bundle_Access_Expires, 0);
		if(access_token != null) facebook.setAccessToken(access_token);
		if(expires != 0) facebook.setAccessExpires(expires);

		friends = new ArrayList<Friend>();

		//Show dialog to distract user while downloading friends
		dialog = ProgressDialog.show(FriendList.this, "", 
				getString(R.string.downloading_friends_text), true);
		dialog.setCancelable(false);
		dialog.show();


		//Load friends if cached
		String jsonFriends = getFriendsJSONFromCache();
		if(jsonFriends != null){
			parseJSONFriendsToArrayList(jsonFriends,friends);
			dialog.cancel();
			
			for(Friend f : friends){
				Bitmap b = getProfilePictureFromCache(f);
				if(b != null) f.setProfilePic(b);
			}
		}

		friendListAdapter = new FriendListAdapter(FriendList.this, friends);
		friendListView = (ListView) findViewById(R.id.friend_list);
		friendListView.setAdapter(friendListAdapter);


		//Download all friends
		downloadFacebookFriends_async();

	}

	protected void downloadFacebookFriends_async() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				//Get all friends with FQL
				Bundle params = new Bundle();
				params.putString("method", "fql.query");
				params.putString("query", "SELECT name, uid, pic_square FROM user WHERE uid IN (select uid2 from friend where uid1=me()) order by name");

				try {
					String response = facebook.request(params);
					saveFriendsToCache(response);

				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				friendDownloadCompleteHandler.sendEmptyMessage(0);
			}


		}).start();
	}
	
	private void parseJSONFriendsToArrayList(String jsonFriends, ArrayList<Friend> arraylist) {
		try {
			JSONArray array = new JSONArray(jsonFriends);
			for(int i = 0; i < array.length(); i++){
				arraylist.add(new Friend(array.getJSONObject(i)));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

	protected Handler friendDownloadCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			
			if(updateToLatestFriendList() || friends.size() == 0){
				//TODO: should notify user
				friendListAdapter = new FriendListAdapter(FriendList.this, friends);
				friendListView.setAdapter(friendListAdapter);
			}
			
			downloadProfilePictures_async();
			dialog.cancel();

		}
	};


	private void saveFriendsToCache(String friends) {
		File file = new File(getCacheDir(), Constants.cache_JSON_Friends);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(friends.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	protected boolean updateToLatestFriendList() {
		ArrayList<Friend> newFriendList = new ArrayList<Friend>();
		String jsonFriends = getFriendsJSONFromCache();
		parseJSONFriendsToArrayList(jsonFriends, newFriendList);
		
		if(!friendListsAreEqual(friends,newFriendList)){
			friends = newFriendList;
			return true;
		}
		
		return false;
	}

	private boolean friendListsAreEqual(ArrayList<Friend> a, ArrayList<Friend> b) {
		if(a.size() != b.size()) return false;
		
		for(int i = 0; i < a.size(); i++) if(!a.get(i).equals(b.get(i))) return false;
		
		return true;
	}

	private String getFriendsJSONFromCache() {
		File file = new File(getCacheDir(), Constants.cache_JSON_Friends);
		if(!file.exists()) return null;
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String read = null;

			while(null != (read = br.readLine())){
				result += read;
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.equals("") ? null : result;
	}

	protected void downloadProfilePictures_async() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i = 0; i < friends.size(); i++){
					int bestIndex = getBestFriend(i);
					final Friend f = friends.get(bestIndex);

					if(f.hasDownloadedProfilePicture()) continue;

					Bitmap picture = getProfilePictureFromCache(f); 

					if(picture == null){
						picture = Tools.downloadBitmap(f.getProfilePictureURL());
						storeProfilePictureToCache(f,picture);
					}
					f.setProfilePic(picture);

					updateProfilePictureAtIndex(f, bestIndex);

					//need to retry to download the old picture
					if(bestIndex != i) i--;
				}

			}


			//Updates the picture in the listview
			private void updateProfilePictureAtIndex(final Friend f, final int index) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						View v = friendListView.getChildAt(index - friendListView.getFirstVisiblePosition());
						if(v != null){
							ImageView iv = (ImageView) v.findViewById(R.id.profile_picture);
							if(f.hasDownloadedProfilePicture()){
								iv.setImageBitmap(f.getProfilePicture());
							}
						}
					}
				});
			}

			//Simple method to find the best friend to download right now (one that the user is looking at is better than one that is not visible)
			private int getBestFriend(int index) {
				int viewingIndex = friendListView.getFirstVisiblePosition();
				int lastViewingIndex = friendListView.getLastVisiblePosition();
				while(viewingIndex <= lastViewingIndex && viewingIndex <= friends.size()){
					if(!friends.get(viewingIndex).hasDownloadedProfilePicture()){
						index = viewingIndex;
						break;
					}
					viewingIndex++;
				}

				return index;
			}

		}).start();

	}
	
	private Bitmap getProfilePictureFromCache(Friend f) {
		File target = new File(getCacheDir(), f.getUID());
		if(target.exists())
			return BitmapFactory.decodeFile(target.getPath());

		return null;
	}

	private void storeProfilePictureToCache(Friend f, Bitmap picture) {
		File file = new File(getCacheDir(), f.getUID());
		try {
			FileOutputStream out = new FileOutputStream(file);
			picture.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

}
