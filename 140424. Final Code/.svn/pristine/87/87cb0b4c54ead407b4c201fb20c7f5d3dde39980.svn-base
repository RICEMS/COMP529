package rice.comp529.dias;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Connection extends BroadcastReceiver
{
	private static EventLog m_log, m_events;
	
	public static void setLogs(EventLog log, EventLog events)
	{
		m_log = log;
		m_events = events;
	}
	
	@Override
    public void onReceive(Context context, Intent intent)
	{
		updateStatus(context);
	}
	
	public void updateStatus(Context context)
	{
		ConnectivityManager cm =
		        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
		                      activeNetwork.isConnectedOrConnecting();
		boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
		
		String s = (isConnected?"Connected":"No Connection");
		if (isConnected)
			s += " " + (isWiFi?"WiFi":"Cell");
		if (m_log != null)
			m_log.addEvent(s + "\n");
		if (m_events != null)
			m_events.addEvent(s + "\n");
	}
}
