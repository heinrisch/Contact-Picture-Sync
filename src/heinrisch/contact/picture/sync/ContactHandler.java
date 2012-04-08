package heinrisch.contact.picture.sync;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class ContactHandler {

	Context context;

	ArrayList<Contact> contacts;
	HashMap<String, Contact> IDToContactMapper;
	

	public ContactHandler(Context context){
		this.context = context;
		contacts = new ArrayList<ContactHandler.Contact>();
		IDToContactMapper = new HashMap<String, ContactHandler.Contact>();
	}

	
	public void buildDB(){
		readContacts();
	}

	public ArrayList<String> getNumbers(String ID){
		ArrayList<String> numbers = new ArrayList<String>();
		ContentResolver cr = context.getContentResolver();
		Cursor phones = cr.query(Phone.CONTENT_URI, null,Phone.CONTACT_ID + " = " + ID, null, null);
		while (phones.moveToNext()) {
			numbers.add(phones.getString(phones.getColumnIndex(Phone.NUMBER)));
		}
		phones.close();

		return numbers;

	}

	public void readContacts(){
		Cursor people = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

		while(people.moveToNext()) {
			String ID = null,name = null;
			int columnIndex = people.getColumnIndex(ContactsContract.Contacts._ID);
			if(columnIndex != -1) ID = people.getString(columnIndex);
			columnIndex = people.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			if(columnIndex != -1) name = people.getString(columnIndex);

			ArrayList<String> numbers = getNumbers(ID);

			addContact(new Contact(ID, name, numbers));
		}
		people.close();
	}

	private void addContact(Contact contact) {
		contacts.add(contact);
		IDToContactMapper.put(contact.ID, contact);
	}

	class Contact {
		ArrayList<String> numbers;
		String ID,name;

		public Contact(String ID, String name, ArrayList<String> numbers){
			this.ID = ID;
			this.name = name;
			this.numbers = numbers;
		}
	}

	public static void matchContactsToFriends(ArrayList<Friend> friends, Handler upateContractMappingStatusHandler) {
		
	}


	public static int getNumberOfContacts(Context context) {
		Cursor people = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		int numberOfContacts = people.getCount();
		people.close();
		return numberOfContacts;
	}

}
