package heinrisch.friendlist.view;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendListAdapter extends ArrayAdapter<Friend> {
	private final Context context;
	ArrayList<Friend> friends;

	public FriendListAdapter(Context context, ArrayList<Friend> friends) {
		super(context, R.layout.list_item_friend, friends);
		this.context = context;
		this.friends = friends;
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
		}


		return layout;
	}
}