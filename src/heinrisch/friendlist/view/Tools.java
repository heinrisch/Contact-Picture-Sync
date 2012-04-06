package heinrisch.friendlist.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Tools {
	public static Bitmap downloadBitmap(String url) {      
		Bitmap bitmap = null;     
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.connect();
			InputStream is = connection.getInputStream();
			bitmap = BitmapFactory.decodeStream(is);
			is.close();
		} catch (IOException e1) {
			// TODO: handle
		}
			
		return bitmap;                
	}

}
