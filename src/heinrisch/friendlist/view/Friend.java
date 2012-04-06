package heinrisch.friendlist.view;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;


public class Friend {
	private String name;
	private String profilePictureURL;
	private String uid;
	private Bitmap profilePicture = null;
	
	public Friend(String name){
		this.name = name;
	}
	
	
	public Friend(JSONObject json) throws JSONException {
        this.name = json.getString("name");
        this.profilePictureURL = json.getString("pic_square");
        this.uid = json.getString("uid");
        
	}


	public CharSequence getName() {
		return name;
	}
	
	public String getProfilePictureURL(){
		return profilePictureURL;
	}


	public void setProfilePic(Bitmap b) {
		this.profilePicture = Bitmap.createScaledBitmap(b, 80, 80, true);
		
	}

	public boolean hasDownloadedProfileImage() {
		return profilePicture != null;
	}


	public Bitmap getProfilePicture() {
		return profilePicture;
	}
	
	public String getUID(){
		return uid;
	}
	
}
