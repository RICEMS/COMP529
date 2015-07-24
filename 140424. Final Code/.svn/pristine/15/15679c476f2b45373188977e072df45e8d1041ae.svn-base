package rice.comp529.dias;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.text.format.Formatter;
import android.util.Log;
import rice.elec529.dias.DIAS;
import rice.elec529.dias.Service;
import rice.elec529.dias.examples.LogFixture;

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
 * @author ergiles, Clay Shepard
 *
 */
public class MainActivity extends Activity
{
	private static final String TAG = "MainActivity";
	private EventsView m_events;
	private LogView m_log;
	private DIAS m_dias;
	LogAndroid m_androidLog;
	private P2PView m_p2p;
	private DiasWebView m_browser;
	private Battery m_battery;
	String[] binFiles = {"ssh","sshd","sshd_config","busybox","scp","rsync","sftp","sshelper_sshd","ssh-keygen","ssh-keyscan","mksh", "dropbearconvert", "rsyncdias.sh"};
	//String binPath = "/data/data/" + this.getPackageName() + "/bin/";
	String binPath = "/data/data/rice.comp529.dias/bin/";
	String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DIAS/";
	GlobalOptions g;
	String node = "";  //should only used to detect of GlobalOptions.nodeName changed.
	String users[] = {"adriana", "clay", "ellis"};
	
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
		OptionsView opt = new OptionsView();
		opt.setMainActivity(this);
		tabs.addTab("CONF", opt);
		//  Browse
		DiasWebView.m_port = DIAS.JSocksPort;
		m_browser = new DiasWebView();
		tabs.addTab("Browse",  m_browser);
		g = GlobalOptions.getOptions(this);
		node = g.nodeName;
		/*
        LogFixture log = new LogFixture();
		Service.log = log;*/
		binPath = "/data/data/" + this.getPackageName() + "/bin/"; //... for some reason this.getPackageName returns "" unless it's in a method...

		File f = new File(basePath + ".ssh/");
		f.mkdirs(); //create the directory if it doesn't exist.
		f = new File(binPath);
		f.mkdirs(); //create the directory if it doesn't exist.
		for (String user : users) {
			f = new File(basePath + "users/"+user+"/www");
			f.mkdirs(); //create the directory if it doesn't exist.	
		}

		copyAssetBinaries(binFiles, binPath); //we should make this check to see if they already exist... but oh well.
		
		setupKeys(false);
		//Apparently there isn't a way to make symlinks in Android Java... (usually you use java.nio.file.Files)
		//I think this will create the symlink we need... but if not, check the SSHelperApplication, but it basically just runs an exec ln -s:
		/*try{
			Runtime.getRuntime().exec(binPath + "busybox --install -s " + binPath);
		} catch (IOException e) {Log.e(TAG, "I/O Exception in copyAssetBinaries()", e);}*/
		runNative(binPath + "busybox --install -s " + binPath); 

		//Log.i(TAG,"ls -al " + binPath + ":\n" + runNative("ls -al " + binPath));

		//test dns --this works!
		//setDNS("java.elec529.recg.rice.edu","168.7.138.32");
		
		//register for network change notifications
		//registerReceivers();
		
		//  Battery Loop
		m_battery = new Battery(this, m_events, m_log);
		mainView.setBattery(m_battery);

		Connection.setLogs(m_events, m_log);
		m_androidLog = new LogAndroid(m_events, m_log);
		m_dias = new DIAS(m_androidLog, g.nodeName, false, binPath, basePath);
		Service.log = m_androidLog;
		mainView.setDias(m_dias, this);
		//let's start them with the buttons now
		//if (!g.nodeName.equals(""))
		//	startServices();
		
		//kill any running sshd processes
		killSSHdProcess();
		
