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
	//public String nodeName = "";
	public String dom = "elec529.recg.rice.edu";
	public String nodeName = "clay"; //testing only, needs to be "" for deployment so we don't setup keys and other conf improperly
	public String[] replicationNodes = {"ellis","adriana"};
	public String replicationNode = "ellis.elec529.recg.rice.edu";
	
	static private GlobalOptions m_options = null;
	static private final String TAG = "GlobalOptions";
	static private final long serialVersionUID = 2L;
	
	//  @TODO remove static
	public static String getDirectory()
	{
		return "/sdcard/DIAS/";
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
			//FileOutputStream fos = activity.openFileOutput(getDirectory()+TAG, Context.MODE_PRIVATE);
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
			//FileInputStream fis = activity.openFileInput(getDirectory()+TAG);
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
