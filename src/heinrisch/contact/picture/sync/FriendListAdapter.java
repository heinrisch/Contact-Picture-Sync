package heinrisch.contact.picture.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class FriendListAdapter extends ArrayAdapter<Friend> implements SectionIndexer {
	private final Context context;
	ArrayList<Friend> friends;

	//Fastscoll variables
	HashMap<Character, Integer> letterIndex;
	Character[] sections;
	
	//Picture for unknown people
	Bitmap mr_unknown;

	public FriendListAdapter(Context context, ArrayList<Friend> friends) {
		super(context, R.layout.list_item_friend, friends);
		this.context = context;
		this.friends = friends;
		
		Resources r = this.getContext().getResources();
		Bitmap b = BitmapFactory.decodeResource(r, R.drawable.mr_unknown);
		mr_unknown = Bitmap.createScaledBitmap(b, Constants.size_Profile_Picture_Width, Constants.size_Profile_Picture_Heigth, true);
		
		createKeyIndex();
	}

	public void createKeyIndex(){
		letterIndex = new HashMap<Character, Integer>(); 

		for (int i = friends.size()-1; i >0; i--) {
			letterIndex.put(friends.get(i).getName().charAt(0), i); 
		}
		
		sections = letterIndex.keySet().toArray(new Character[letterIndex.size()]);
		
		Arrays.sort(sections);
	}

	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		createKeyIndex();
	}

	//For view recycling (optimization for listview)
	static class ViewHolder {
		public TextView name;
		public TextView matchFound;
		public ImageView profilePicture;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View layout = convertView;
		if(convertView == null){
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layout = inflater.inflate(R.layout.list_item_friend, parent, false);

			ViewHolder viewHolder = new ViewHolder();
			viewHolder.name = (TextView) layout.findViewById(R.id.real_life_name);
			viewHolder.matchFound = (TextView) layout.findViewById(R.id.friend_contact_match_found);
			viewHolder.profilePicture = (ImageView) layout.findViewById(R.id.profile_picture);
			layout.setTag(viewHolder);
		}

		Friend friend = friends.get(position);
		ViewHolder holder = (ViewHolder) layout.getTag();

		holder.name.setText(friend.getName());
		
		if(friend.hasDownloadedProfilePicture()){
			holder.profilePicture.setImageBitmap(friend.getProfilePicture());
		}else{
			holder.profilePicture.setImageBitmap(mr_unknown);
		}

		if(friend.isMatchedWithContact()){
			holder.matchFound.setText(getContext().getString(R.string.friend_contact_match_found));
			holder.matchFound.setTextColor(getContext().getResources().getColor(R.color.green));
		}else{
			holder.matchFound.setText(getContext().getString(R.string.friend_contact_no_match_found));
			holder.matchFound.setTextColor(getContext().getResources().getColor(R.color.red));
		}

		return layout;
	}



	@Override
	public int getPositionForSection(int section) {
		Character letter = sections[section];

		return letterIndex.get(letter);
	}

	@Override
	public Object[] getSections() {
		return sections;
	}

	@Override
	public int getSectionForPosition(int arg0) {
		return 0;
	}

}