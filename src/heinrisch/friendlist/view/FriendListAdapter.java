package heinrisch.friendlist.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.util.Log;
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

	public FriendListAdapter(Context context, ArrayList<Friend> friends) {
		super(context, R.layout.list_item_friend, friends);
		this.context = context;
		this.friends = friends;
		
		//create selection index
		letterIndex = new HashMap<Character, Integer>(); 
		
		for (int i = friends.size()-1; i >0; i--) {
			letterIndex.put(friends.get(i).getName().charAt(0), i); 
		}
		
		Set<Character> keys = letterIndex.keySet();
		
		ArrayList<Character> keyList = new ArrayList<Character>();

		for (Character c : keys) keyList.add(c);

		Collections.sort(keyList);

		sections = new Character[keyList.size()];
		keyList.toArray(sections);
	}
	
	static class ViewHolder {
		public TextView name;
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
			viewHolder.profilePicture = (ImageView) layout.findViewById(R.id.profile_picture);
			layout.setTag(viewHolder);
		}
		
		Friend friend = friends.get(position);
		ViewHolder holder = (ViewHolder) layout.getTag();
		
		holder.name.setText(friend.getName());
		if(friend.hasDownloadedProfileImage()){
			holder.profilePicture.setImageBitmap(friend.getProfilePicture());
		}else{
			holder.profilePicture.setImageResource(R.drawable.mr_unknown);
		}


		return layout;
	}
	


	@Override
	public int getPositionForSection(int section) {
		Character letter = sections[section];

		return letterIndex.get(letter);
	}

	@Override
	public int getSectionForPosition(int position) {
		return 1;
	}

	@Override
	public Object[] getSections() {
		return sections;
	}

}