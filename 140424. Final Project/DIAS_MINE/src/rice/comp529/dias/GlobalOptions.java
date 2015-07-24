package rice.comp529.dias;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class GlobalOptions implements Serializable
{	
	public boolean doGPS = true;
	public boolean monitorBattery = true;
	public boolean monitorNetwork = true;
	public String nodeName = "ellis.elec529.recg.rice.edu";
	
	static private GlobalOptions m_options = null;
	static private final String TAG = "GlobalOptions";
	static private final long serialVersionUID = 1L;
	
	//  @TODO removd static
	public static String getDirectory()
	{
		return "/sdcard/";
		//String s = "/Android/data/com.example.launchrecorder/files/";
		//return Environment.getExternalStorageDirectory() + "Download/";
		//return s;
	}
		
	static public GlobalOptions getOptions(Activity activity)
	{
		if (m_options == null)
			m_options = loadOptions(activity);
		return m_options;
	}
	
	public void saveOptions(Activity activity)
	{
		try
		{
			FileOutputStream fos = activity.openFileOutput(TAG, Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(this);
			os.close();
			Log.i(TAG, "Options Saved");
		}
		catch (Exception e)
		{
			Log.e(TAG, "Error saving options: " + e.getMessage() + e.toString());
		}
	}
	
	static private GlobalOptions loadOptions(Activity activity)
	{
		try
		{
			FileInputStream fis = activity.openFileInput(TAG);
			ObjectInputStream is = new ObjectInputStream(fis);
			GlobalOptions options = (GlobalOptions)is.readObject();
			is.close();
			return options;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
			Log.e(TAG, "Returning default options.");
			return new GlobalOptions();
		}
	}
}
