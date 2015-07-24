package rice.elec529.dias.rsync;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Arrays;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.ServicesInterface;

public class RsyncService implements ServicesInterface {
	
	private LogInterface m_logger;
	private RsyncServer servers[];
	private String userPath = "/sdcard/DIAS/users/";
	private String rsyncPath = "/data/data/rice.comp529.dias/bin/";
	//private String rsyncName = "rsync";
	private String rsyncCmd = "rsyncdias.sh";
	private String rsyncArgs = "-auvz";  //could use "--delete", but could unintentionally delete new files...
	//may need something like -e 'ssh -p1234  -i /sdcard/DIAS/ssh/id_dsa' to specify the key file.
	//private String users[] = {"adriana", "clay", "ellis", "haihua", "yanda"};
	private String users[] = {"adriana", "clay", "ellis"};
	private String node = "clay";
	//private String users[] = {"adriana"};
	//private Map<String, String[]> hostFolderMap; //This would be a more flexible way to do it (we could choose which files were replicated to which users)...  maybe we should read a config file for it?
	//i.e. the current method requires that we replicate every user to every other user (or with a minor change each user only back to that user)
	int port = 2222;
	
	public RsyncService(LogInterface logger)
	{
		m_logger = logger;
		servers = new RsyncServer[users.length];
	}		
	
	public RsyncService(int p, LogInterface logger)
	{
		this(logger);
		port = p;
	}	
	public RsyncService(int p, String binPath, String basePath, String nodeName, LogInterface logger)
	{
		this(p, logger);
		//userPath = basePath;
		rsyncPath = binPath;
		node = nodeName;
	}	
	public RsyncService(int p, String binPath, String basePath, String nodeName, String[] rUsers, LogInterface logger)
	{
		this(p, binPath, basePath, nodeName, logger);
		users = rUsers;
	}
	 
	public RsyncService(int p, String rsyncOptions, LogInterface logger)
	{
		this(p, logger);
		rsyncArgs = rsyncOptions;
	}

	@Override
	public String getDescription() {
		return "RsyncService";
	}

	@Override
	public String getConfigUrl() {
		return "rsync://localhost:" + port;
	}

	@Override
	public void start() {
		m_logger.log("RsyncService.start()");
		
		boolean t = false;
		for (RsyncServer s : servers)
			if (s == null)
				t |= true;
		
		if ( t && !isRunning() ) { 
			int i = 0;
			
			//The only way I could get rsync to work from the process builder was to create a shell script with the right args (assets/rsyncdias.sh).
			//The only way I could get the shell script to run is to call it with the shell explicitly.
			//The only way I could get the ssh key auth to work is to use the dropbear ssh client (dbclient), which requires the 
			//Note:  It is very important to query the master node (m.), since we only want to replicate to the real node (not a failover)
			for (String user : users) {
				//TODO: resolve domain here with DNSResolver
				//real cmd
				if (!user.equals(node))
					//servers[i++] = new RsyncServer(m_logger, Arrays.asList(rsyncPath + "ash", rsyncPath + rsyncCmd, userPath+"../", "users", "root@m."+user+".elec529.recg.rice.edu", ""+port, rsyncArgs)); //full replication
					servers[i++] = new RsyncServer(m_logger, Arrays.asList(rsyncPath + "ash", rsyncPath + rsyncCmd, userPath, user, "root@m."+user+".elec529.recg.rice.edu", ""+port, rsyncArgs)); //sparse replication
				
				//testing cmd (to elec529 main server)
				//servers[i++] = new RsyncServer(m_logger, Arrays.asList(rsyncPath + "ash", rsyncPath + rsyncCmd, userPath, user, "root@elec529.recg.rice.edu", ""+22, rsyncArgs));
				
				//in case we ever go back, these were the closest I had to working:
				//servers[i++] = new RsyncServer(m_logger, Arrays.asList(rsyncPath + rsyncName, rsyncArgs, "-e", "/data/data/rice.comp529.dias/bin/ssh -y -i /sdcard/DIAS/.ssh/id_rsa.dropbear", userPath+user, " root@elec529.recg.rice.edu:" + userPath+user));
				//servers[i++] = new RsyncServer(m_logger, rsyncPath + rsyncName + rsyncArgs + userPath + " root@" + user + "elec529.recg.rice.edu:" + port + userPath);

			}
		}
		if ( !isRunning() ) {
			stop();  //first make sure they are all stopped
			
			for (RsyncServer server : servers)
				server.start();
		}
		else {
			m_logger.log("RsyncService.start(): rsync server is already running.");
		}
	}
	
	//returns true if all servers are running
	public boolean isRunning() {
		boolean out = true;
		for (RsyncServer server : servers) {
			if (server == null)
				return false;
			out &= server.running;
		}
		return out;
	}
	@Override
	public void stop() {
		m_logger.log("RsyncService.stop()");
		for (RsyncServer server : servers) 
			if (server != null)
				server.stopProcess();
	}
	
	public void setNodeName(String nn) {
		node = nn;
	}

}
