package heinrisch.friendlist.view;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.facebook.android.Facebook;

public class FriendList extends ListActivity {

	Facebook facebook = new Facebook("424405194241987");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);


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


	}

}
