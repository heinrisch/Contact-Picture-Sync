package heinrisch.contact.picture.sync;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.util.Pair;


public class Friend {
	public static final String SAVING_PROFILEPICTUREHASH = "hash";
	public static final String saving_contactid = "contactid";

	private String name;
	private String profilePictureURL;
	private String profilePictureBigURL;
	private String uid;
	private Bitmap profilePicture = null;
	private String contactID; //Mapping friends with local contacts
	private Bitmap contactPicture = null;
	private int profilePictureSyncHash; //Hashing profilepicture(small) when syncing picture to keep track of which pictures have been synced.


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
		this.profilePicture = Bitmap.createScaledBitmap(b, Constants.size_Profile_Picture, Constants.size_Profile_Picture, true);

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
		if(contactPicture == null) return;
		Pair<Integer, Integer> size = getSmallSize(contactPicture);
		this.contactPicture = Bitmap.createScaledBitmap(contactPicture, size.first, size.second, true);
	}

	public boolean hasContactPicture() {
		return contactPicture != null;
	}
	
	public void savePictureHash(){
		profilePictureSyncHash = profilePicture.hashCode();
	}
	
	public boolean hasSyncedPicture(){
		if(profilePicture == null) return false;
		return profilePictureSyncHash == profilePicture.hashCode();
	}
	
	public String getSaveSyncProfilePictureFileName(){
		return "PICTUREHASH-" + uid;
	}
	
	public String getSaveContactIDFileName(){
		return "CONTACTID-" + uid;
	}
	
	public String getSaveProfiePictureFileName(){
		return "PIC-" + uid;
	}
	

	public Pair<Integer, Integer> getSmallSize(Bitmap b){
		double w = b.getWidth();
		double h = b.getHeight();

		if(w > h){
			double diff = Constants.size_Profile_Picture/w;
			return new Pair<Integer, Integer>(Constants.size_Profile_Picture, (int) (h*diff));
		}else if(w < h){
			double diff = Constants.size_Profile_Picture/h;
			return new Pair<Integer, Integer>((int) (w*diff), Constants.size_Profile_Picture);
		}

		return new Pair<Integer, Integer>(Constants.size_Profile_Picture, Constants.size_Profile_Picture);
	}

	public void unlink() {
		contactID = null;
		profilePictureSyncHash = 0;
	}

}
