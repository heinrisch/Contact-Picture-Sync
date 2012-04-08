package heinrisch.contact.picture.sync;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.android.Facebook;

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
					if(soh.url == null || soh.url.equals("")) continue;

					//try to find larger picture
					String largeURL = getLargeProfilePictureURL(soh.uid);

					if(largeURL != null)
						lastPicture = Tools.downloadBitmap(largeURL);
					
					if(largeURL == null || lastPicture == null)
						lastPicture = Tools.downloadBitmap(soh.url);
						
					lastName = soh.name;
					updateProgressCounterHandler.sendEmptyMessage(0);
					ContactHandler.setContactPicture(PictureSync.this, soh.contactID, lastPicture);
				}

				syncingDoneHandler.sendEmptyMessage(0);

			}
		}).start();

	}

	public String getLargeProfilePictureURL(String uid){
		Bundle params = new Bundle();
		params.putString("method", "fql.query");
		params.putString("query", 	"SELECT src_big FROM photo WHERE aid IN (SELECT aid FROM album WHERE owner = "+uid+" AND type = 'profile')  limit 1");							
		String URL = null;
		try {
			String response = facebook.request(params);
			JSONArray array = new JSONArray(response);

			if(array.length() > 0) URL = array.getJSONObject(0).getString("src_big");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return URL;
	}

	protected Handler updateProgressCounterHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			progressbar.setProgress(++progress_counter);
			dowloaded_picture.setImageBitmap(lastPicture);
			download_name.setText(lastName);

		}
	};

	protected Handler syncingDoneHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			PictureSync.this.finish();
		}
	};


	class SyncObject{
		String name,contactID,url,uid;

		public SyncObject(JSONObject json) throws JSONException{
			this.name = json.getString(Constants.facebook_name);
			this.contactID = json.getString(Constants.local_contactID);
			this.url = json.getString(Constants.facebook_pic_big);
			this.uid = json.getString(Constants.facebook_uid);
		}

	}
}
