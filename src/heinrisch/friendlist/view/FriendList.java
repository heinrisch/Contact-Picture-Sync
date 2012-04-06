package heinrisch.friendlist.view;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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


		//Show dialog to distract user while downloading friends
		dialog = ProgressDialog.show(FriendList.this, "", 
				getString(R.string.downloading_friends_text), true);
		dialog.setCancelable(false);
		dialog.show();


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


				friends = new ArrayList<Friend>();
				try {
					String response = facebook.request(params);
					System.out.println(response);

					JSONArray array = new JSONArray(response);

					for(int i = 0; i < array.length(); i++){
						System.out.println(array.getJSONObject(i).get("name") +  " " + array.getJSONObject(i).getString("pic_square"));
						friends.add(new Friend(array.getJSONObject(i)));
					}

				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e){
					e.printStackTrace();
				}

				downloadCompleteHandler.sendEmptyMessage(0);
			}
		}).start();
	}

	protected Handler downloadCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			friendListAdapter = new FriendListAdapter(FriendList.this, friends);

			friendListView = (ListView) findViewById(R.id.friend_list);
			friendListView.setAdapter(friendListAdapter);

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

					if(f.hasDownloadedProfileImage()) continue;

					Bitmap b = Tools.downloadBitmap(f.getProfilePictureURL());
					Log.i("Downloaded image for:", (String) f.getName());
					f.setProfilePic(b);

					//Finally we need to uptade the picture
					final int index = bestIndex;
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							View v = friendListView.getChildAt(index - friendListView.getFirstVisiblePosition());
							if(v != null){
								ImageView iv = (ImageView) v.findViewById(R.id.profile_picture);
								if(f.hasDownloadedProfileImage()){
									iv.setImageBitmap(f.getProfilePicture());
								}
							}
						}
					});

					//need to retry to download the old picture
					if(bestIndex != i) i--;
				}

			}

			//Simple method to find the best friend to download right now (one that the user is looking at is better than one that is not visible)
			private int getBestFriend(int index) {
				int viewingIndex = friendListView.getFirstVisiblePosition();
				int lastViewingIndex = friendListView.getLastVisiblePosition();
				while(viewingIndex <= lastViewingIndex && viewingIndex <= friends.size()){
					if(!friends.get(viewingIndex).hasDownloadedProfileImage()){
						index = viewingIndex;
						break;
					}
					viewingIndex++;
				}

				return index;
			}

		}).start();

	}


}
