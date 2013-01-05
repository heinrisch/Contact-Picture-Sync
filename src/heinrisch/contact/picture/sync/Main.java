package heinrisch.contact.picture.sync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;

import com.bugsense.trace.BugSenseHandler;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.google.android.apps.analytics.easytracking.EasyTracker;
import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class Main extends TrackedActivity {

	Facebook facebook = new Facebook(Constants.facebook_appID);

	SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		BugSenseHandler.setup(this, Constants.bugsense_appID);

		//Check if we already have an access token
		sharedPreferences = getPreferences(MODE_PRIVATE);
		String access_token = sharedPreferences.getString("access_token", null);
		long expires = sharedPreferences.getLong("access_expires", 0);

		if(access_token != null) facebook.setAccessToken(access_token);
		if(expires != 0) facebook.setAccessExpires(expires);
		
		if (facebook.isSessionValid()) {
			launchFriendList();
			finish();
		} else {
			ImageButton login = (ImageButton) findViewById(R.id.login_button);
			login.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(facebook.isSessionValid()){
						launchFriendList();
					} else{
						authorizeWithFacebook();
					}
				}
			});
		}
	}

	protected void authorizeWithFacebook() {
		facebook.authorize(this, new String[] { "friends_photos" }, new DialogListener() {
			@Override
			public void onComplete(Bundle values) {
				//Save access token on successful login
				EasyTracker.getTracker().trackPageView("/actionLoginComplete");
				saveFacebookAccess();
				launchFriendList();
			}

			@Override
			public void onFacebookError(FacebookError error) {
				EasyTracker.getTracker().trackPageView("/actionFacebookError");
				Tools.showError(getString(R.string.login_failed_text) + "\n(" + error.toString() +")",Main.this);
			}

			@Override
			public void onError(DialogError e) {
				EasyTracker.getTracker().trackPageView("/actionDialogError");
				Tools.showError(getString(R.string.login_failed_text) + "\n(" + e.toString() +")",Main.this);
			}

			@Override
			public void onCancel() {
				EasyTracker.getTracker().trackPageView("/actionCancel");
				Tools.showError(getString(R.string.login_failed_text) + "\n(" + getString(R.string.login_canceled_text) +")",Main.this);
			}
		});
	}

	protected void saveFacebookAccess() {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("access_token", facebook.getAccessToken());
		editor.putLong("access_expires", facebook.getAccessExpires());
		editor.commit();
	}


	public void launchFriendList(){
		if(!facebook.isSessionValid()){
			Tools.showError(getString(R.string.session_no_valid_text),this);
			return;
		}

		Intent i = new Intent(Main.this, FriendList.class);
		i.putExtra(Constants.bundle_Access_Token, facebook.getAccessToken());
		i.putExtra(Constants.bundle_Access_Expires, facebook.getAccessExpires());
		startActivity(i);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		facebook.authorizeCallback(requestCode, resultCode, data);
	}

}