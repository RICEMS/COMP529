package rice.elec529.dias;

import java.io.Reader;
import java.util.Vector;

import rice.elec529.dias.examples.LogFixture;
import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.P2PInterface;
import rice.elec529.dias.jes.JES;
import rice.elec529.dias.server.JSocks;
import rice.elec529.dias.server.Nano;
import rice.elec529.dias.server.JettyService;
import rice.elec529.dias.server.Pastry;
import rice.elec529.dias.ssh.SSHService;
import rice.elec529.dias.rsync.RsyncService;

public class DIAS
{
	//  --Services--
	Service proxyService, webService, emailService, webJettyService;
	SSHService sshService;
	RsyncService rsyncService;
	
	LogInterface m_log;
	String m_nodeName;

	boolean m_onlyP2P = false;
	//  Static Initializers
	public static final int PastryPort = 18025;
	public static final int JSocksPort = 18026;//1082;
	int WWWPort = 18080;
	int SSHPort = 2222;
	//  Directory hierarchy.
	static final String DefaultBaseDir   = System.getProperty("user.dir") + "/";
	static final String DefaultDataDir   = DefaultBaseDir + "data/";
	private P2PInterface p2p;
	String binPath = "/data/data/rice.comp529.dias/bin/";
	String basePath = "/sdcard/DIAS/";
	
	public DIAS(LogInterface log, String nodeName, boolean onlyP2P, String binP, String baseP)
	{
		Service.log = log;
		m_log = log;
		Pastry.BootAddress = "ring.clear.rice.edu"; 
		//"ec2-54-227-157-239.compute-1.amazonaws.com";  //"elec529.recg.rice.edu";
		Pastry.P2PPort = PastryPort;
		binPath = binP;
		basePath = baseP;
		
		String ConfigDir = basePath + "conf/";
		String WWWDir    = ConfigDir + "www/";
		String EmailDir  = ConfigDir + "email/";
		
		m_nodeName = nodeName;
		m_onlyP2P = onlyP2P;
		
		proxyService = new Service(new JSocks(JSocksPort, m_log));
		sshService = new SSHService(SSHPort, binPath, basePath, m_log);
		rsyncService = new RsyncService(SSHPort, binPath, basePath, m_nodeName, m_log);

		//webService = new Service(new Nano(WWWPort, ConfigDir + WWWDir, log));
		webService = new Service(new Nano(WWWPort, basePath + "users/"+m_nodeName+"/www/", m_log));
		emailService = new Service(new JES(m_log, "conf/email/"));	
		
		//webService = new Service(new JettyService(WWWPort, log));
		
	}
	
	public void setNodeName(String nn)
	{
		m_nodeName = nn;
		rsyncService.setNodeName(nn);
		webService = new Service(new Nano(WWWPort, basePath + "users/"+m_nodeName+"/www/", m_log));
	}
	
	public void startP2P(Reader r)
	{
		m_log.log("user.dir = " + System.getProperty("user.dir"));
		m_log.logEvent("Starting P2P Server");
		m_log.logEvent("Node name = " + m_nodeName);
		
		try
		{
		//  Start P2P.
		p2p = Pastry.getP2P(m_log, m_nodeName, r);
		p2p.start();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}
	}
	public void stopP2P()
	{
		if (p2p != null)
		{
			try
			{
			p2p.stop();
			}
			catch (Exception e)
			{
				m_log.log(e.getMessage());
			}
		}
	}
	
	public void startProxy()
	{
		m_log.logEvent("Starting SOCKS Proxy Server");
		try
		{
			proxyService.start();
		}
	catch (Exception e)
	{
		m_log.log(e.getMessage());
	}
	}
	public void stopProxy()
	{
		m_log.logEvent("Stopping SOCKS Proxy Server");
		try
		{
			proxyService.stop();
		}
	catch (Exception e)
	{
		m_log.log(e.getMessage());
	}
	}
	public void startSSH()
	{
		m_log.logEvent("Starting SSH Server");
		try
		{
			sshService.start();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}

	}
	public void stopSSH()
	{
		m_log.logEvent("Stopping SSH Server");
		try {
			sshService.stop();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}

	}
	public void startRsync()
	{
		m_log.logEvent("Starting Rsync Server");
		try {
			rsyncService.start();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}

	}
	public void stopRsync()
	{
		m_log.logEvent("Stopping Rsync Server");
		try {
			rsyncService.stop();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}

	}
	public void startWeb()
	{
		m_log.logEvent("Starting Web Server");
		try {
			webService.start();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}

	}
	public void stopWeb()
	{
		m_log.logEvent("Stopping Web Server");
		try {
			webService.stop();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}

	}
	public void startEmail()
	{
		m_log.logEvent("Starting Email Server");
		try {
			emailService.start();
		}
		catch (Exception e)
		{
			m_log.log(e.getMessage());
		}

	}
	public void stopEmail()
	{
		m_log.logEvent("Stopping Email Server");
		try {
			emailService.stop();
		}
		catch (java.lang.NoClassDefFoundError e)
		{
			m_log.log(e.getMessage());
		}

	}
	
	public void startServices(Reader r)
	{
		m_log.logEvent("Starting Services");
		startP2P(r);
		if (m_onlyP2P)
			return;
		
		startProxy();
		//startSSH();
		//startRsync();
		startWeb();
		//startEmail();
		
		m_log.logEvent("Complete");
	}
	
	public void stopAll()
	{
		//  On Android destroy want to call this method to stop all the services.
		m_log.logEvent("Stopping Services");
		stopProxy();
		stopSSH();
		stopRsync();
		stopWeb();
		//  this seg faults  stopEmail();
		m_log.logEvent("Stopped.");
	}
	
	public static void printUsage()
	{
		System.err.println("Usage:  java [-Donlypastry=1] -jar dias.jar NodeName [bootAddress=elec529.recg.rice.edu]");
	}
	
	public static void main(String args[])
	{
		LogFixture log = new LogFixture();
		Service.log = log;
		
		if (args.length < 1)
		{
			printUsage();
			return;
		}
		String nodeName = args[0];
		if (args.length > 1)
		{
			log.log("Using Pastry boot address " + args[1]);
			Pastry.BootAddress = args[1];
		}
		
		boolean onlypastry = (System.getProperty("onlypastry") != null) &&
				(System.getProperty("onlypastry").trim().equals("1"));
		DIAS d = new DIAS(log, nodeName, onlypastry, DefaultDataDir, DefaultBaseDir);
		d.startServices(null);
	}
	
}
