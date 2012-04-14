package heinrisch.contact.picture.sync;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;


public class Friend {
	private String name;
	private String profilePictureURL;
	private String profilePictureBigURL;
	private String uid;
	private Bitmap profilePicture = null;
	private String contactID; //Mapping friends with local contacts
	private Bitmap contactPicture = null;
	
	
	public String getProfilePictureBigURL() {
		return profilePictureBigURL;
	}

	public void setProfilePictureBigURL(String profilePictureBigURL) {
		this.profilePictureBigURL = profilePictureBigURL;
	}
	
	public String getContactID() {
		return contactID;
	}

	public void setContactID(String contactID) {
		this.contactID = contactID;
	}


	public Friend(JSONObject json) throws JSONException {
        this.name = 				json.getString(Constants.facebook_name);
        this.profilePictureURL = 	json.getString(Constants.facebook_pic_square);
        this.profilePictureBigURL =	json.getString(Constants.facebook_pic_big);
        this.uid = 					json.getString(Constants.facebook_uid);
        contactID = null;
	}


	public CharSequence getName() {
		return name;
	}
	
	public String getProfilePictureURL(){
		return profilePictureURL;
	}


	public void setProfilePic(Bitmap b) {
		this.profilePicture = Bitmap.createScaledBitmap(b, Constants.size_Profile_Picture_Width, Constants.size_Profile_Picture_Heigth, true);
		
	}

	public boolean hasDownloadedProfilePicture() {
		return profilePicture != null;
	}


	public Bitmap getProfilePicture() {
		return profilePicture;
	}
	
	public String getUID(){
		return uid;
	}
	
	@Override
	public boolean equals(Object o) {
		return uid.equals(((Friend)o).uid);
	}

	public boolean isMatchedWithContact() {
		return contactID != null;
	}
	
	@Override
	public String toString() {
		return "Friend: {" + name + ", " + uid  + ", " + contactID + "}";
	}

	public Bitmap getContactPicture() {
		return contactPicture;
	}

	public void setContactPicture(Bitmap contactPicture) {
		this.contactPicture = contactPicture;
	}
	
	public boolean hasContactPicture() {
		return contactPicture != null;
	}
	
}
