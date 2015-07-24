package rice.comp529.dias;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainView extends Fragment
{
	private static final String TAG = "MainView";
    private Activity m_activity;
    private TextView m_batteryText;
    private static Battery m_battery;
    
    public void setBattery(Battery bat)
    {
    	m_battery = bat;
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState)
	{
		m_activity = getActivity();
		
		//  GPS Testing
        Log.i(TAG, "onCreateView");
        
        View view = inflater.inflate(R.layout.activity_main, container, false);

        view.setKeepScreenOn(true);
        
        return view;
	}
	
	@Override
	public void onDestroyView()
	{
		super.onDestroy();
		Log.i(TAG, "MainView.onDestroyView()");
		m_battery.setTextView(null);
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		//  Load the Options
        //GlobalOptions go = GlobalOptions.getOptions(getActivity());
        
    	//  Get the battery text.
    	m_batteryText = (TextView)m_activity.findViewById(R.id.textBattery);
    	m_batteryText.setText(m_battery.getBatteryStatus());
    	m_battery.setTextView(m_batteryText);
     }
	
}

