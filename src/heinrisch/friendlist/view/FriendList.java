package heinrisch.friendlist.view;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

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

		
		//TODO: make this async...
		//Get all friends with FQL
		Bundle params = new Bundle();
		params.putString("method", "fql.query");
		params.putString("query", "SELECT name, uid, pic_square FROM user WHERE uid IN (select uid2 from friend where uid1=me()) order by name");

		
		ArrayList<Friend> friends = new ArrayList<Friend>();
		try {
			String response = facebook.request(params);
			System.out.println(response);

			JSONArray array = new JSONArray(response);
			
			for(int i = 0; i < array.length(); i++){
				System.out.println(array.getJSONObject(i).get("name"));
				friends.add(new Friend(array.getJSONObject(i)));
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		FriendListAdapter adapter = new FriendListAdapter(this, friends);
		setListAdapter(adapter);
		

	}

}
