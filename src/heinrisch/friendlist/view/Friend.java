package heinrisch.friendlist.view;

import org.json.JSONException;
import org.json.JSONObject;

public class Friend {
	private String name;
	
	public Friend(String name){
		this.name = name;
	}
	
	
	public Friend(JSONObject json) throws JSONException {
		this.name = json.get("name").toString();
	}


	public CharSequence getName() {
		return name;
	}
	
	
}
