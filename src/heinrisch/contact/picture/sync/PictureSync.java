package heinrisch.contact.picture.sync;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Facebook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.DownloadListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PictureSync extends Activity{

	Facebook facebook = new Facebook(Constants.facebook_appID);
	ArrayList<SyncObject> syncObjects;
	
	TextView download_name;
	ImageView dowloaded_picture;
	ProgressBar progressbar;
	int progress_counter;
	
	Bitmap lastPicture;
	String lastName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.picture_sync);
		
		syncObjects = new ArrayList<SyncObject>();

		final Intent recieve_intent = getIntent();
		String access_token = recieve_intent.getStringExtra(Constants.bundle_Access_Token);
		long expires = recieve_intent.getLongExtra(Constants.bundle_Access_Expires, 0);
		if(access_token != null) facebook.setAccessToken(access_token);
		if(expires != 0) facebook.setAccessExpires(expires);

		String json = recieve_intent.getStringExtra(Constants.bundle_JSONFriends);

		int numberOfContacs = 0;
		try {
			JSONArray array = new JSONArray(json);
			numberOfContacs = array.length();
			for(int i = 0; i < array.length(); i++){
				syncObjects.add(new SyncObject(array.getJSONObject(i)));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		download_name = (TextView) findViewById(R.id.downloaded_name);
		dowloaded_picture = (ImageView) findViewById(R.id.downloaded_picture);
		
		progressbar = (ProgressBar) findViewById(R.id.progressBar_syncing);
		progressbar.setProgress(0);
		progressbar.setMax(numberOfContacs);
		
		
		startSyncingPicutres_async();


	}
	
	private void startSyncingPicutres_async() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(SyncObject soh : syncObjects){
					lastPicture = Tools.downloadBitmap(soh.url);
					lastName = soh.name;
					if(soh.name.contains("Malin")){
						Log.e("Help!", "Me!");
					}
					updateProgressCounterHandler.sendEmptyMessage(0);
					ContactHandler.setContactPicture(PictureSync.this, soh.contactID, lastPicture);
				}
				
			}
		}).start();
		
	}

	protected Handler updateProgressCounterHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			progressbar.setProgress(++progress_counter);
			dowloaded_picture.setImageBitmap(lastPicture);
			download_name.setText(lastName);
			
		}
	};


	class SyncObject{
		String name,contactID,url;

		public SyncObject(JSONObject json) throws JSONException{
			this.name = json.getString(Constants.facebook_name);
			this.contactID = json.getString(Constants.local_contactID);
			this.url = json.getString(Constants.facebook_pic_big);
		}

	}
}
