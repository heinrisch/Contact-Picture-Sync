package heinrisch.friendlist.view;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.android.Facebook;

public class FriendList extends Activity {

	Facebook facebook = new Facebook("424405194241987");
	
	ProgressDialog dialog;
	
	ArrayList<Friend> friends;

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


		if(facebook.isSessionValid()){
			Toast.makeText(this, "So far so good...", Toast.LENGTH_LONG).show();
		}else{
			Toast.makeText(this, "So far... not so good...", Toast.LENGTH_LONG).show();
		}

		
		//Show dialog
		dialog = ProgressDialog.show(FriendList.this, "", 
	            "Getting your friends. Please wait...", true);
		dialog.setCancelable(false);
		dialog.show();

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
				
				/*
				//TODO: make this async...
				for(Friend f : friends){
					Bitmap b = Tools.downloadBitmap(f.getProfilePictureURL());
					Log.i("Downloaded image for:", (String) f.getName());
					f.setProfilePic(b);
				}*/
				
				downloadCompleteHandler.sendEmptyMessage(0);
			}
		}).start();
		
	}
	
	protected Handler downloadCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			FriendListAdapter adapter = new FriendListAdapter(FriendList.this, friends);
			((ListView) findViewById(R.id.friend_list)).setAdapter(adapter);
			dialog.cancel();

		}
	};
}
