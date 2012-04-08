package heinrisch.contact.picture.sync;



import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.facebook.android.Facebook;

public class FriendList extends Activity {

	Facebook facebook = new Facebook(Constants.facebook_appID);

	Dialog dialog;

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
		dialog = new ProgressDialog(this);
		dialog.show();
		dialog.setContentView(R.layout.custom_progress_dialog_downloading_friends);

		friendListView = (ListView) findViewById(R.id.friend_list);


		setSyncButtonAction();

		//Download all friends
		downloadFacebookFriends_async();

	}

	private void setSyncButtonAction() {
		ImageButton sync = (ImageButton) findViewById(R.id.sync_image);

		sync.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//Pack content and send to picturesync
				JSONArray jsonFriends = new JSONArray();
				for(Friend f : friends) {
					if(f.isMatchedWithContact()){
						try {
							JSONObject obj = new JSONObject();
							obj.put(Constants.facebook_name,f.getName());
							obj.put(Constants.local_contactID,f.getContactID());
							obj.put(Constants.facebook_pic_big,f.getProfilePictureBigURL());
							jsonFriends.put(obj);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				Intent i = new Intent(FriendList.this,PictureSync.class);
				i.putExtra(Constants.bundle_JSONFriends, jsonFriends.toString());
				i.putExtra(Constants.bundle_Access_Token, facebook.getAccessToken());
				i.putExtra(Constants.bundle_Access_Expires, facebook.getAccessExpires());
				startActivity(i);
			}
		});
	}

	//Download friends and put them in cache
	protected void downloadFacebookFriends_async() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				//Get all friends with FQL
				Bundle params = new Bundle();
				params.putString("method", "fql.query");
				String elements = Constants.facebook_name + ", " + Constants.facebook_uid + ", " + Constants.facebook_pic_square + ", " + Constants.facebook_pic_big;
				params.putString("query", "SELECT "+ elements + " FROM user WHERE uid IN (select uid2 from friend where uid1=me()) order by name");

				try {
					String response = facebook.request(params);
					parseJSONFriendsToArrayList(response, friends);

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

			friendListAdapter = new FriendListAdapter(FriendList.this, friends);
			friendListView.setAdapter(friendListAdapter);

			dialog.setContentView(R.layout.custom_progress_dialog_getting_contacts);

			//Fetch the progressbar so that it can be update
			matchContactToFriends_async();
		}
	};

	protected Handler friendContactMappingCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			downloadProfilePictures_async();
			dialog.cancel();

		}
	};

	protected void downloadProfilePictures_async() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i = 0; i < friends.size(); i++){
					int bestIndex = getBestFriend(i);
					final Friend f = friends.get(bestIndex);

					if(f.hasDownloadedProfilePicture()) continue;

					File file = new File(getCacheDir(), f.getUID());
					Bitmap picture = Tools.getBitmapFromFile(file); 

					if(picture == null){
						picture = Tools.downloadBitmap(f.getProfilePictureURL());
					}
					if(picture != null){
						f.setProfilePic(picture);
						Tools.storePictureToFile(file, picture);
					}

					updateProfilePictureAtIndex(f, bestIndex);

					//need to retry to download the old picture
					if(bestIndex != i) i--;
				}

			}

			//Updates the picture in the listView
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

	protected void matchContactToFriends_async() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				ContactHandler.matchContactsToFriends(friends,FriendList.this);
				friendContactMappingCompleteHandler.sendEmptyMessage(0);
			}
		}).start();

	}

}