		//try to exit gracefully (especially so that SSH stops.
		Runtime.getRuntime().addShutdownHook(new Thread() {
		      public void run() {
		    	  Log.i(TAG,"Running Shutdown Hook");
		    	  m_dias.stopAll();
		      }
		    });
	}
	
	private void setupKeys(boolean overwrite) {
	
		if (g.nodeName.equals(""))
			return;
		if (overwrite) {
			Log.i(TAG,"Called setupKeys() with overwrite; deleting existing keys and authorized_hosts...");
			Log.i(TAG,runNative(binPath+"rm -rf " + basePath + ".ssh/*"));  //hrm, this doesn't work -- maybe we should do /system/bin?
			//Log.i(TAG,runNative("/system/bin/rm" + basePath + ".ssh/*")); 
		}
			
		//Generate the SSH keys if we need to.  Note that for recovering an existing user, they should put these keys in manually...
		File f = new File(basePath + ".ssh/id_rsa");
		if(!f.exists() || overwrite) {
			//Log.i(TAG,runNative(binPath+"wget -r -O .ssh/id_rsa http://elec529.recg.rice.edu/"+node, basePath + ".ssh/"));  //we can do something like this to get the user conf -- it is much easier than HttpClient anyway...
			Log.i(TAG,"Downloading SSH Keys...");
			for (String s : new String[]{"id_rsa","id_rsa.pub","id_dsa","id_dsa.pub","id_ecdsa","id_ecdsa.pub","authorized_keys"})
				Log.i(TAG,runNative(binPath+"wget http://elec529.recg.rice.edu/"+g.nodeName+"/"+s + " -O " + basePath  + ".ssh/" + s)); //-P doesn't overwrite existing files, but this will.
			
			//unfortunate, the busybox version of wget doesn't do recursive
			//Log.i(TAG,runNative(binPath+"wget --no-dwirectories -r -R index.html* -R *.gif http://elec529.recg.rice.edu/"+g.nodeName+"/ -P " + basePath + ".ssh/")); //wget prints to stderr
						
			File n = new File(basePath + ".ssh/id_rsa");
			if(!n.exists()) {
				Log.w(TAG,"Error downloading SSH Keys! Generating new ones...");
				Log.i(TAG,runNative(binPath+"ssh-keygen -b 4096 -t rsa -f " + basePath + ".ssh/id_rsa -Y -q"));
				Log.i(TAG,runNative(binPath+"ssh-keygen -t dsa -f " + basePath + ".ssh/id_dsa -Y -q"));	
				Log.i(TAG,runNative(binPath+"ssh-keygen -t ecdsa -f " + basePath + ".ssh/id_ecdsa -Y -q"));		
			}
		}
		
		//the openssh ssh version had problems with public key auth.  dropbear ssh worked, but requires a different key format.
		//do it separately from keygen so we don't have to copy both versions on recovery.
		f = new File(basePath + "ssh/id_rsa.dropbear");
		if(!f.exists() || overwrite) {
			Log.i(TAG,"Converting keys for dropbear ssh client (dbclient)...");
			Log.i(TAG,runNative(binPath+"dropbearconvert openssh dropbear " + basePath + ".ssh/id_rsa " + basePath + ".ssh/id_rsa.dropbear"));	
			Log.i(TAG,runNative(binPath+"dropbearconvert openssh dropbear " + basePath + ".ssh/id_dsa " + basePath + ".ssh/id_dsa.dropbear"));
		}	
	}
	protected void startP2P()
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				m_dias.startP2P(getPastryReader());
			}
		};
		Thread t = new Thread(r);
		t.start();
	}

	protected void startServices()
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				m_dias.startServices(getPastryReader());
			}
		};
		Thread t = new Thread(r);
		t.start();
	}

	protected void saveOptions()
	{
		m_androidLog.logEvent("Saving Options");
		g.getOptions(this);  //double check... do we even need this?
		g.saveOptions(this);
		m_androidLog.logEvent("Saved.");
		m_androidLog.logEvent("Restarting services.");
		m_dias.stopAll();
		if(!node.equals(g.nodeName)) { //check if the nodeName changed, if so force a rewrite of the keys
			node = g.nodeName;
			setupKeys(true);
		}
		m_dias.setNodeName(g.nodeName);
		startServices();
		m_androidLog.logEvent("Done Saving.");
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
		m_dias.stopAll();
		Log.i(TAG, "MainActivity.onDestroy()");
		m_battery.stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public Reader getPastryReader()
	{
		try {
			return new InputStreamReader(this.getAssets().open("freepastry.params"));
		} catch (IOException e) {
			m_androidLog.log("error reading pastry file");
			// TODO Auto-generated catch block
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			m_androidLog.log(errors.toString());
		}
		return null;
	}

	/*
	 * Copy the executable asset files to a place we can run them.
	 */
	private void copyAssetBinaries(String[] binaries, String dir) {
		AssetManager assetManager = this.getAssets();

		for (String asset : binaries) {
			try {
				InputStream in = assetManager.open(asset);
				OutputStream out = new FileOutputStream( dir + asset);  

				byte[] buffer = new byte[1024];
				int read;
				while ((read = in.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				in.close();
				out.flush();
				out.close();

				File f = new File(dir + asset);
				f.setExecutable(true);

			} catch (IOException e) {
				Log.e(TAG, "I/O Exception in copyAssetBinaries()", e);
			}
		}
	}
	public String runNative (String name) {
		return runNative (name, null);
	}
	//run a native command and return the output.
	public String runNative (String name, String dir) {
		String output = "";
		try {
			Process p;
			if(dir == null)
				p = Runtime.getRuntime().exec(name);
			else
				p = Runtime.getRuntime().exec(name, new String[]{}, new File(dir));	
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));  
			BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));  			
			String line;
			String errLine = null;
			while (((line = reader.readLine()) != null)  || ((errLine = errReader.readLine()) != null)) {
				if (errLine != null)
					output += "err: " + errLine + "\n";
				else
					output += line + "\n";
				//Log.i(TAG,line);  //if the output is too long, you need to print it here...
			}
		} catch (Exception e) {Log.e(TAG, "Exception in runNative()", e);}
		return output;        
	}
	
	private void disableStrict()
	{
		// ICS: 4.0
	    if (Build.VERSION.SDK_INT >= 15)
	    {   
	    	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    	StrictMode.setThreadPolicy(policy);
	    	return;
	    }

		try {
		Class<?> strictModeClass = Class.forName("android.os.StrictMode", true, Thread.currentThread().getContextClassLoader());
		Class<?> threadPolicyClass = Class.forName("android.os.StrictMode$ThreadPolicy", true, Thread .currentThread().getContextClassLoader());
		Class<?> threadPolicyBuilderClass = Class.forName("android.os.StrictMode$ThreadPolicy$Builder", true, Thread.currentThread().getContextClassLoader());
		Method setThreadPolicyMethod = strictModeClass.getMethod("setThreadPolicy", threadPolicyClass);
		Method detectAllMethod = threadPolicyBuilderClass.getMethod("detectAll");
		Method penaltyMethod = threadPolicyBuilderClass.getMethod("penaltyLog");
		Method buildMethod = threadPolicyBuilderClass.getMethod("build");
		Constructor<?> threadPolicyBuilderConstructor = threadPolicyBuilderClass.getConstructor();
		Object threadPolicyBuilderObject = threadPolicyBuilderConstructor.newInstance();
		Object obj = detectAllMethod.invoke(threadPolicyBuilderObject);
		obj = penaltyMethod.invoke(obj);
		Object threadPolicyObject = buildMethod.invoke(obj);
		setThreadPolicyMethod.invoke(strictModeClass, threadPolicyObject);
		}
		catch (Exception ex) {
		Log.w("disableStrictMode", ex);
		}
	}
	
	//add (or change) host to point to ip in the dns server
	//untested, but should work if the paths and keys are right...
	public void setDNS(String host, String ip){		
		Log.i(TAG,"Setting DNS to:" + host +" " + ip + ": " +runNative(binPath+"ssh -y -i " + basePath + ".ssh/id_rsa.dropbear root@elec529.recg.rice.edu /root/setdns.sh " + host + " " + ip));		
	}	
	
	
	//http://thiranjith.com/2011/03/31/how-to-monitor-network-connectivity-in-android/
	//also: http://stackoverflow.com/questions/10683495/android-how-to-know-an-ip-address-is-a-wifi-ip-address
	//untested.  Not sure how it works with vpn
    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);

	        //NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
	        //NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

	        NetworkInfo currentNetworkInfo = connMgr.getActiveNetworkInfo();
	        NetworkInfo[] otherNetworkInfo = connMgr.getAllNetworkInfo();
	        final NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	        final NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
	        String IP;
	        if (wifi.isAvailable()) {

	            WifiManager myWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
	            WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
	            int ipAddress = myWifiInfo.getIpAddress();
	            IP = Formatter.formatIpAddress(ipAddress);
	            //System.out.println("WiFi address is "+ android.text.format.Formatter.formatIpAddress(ipAddress));

	        } else if (mobile.isAvailable()) {

	            IP = GetLocalIpAddress();
	            
	        } 
	        
            // set the DNS to the new IP...
            //setDNS(node+"."+g.dom,GetLocalIpAddress() );
            //setDNS(node+"."+IP );
        }
    };

   private void registerReceivers() {    
       registerReceiver(mConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
   }
   
   private String GetLocalIpAddress() {
       try {
           for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
               NetworkInterface intf = en.nextElement();
               for (Enumeration<InetAddress> enumIpAddr = intf
                       .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                   InetAddress inetAddress = enumIpAddr.nextElement();
                   if (!inetAddress.isLoopbackAddress()) {
                       return inetAddress.getHostAddress().toString();
                   }
               }
           }
       } catch (SocketException ex) {
           return "ERROR Obtaining IP";
       }
       return "No IP Available";
   }
   public void killSSHdProcess() {
	   /*Log.i(TAG,"Killing background processes");
	   Context context = this.getApplicationContext();
	   ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	   mActivityManager.killBackgroundProcesses(context.getPackageName());
	   List<RunningAppProcessInfo> runningProcesses = mActivityManager.getRunningAppProcesses();
	   for (RunningAppProcessInfo p : runningProcesses)
		   Log.i(TAG,"process running:" + p.processName);*/
	   Log.i(TAG,"Finding already running sshd processes");
	   //String[] ps = runNative("ps |grep sshd").split("\\s+");
	   String[] ps = runNative("/system/bin/ps |grep sshd").split("\\s+");
	   Log.i(TAG,"ps output:"+Arrays.toString(ps));
	   if (ps != null && ps.length > 9) {
		   try {
			   int pid = Integer.parseInt(ps[9]);  //note that
			   Log.i(TAG,"Killing sshd process: " + pid);
			   android.os.Process.killProcess(pid);
		   } catch ( NumberFormatException e ) { Log.i(TAG, "Invalid PID! " + e.toString()); }
	   }
   }
}

