package heinrisch.contact.picture.sync;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;

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
					break;
				}
			}
			
		}
		people.close();
	}
	


	public static int getNumberOfContacts(Context context) {
		Cursor people = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		int numberOfContacts = people.getCount();
		people.close();
		return numberOfContacts;
	}

}
