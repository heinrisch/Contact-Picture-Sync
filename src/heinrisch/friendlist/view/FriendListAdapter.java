package heinrisch.friendlist.view;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FriendListAdapter extends ArrayAdapter<Friend> {
	private final Context context;
	ArrayList<Friend> friends;

	public FriendListAdapter(Context context, ArrayList<Friend> friends) {
		super(context, R.layout.list_item_friend, friends);
		this.context = context;
		this.friends = friends;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View rowView = inflater.inflate(R.layout.list_item_friend, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.real_life_name);
		//ImageView imageView = (ImageView) rowView.findViewById(R.id.profile_picture);
		textView.setText(friends.get(position).getName());

		return rowView;
	}
}