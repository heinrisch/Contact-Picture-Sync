package heinrisch.contact.picture.sync;



import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.facebook.android.Facebook;
import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class FriendList extends TrackedActivity {

	@SuppressWarnings("deprecation")
	Facebook facebook = new Facebook(Constants.facebook_appID);

	ArrayList<Friend> friends;

	FriendListAdapter friendListAdapter;
	ListView friendListView;

	public Friend activeFriend; //Used for callbacks from contactpicker (change this?)

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friend_list);

		//Get facebook access info
		final Intent recieve_intent = getIntent();
		String access_token = recieve_intent.getStringExtra(Constants.bundle_Access_Token);
		long expires = recieve_intent.getLongExtra(Constants.bundle_Access_Expires, 0);
		if(access_token != null) facebook.setAccessToken(access_token);
		if(expires != 0) facebook.setAccessExpires(expires);

		friends = new ArrayList<Friend>();

		friendListView = (ListView) findViewById(R.id.friend_list);
		friendListView.setOnItemClickListener(new FriendClicker());


		setSyncButtonAction();

		//Download all friends
		downloadFacebookFriends_async();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.friendlist_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_unlink_all:
			for(Friend f : friends) f.unlink();
			friendListAdapter.notifyDataSetChanged();
			EasyTracker.getTracker().trackPageView("/optionUnlinkAll");
			return true;
		case R.id.menu_smartmatch:
			matchContactToFriends_async(false);
			EasyTracker.getTracker().trackPageView("/optionSmartMatch");
			return true;
		case R.id.menu_syncpictures:
			startSyncingActivity(null);
			EasyTracker.getTracker().trackPageView("/optionSyncPictures");
			return true;
		case R.id.menu_savelinks:
			saveAllFriendLinks();
			EasyTracker.getTracker().trackPageView("/optionSaveLinks");
			return true;
		case R.id.menu_loadlinks:
			loadAllFriendLinks();
			friendListAdapter.notifyDataSetChanged();
			EasyTracker.getTracker().trackPageView("/optionLoadLinks");
			return true;
		case R.id.menu_recommend:
			Tools.advertiseOnFacebookWall(facebook, this);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setSyncButtonAction() {
		ImageButton sync = (ImageButton) findViewById(R.id.sync_image);

		sync.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				EasyTracker.getTracker().trackPageView("/buttonSyncPictures");
				showSaveDialogAndSync();
			}
		});
	}

	public void showSaveDialogAndSync() {
		Builder b = new Builder(this);
		b.setMessage(getString(R.string.save_friends_before_sync_question));
		b.setCancelable(false);
		b.setPositiveButton(R.string.yes_text, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogIn, int id) {
				saveAllFriendLinks();
				startSyncingActivity(null);
			}
		});
		b.setNegativeButton(R.string.no_text, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogIn, int id) {
				startSyncingActivity(null);
			}
		});
		b.show();

	}

	@SuppressWarnings("deprecation")
	protected void startSyncingActivity(Friend friend) {
		JSONArray jsonFriends = new JSONArray();
		if (friend == null) {
			for(Friend f : friends) {
				if(f.isMatchedWithContact()){
					try {
						JSONObject obj = new JSONObject();
						obj.put(Constants.facebook_name, f.getName());
						obj.put(Constants.local_contactID, f.getContactID());
						obj.put(Constants.facebook_pic_big, f.getProfilePictureBigURL());
						obj.put(Constants.facebook_uid, f.getUID());
						jsonFriends.put(obj);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			try {
	 			JSONObject obj = new JSONObject();
				obj.put(Constants.facebook_name, friend.getName());
				obj.put(Constants.local_contactID, friend.getContactID());
				obj.put(Constants.facebook_pic_big, friend.getProfilePictureBigURL());
				obj.put(Constants.facebook_uid, friend.getUID());
				jsonFriends.put(obj);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		if(jsonFriends.length() < 1){
			Tools.showError(getString(R.string.no_friend_selected), FriendList.this);
			return;
		}

		Intent i = new Intent(FriendList.this,PictureSync.class);
		i.putExtra(Constants.bundle_JSONFriends, jsonFriends.toString());
		i.putExtra(Constants.bundle_Access_Token, facebook.getAccessToken());
		i.putExtra(Constants.bundle_Access_Expires, facebook.getAccessExpires());
		startActivityForResult(i, Constants.activity_result_PICTURE_SYNC_RESULT);
	}

	public void saveAllFriendLinks() {
		for(Friend f : friends){
			if(f.isMatchedWithContact()){
				Tools.saveStringToFile(f.getContactID(), new File(getCacheDir(), f.getSaveContactIDFileName()));
			}else{
				Tools.saveStringToFile("", new File(getCacheDir(), f.getSaveContactIDFileName()));
			}
		}
	}

	public void loadAllFriendLinks() {
		for(Friend f : friends){
			File file = new File(getCacheDir(), f.getSaveContactIDFileName());
			String ID = Tools.getStringFromFile(file);
			if(ID == null || ID.equalsIgnoreCase("")) continue;
			f.setContactID(ID);
			ContactHandler.setContactPicture(f,this);
		}
	}


	//Download friends and put them in cache
	protected void downloadFacebookFriends_async() {
		new AsyncTask<Void, Void, Void>() {
			ProgressDialog d;
			
			protected void onPreExecute() {
				d = new ProgressDialog(FriendList.this);
				d.setMessage(FriendList.this.getString(R.string.downloading_friends_text));
				d.show();
			};

			@SuppressWarnings("deprecation")
			@Override
			protected Void doInBackground(Void... args) {
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

				EasyTracker.getTracker().trackEvent(
						"Event",  // Category
						"Download Complete",  // Action
						"Number of Friends", // Label
						friends.size());
				return null;
			}
			
			protected void onPostExecute(Void result) {
				d.dismiss();
				
				friendListAdapter = new FriendListAdapter(FriendList.this, friends);
				friendListView.setAdapter(friendListAdapter);

				//Fetch the progressbar so that it can be update
				matchContactToFriends_async(true);
			};
			
		}.execute((Void) null);
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

	protected Handler friendContactMappingCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			

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

					File file = new File(getCacheDir(), f.getSaveProfiePictureFileName());
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

	protected void matchContactToFriends_async(final boolean loadLinks) {
		new AsyncTask<Void, Void, Void>() {
			ProgressDialog d;
			
			protected void onPreExecute() {
				d = new ProgressDialog(FriendList.this);
				d.setMessage(FriendList.this.getString(R.string.getting_your_contacts_text));
				d.show();
			};

			@Override
			protected Void doInBackground(Void... args) {
				ContactHandler.matchContactsToFriends(friends,FriendList.this);
				if(loadLinks) loadAllFriendLinks();
				return null;
			}
			
			protected void onPostExecute(Void result) {
				d.dismiss();
				
				downloadProfilePictures_async();
				friendListAdapter.notifyDataSetChanged();
			};
		}.execute((Void) null);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode != Activity.RESULT_OK) return;

		if(requestCode == Constants.activity_result_CONTACT_PICKER_RESULT && activeFriend != null){
			EasyTracker.getTracker().trackPageView("/onActivityResultContactPicked");
			Uri result = data.getData(); 
			String id = result.getLastPathSegment();
			activeFriend.setContactID(id);
			activeFriend.setContactPicture(ContactHandler.getPhoto(this, id));
			activeFriend = null;

			friendListAdapter.notifyDataSetChanged(); //should only update one post...
		}else if(requestCode == Constants.activity_result_PICTURE_SYNC_RESULT){
			for(Friend f : friends){
				if(f.isMatchedWithContact()){
					f.savePictureHash();
				}
			}
			friendListAdapter.notifyDataSetChanged();
			if(data.getBooleanExtra(Constants.bundle_advertise, false)){
				Tools.advertiseOnFacebookWall(facebook, this);
			}
		}else{
			EasyTracker.getTracker().trackPageView("/onActivityResultFailed");
		}

	}

	class FriendClicker implements OnItemClickListener{

		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long id){
			Context context= FriendList.this;
			final Friend friend = friends.get(position);
			
			Builder b = new Builder(context);
			b.setTitle(friend.getName());
			String[] items;
			if (friend.isMatchedWithContact()) {
				items = new String[] {
						context.getString(R.string.link_friend_text),
						context.getString(R.string.unlink_friend_text),
						context.getString(R.string.sync_this_friend)
						};
			} else {
				items = new String[] {
						context.getString(R.string.link_friend_text)
						};
			}
			b.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						EasyTracker.getTracker().trackPageView("/buttonLinkFriend");
						activeFriend = friend; // save for callback
						Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);  
						contactPickerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivityForResult(contactPickerIntent, Constants.activity_result_CONTACT_PICKER_RESULT); 
						return;
					case 1:
						EasyTracker.getTracker().trackPageView("/buttonUnlinkFriend");
						friend.unlink();
						friendListAdapter.notifyDataSetChanged(); // should only update one post...
						return;
					case 2:
						startSyncingActivity(friend);
						return;
					default:
						throw new IllegalStateException("illegal item selected");
					}
				}
			});
			b.show();
		}
	}
}