package heinrisch.contact.picture.sync;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
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

	public static void saveStringToFile(String friends, File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(friends.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getStringFromFile(File file) {
		if(!file.exists()) return null;
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String read = null;

			while(null != (read = br.readLine())){
				result += read;
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.equals("") ? null : result;
	}
	
	
	public static void storePictureToFile(File file, Bitmap picture) {
		try {
			FileOutputStream out = new FileOutputStream(file);
			picture.compress(Bitmap.CompressFormat.PNG, 90, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static Bitmap getBitmapFromFile(File file) {
		if(file.exists())
			return BitmapFactory.decodeFile(file.getPath());

		return null;
	}
	
	public static byte[] bitmapToByteArray(Bitmap bitmap){
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		bitmap.compress(CompressFormat.PNG, 90, baos); 
		return baos.toByteArray();
	}

}
