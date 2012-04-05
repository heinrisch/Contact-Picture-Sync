package heinrisch.friendlist.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class Main extends Activity {
   
    Facebook facebook = new Facebook("424405194241987");
    
    String TOKENSAVE = "LoginToken";
    
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        //Check if we already have an access token
        sharedPreferences = getPreferences(MODE_PRIVATE);
        String access_token = sharedPreferences.getString("access_token", null);
        long expires = sharedPreferences.getLong("access_expires", 0);
        
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }
        
        
        //Session is valid we are good to go, else we stay at the login screen
        if(facebook.isSessionValid()){
        	
        	
        }
        
        Button login = (Button) findViewById(R.id.login_button);
        login.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				authorizeWithFacebook();
				
			}
		});
        
    }

    protected void authorizeWithFacebook() {
    	facebook.authorize(this, new DialogListener() {
            @Override
            public void onComplete(Bundle values) {
            	//Save access token on successful login
            	SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("access_token", facebook.getAccessToken());
                editor.putLong("access_expires", facebook.getAccessExpires());
                editor.commit();
            	
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

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebook.authorizeCallback(requestCode, resultCode, data);
    }
}