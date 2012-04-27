package heinrisch.contact.picture.sync;

import java.io.InputStream;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class ContactHandler {


	public static ArrayList<String> getNumbers(String ID, Context context){
		ArrayList<String> numbers = new ArrayList<String>();
		ContentResolver cr = context.getContentResolver();
		Cursor phones = cr.query(Phone.CONTENT_URI, null,Phone.CONTACT_ID + " = " + ID, null, null);
		while (phones.moveToNext()) {
			numbers.add(phones.getString(phones.getColumnIndex(Phone.NUMBER)));
		}
		phones.close();

		return numbers;

	}

	public static void matchContactsToFriends(ArrayList<Friend> friends, Context context) {
		Cursor people = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		if(people == null){
			Log.e("ContactHandler", "Could not find contacts...?");
			return;
		}
		while(people.moveToNext()) {
			String ID = null,name = null;
			int columnIndex = people.getColumnIndex(ContactsContract.Contacts._ID);
			if(columnIndex != -1) ID = people.getString(columnIndex);
			columnIndex = people.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			if(columnIndex != -1) name = people.getString(columnIndex);

			//ArrayList<String> numbers = getNumbers(ID,context); //"can't" get this from facebook
			if(name == null) continue;

			for(Friend f : friends){
				if(f.isMatchedWithContact()) continue;
				if(f.getName().equals(name)){
					f.setContactID(ID);
					Bitmap contactPhoto = getPhoto(context, ID);
					if(contactPhoto != null) f.setContactPicture(contactPhoto);
					break;
				}
			}

		}
		people.close();
	}

	
	public static void setContactPicture(Friend f, Context context){
		f.setContactPicture(getPhoto(context, f.getContactID()));
	}


	public static int getNumberOfContacts(Context context) {
		Cursor people = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		int numberOfContacts = people.getCount();
		people.close();
		return numberOfContacts;
	}


	public static Uri getPicture(Context context, String ID){
		ContentResolver cr = context.getContentResolver();
		Uri rawContactUri = null;
		Cursor rawContactCursor =  cr.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID}, RawContacts.CONTACT_ID + " = " + ID, null, null);
		if(!rawContactCursor.isAfterLast()) {
			rawContactCursor.moveToFirst();
			rawContactUri = RawContacts.CONTENT_URI.buildUpon().appendPath(""+rawContactCursor.getLong(0)).build();
		}
		rawContactCursor.close();

		return rawContactUri;
	}


	public static void setContactPicture(Context context, String ID, Bitmap picture){
		ContentResolver cr = context.getContentResolver();
		Uri rawContactUri = getPicture(context, ID);
		if(rawContactUri == null){
			Log.e("rawContactUri", "is null");
			return;
		}
		ContentValues values = new ContentValues(); 
		int photoRow = -1; 
		String where = ContactsContract.Data.RAW_CONTACT_ID + " == " + 
				ContentUris.parseId(rawContactUri) + " AND " + Data.MIMETYPE + "=='" + 
				ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'"; 
		Cursor cursor = cr.query(ContactsContract.Data.CONTENT_URI,	null, where, null, null); 
		int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID); 
		if(cursor.moveToFirst()){ 
			photoRow = cursor.getInt(idIdx); 
		} 
		cursor.close(); 
		values.put(ContactsContract.Data.RAW_CONTACT_ID, 
				ContentUris.parseId(rawContactUri)); 
		values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1); 
		values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, Tools.bitmapToByteArray(picture)); 
		values.put(ContactsContract.Data.MIMETYPE, 
				ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE); 
		if(photoRow >= 0){ 
			cr.update(
					ContactsContract.Data.CONTENT_URI, 
					values, 
					ContactsContract.Data._ID + " = " + photoRow, null);
		} else { 
			cr.insert(
					ContactsContract.Data.CONTENT_URI, 
					values); 
		} 
	} 

	public static Bitmap getPhoto(Context context, String contactId) {
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
		if (input == null) {
			return null;
		}
		return BitmapFactory.decodeStream(input);
	}
}

