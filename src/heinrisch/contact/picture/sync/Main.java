package heinrisch.contact.picture.sync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class Main extends Activity {

	Facebook facebook = new Facebook(Constants.facebook_appID);

	SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		
		ContactHandler ch = new ContactHandler(this);
		ch.readContacts();

		//Check if we already have an access token
		sharedPreferences = getPreferences(MODE_PRIVATE);
		String access_token = sharedPreferences.getString("access_token", null);
		long expires = sharedPreferences.getLong("access_expires", 0);

		if(access_token != null) facebook.setAccessToken(access_token);
		if(expires != 0) facebook.setAccessExpires(expires);

		ImageButton login = (ImageButton) findViewById(R.id.login_button);
		login.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(facebook.isSessionValid()){
					launchFriendList();
				}else{
					authorizeWithFacebook();
				}

			}
		});

	}

	protected void authorizeWithFacebook() {
		facebook.authorize(this, new DialogListener() {
			@Override
			public void onComplete(Bundle values) {
				//Save access token on successful login
				saveFacebookAccess();
			}

			@Override
			public void onFacebookError(FacebookError error) {
				showError(getString(R.string.login_failed_text) + "\n(" + error.toString() +")");
			}

			@Override
			public void onError(DialogError e) {
				showError(getString(R.string.login_failed_text) + "\n(" + e.toString() +")");
			}

			@Override
			public void onCancel() {
				showError(getString(R.string.login_failed_text) + "\n(" + getString(R.string.login_canceled_text) +")");
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
			showError(getString(R.string.session_no_valid_text));
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

		if(facebook.isSessionValid()){
			saveFacebookAccess();
			launchFriendList();
		}
	}
	
	
	public void showError(String Error){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(Error)
		.setCancelable(false)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		builder.create().show();
	}
}