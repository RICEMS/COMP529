package rice.comp529.dias;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.TextView;

public class Battery extends BroadcastReceiver
{
	private static Activity m_activity;
	private static EventLog m_events, m_log;
	private static String m_status = "";
	private static final String TAG = "Battery";
    private static TextView m_batteryText;
    private UpdateLoop m_updater;
	
	public Battery(Activity act, EventLog events, EventLog log)
	{
		m_activity = act;
		m_events = events;
		m_log = log;

		m_updater = new UpdateLoop(10000)
		{
			public void update()
			{
				updateBattery(null);
			}
		};
	}
	
	@Override
    public void onReceive(Context context, Intent intent)
	{ 
		updateBattery(intent);
	}
	
	public void updateBattery(Intent intent)
	{
		String s="";
		boolean logEvent = false;
		if (intent == null)
		{
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			intent = m_activity.getApplicationContext().registerReceiver(null, ifilter);
			
		}
		if (intent == null)
			return;
		try
		{
			
			// Are we charging / charged?
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
					status == BatteryManager.BATTERY_STATUS_FULL;
			
			if (isCharging)
			{
				// How are we charging?
				int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
				//boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
				if (usbCharge)
				{
					s += "USB Charging ";
				}
				else
				{
					s += "AC Charging ";
				}
			}
			Log.i(TAG,"2");
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			Log.i(TAG,"scale "+scale);
			float batteryPct = level;
			batteryPct *= 100.0;
			batteryPct /= (float)scale;
			DecimalFormat df = new DecimalFormat("###.##");
			s += df.format(batteryPct) + "%";
			Log.i(TAG,s);
			
			//  Log all battery updates for now...
			//  TODO move to below m_status comparison.
			if (m_log != null)
				m_log.addEvent(s);
			if (!s.equalsIgnoreCase(m_status))
			{
				m_status = s;
				if (m_batteryText != null)
				  m_batteryText.setText(s);
				if (logEvent && (m_events != null))
					m_events.addEvent(s);
			}
		}
		catch (Exception e)
		{
			if ((e != null) && (e.getMessage() != null))
				Log.e(TAG, e.getMessage());
		}
	}
	
	public String getBatteryStatus()
	{
		return m_status;
	}
	
	public void setTextView(TextView tv)
	{
		m_batteryText = tv;
	}
	public void stop()
	{
		m_updater.stop();
	}
}

