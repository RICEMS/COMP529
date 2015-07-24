package rice.comp529.dias;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.util.Log;

/**
 * Some Notes:
 * Cool stuff.  Created a TabController class (after going through action bar, third party libs, and deprecated TabHander.
 * Created a dynamic button handler using java reflection.  addButtonAction(resource, this, method name)
 * Sensors, Sensors, Sensors
 * 	Request asynchronous updates for sensor events via a sensor change listener
 *  However, we need synchronous updates for all sensors in one blob of the latest values.
 *  Created a class that receives updates from the various sensors asynchronously up to a period of 25ms time
 *   and saves the value into a resource that is then queried at the desired sampling rate for all sensors.
 * 
 * @author ergiles
 *
 */
public class MainActivity extends Activity
{
	private static final String TAG = "MainActivity";
	private EventsView m_events;
	private LogView m_log;
	private P2PView m_p2p;
	private Battery m_battery;
	
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setTitle("Buenos DIAS");
        Log.i(TAG, "MainActivity.onCreate()");
        setContentView(R.layout.fragmentlayout);
        
        TabController tabs = new TabController(this, R.id.fragment_placeholder);
        //  Main View
        MainView mainView = new MainView();
        tabs.addTab("Main", mainView);
        //  Events
        m_events = new EventsView();
        tabs.addTab("Events", m_events);
        //  Log
        m_log = new LogView();
        tabs.addTab("Log", m_log);
        //  P2P
        //m_p2p = new P2PView();
        //tabs.addTab("P2P", m_p2p);
        //  Options
        tabs.addTab("Options", new OptionsView());
        
        //  Battery Loop
        m_battery = new Battery(this, m_events, m_log);
        mainView.setBattery(m_battery);
        
        Connection.setLogs(m_events, m_log);
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	Log.i(TAG, "MainActivity.onResume()");
    }
    
    @Override
    protected void onPause()
    {
    	super.onPause();
    	Log.i(TAG, "MainActivity.onPause()");
    }
    
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	Log.i(TAG, "MainActivity.onDestroy()");
    	m_battery.stop();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
