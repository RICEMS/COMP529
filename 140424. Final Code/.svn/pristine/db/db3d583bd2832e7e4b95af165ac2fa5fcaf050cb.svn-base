package rice.comp529.dias;

import rice.elec529.dias.DIAS;
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
	private static DIAS m_dias = null;
	private static MainActivity m_mainActivity;

	public void setDias(DIAS d, MainActivity ma)
	{
		m_dias = d;
		m_mainActivity = ma;
	}

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

	private void showNodeName(String s)
	{
		TextView tv = (TextView)m_activity.findViewById(R.id.textNodeName);
		if (tv != null)
			tv.setText(s);
	}


	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		//  Load the Options
		GlobalOptions go = GlobalOptions.getOptions(getActivity());
		showNodeName(go.nodeName);

		//  Get the battery text.
		m_batteryText = (TextView)m_activity.findViewById(R.id.textBattery);
		m_batteryText.setText(m_battery.getBatteryStatus());
		m_battery.setTextView(m_batteryText);



		//  Initialize Buttons
		//WEB
		Button WebStartButton = (Button)m_activity.findViewById(R.id.WebStartButton);
		Button WebStopButton = (Button)m_activity.findViewById(R.id.WebStopButton);

		WebStartButton.setOnClickListener(onClickListener);
		WebStopButton.setOnClickListener(onClickListener);

		//EMAIL
		Button EmailStartButton = (Button)m_activity.findViewById(R.id.EmailStartButton);
		Button EmailStopButton = (Button)m_activity.findViewById(R.id.EmailStopButton);

		EmailStartButton.setOnClickListener(onClickListener);
		EmailStopButton.setOnClickListener(onClickListener);

		//Proxy
		Button ProxyStartButton = (Button)m_activity.findViewById(R.id.ProxyStartButton);
		Button ProxyStopButton = (Button)m_activity.findViewById(R.id.ProxyStopButton);

		ProxyStartButton.setOnClickListener(onClickListener);
		ProxyStopButton.setOnClickListener(onClickListener);

		//P2P
		Button P2PStartButton = (Button)m_activity.findViewById(R.id.P2PStartButton);
		Button P2PStopButton = (Button)m_activity.findViewById(R.id.P2PStopButton);

		P2PStartButton.setOnClickListener(onClickListener);
		P2PStopButton.setOnClickListener(onClickListener);

		//SSH
		Button SSHStartButton = (Button)m_activity.findViewById(R.id.SSHStartButton);
		Button SSHStopButton = (Button)m_activity.findViewById(R.id.SSHStopButton);

		SSHStartButton.setOnClickListener(onClickListener);
		SSHStopButton.setOnClickListener(onClickListener);

		//Replicate
		Button ReplicationStartButton = (Button)m_activity.findViewById(R.id.ReplicationStartButton);
		Button ReplicationStopButton = (Button)m_activity.findViewById(R.id.ReplicationStopButton);

		ReplicationStartButton.setOnClickListener(onClickListener);
		ReplicationStopButton.setOnClickListener(onClickListener);

		//Start & Stop ALL
		Button StartALLButton = (Button)m_activity.findViewById(R.id.StartALL);
		Button StopALLButton = (Button)m_activity.findViewById(R.id.StopALL);

		StartALLButton.setOnClickListener(onClickListener);
		StopALLButton.setOnClickListener(onClickListener);




	}

	private OnClickListener onClickListener = new OnClickListener() {


		public void onClick(final View v) {
			switch(v.getId()){
			//Web
			case R.id.WebStartButton:
				Log.i(TAG, "Starting Web Server...");
				if (m_dias != null)  m_dias.startWeb();
				break;
			case R.id.WebStopButton:
				Log.i(TAG, "Stopping Web Server...");
				if (m_dias != null)  m_dias.stopWeb();
				break;
				//Email
			case R.id.EmailStartButton:
				Log.i(TAG, "Starting Email Server...");
				if (m_dias != null)  m_dias.startEmail();
				break;
			case R.id.EmailStopButton:
				Log.i(TAG, "Stopping Email Server...");
				if (m_dias != null)  m_dias.stopEmail();
				break;	
				//Proxy
			case R.id.ProxyStartButton:
				Log.i(TAG,"Starting Proxy Server");
				if (m_dias != null)  m_dias.startProxy();
				break;
			case R.id.ProxyStopButton:
				Log.i(TAG,"Stopping Proxy Server");
				if (m_dias != null)  m_dias.stopProxy();
				break;
				//P2P  
			case R.id.P2PStartButton:
				Log.i(TAG,"Starting P2P Server");
				if (m_dias != null)  m_mainActivity.startP2P();
				break;
			case R.id.P2PStopButton:
				Log.i(TAG,"Stopping P2P Server");
				if (m_dias != null)  m_dias.stopP2P(); // No function stopP2P() in Dias
				break;
				//SSh
			case R.id.SSHStartButton:
				Log.i(TAG,"Starting SSH Server");
				if (m_dias != null)  m_dias.startSSH();
				break;
			case R.id.SSHStopButton:
				Log.i(TAG,"Stopping SSH Server");
				if (m_dias != null)  m_dias.stopSSH();
				m_mainActivity.killSSHdProcess();
				break;
				//Replication
			case R.id.ReplicationStartButton:
				Log.i(TAG,"Starting Replication Server");
				if (m_dias != null)  m_dias.startRsync();
				break;
			case R.id.ReplicationStopButton:
				Log.i(TAG,"Stopping Replication Server");
				if (m_dias != null)  m_dias.stopRsync();
				break;
			case R.id.StartALL:
				Log.i(TAG,"Starting Replication Server");
				if (m_dias != null)  m_mainActivity.startServices();
				break;
			case R.id.StopALL:
				Log.i(TAG,"Stopping Replication Server");
				if (m_dias != null)  m_dias.stopAll();
				break;
			}

		}
	};

}

