//We need to setup all the keys before this works...
//The main DIAS Activity will have to setup all the config for all the servers (including keys).

package rice.elec529.dias.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.ServicesInterface;

public class SSHService implements ServicesInterface {
	
	private LogInterface m_logger;
	private SSHServer server;
	private String sshdPath = "/sdcard/DIAS/ssh/";
	private String sshdName = "sshd";
	//-D keeps it from running in the background, -e logs to stdout
	//todo: set ssh key path and pid path using basePath
	//private List<String> sshdArgs = Arrays.asList("-D","-e", "-h", "/sdcard/DIAS/ssh/id_rsa", "-o", "PidFile", "/sdcard/DIAS/ssh/sshd.pid", "-f", "/data/data/rice.elec529.dias/bin/sshd_config", "-o", "PermitTunnel", "yes", "-p"); //-p has to go last so we can config the port

	private List<String> sshdArgs = Arrays.asList("-D","-e", "-h", "/sdcard/DIAS/.ssh/id_rsa", "-o PidFile /sdcard/DIAS/.ssh/sshd.pid", "-f", "/data/data/rice.comp529.dias/bin/sshd_config", "-o PermitTunnel yes", "-p"); //-p has to go last so we can config the port
	//We should be able to put most of this in the config file, this is just how sshelper handled it
	// also "-o Banner /path/to/banner", "-t" for test, "-o PasswordAuthentication no", "-#" where # is a debug level 1-4, "-c /path/to/id_dsa"
	//where is bin path (i.e. where the shell is...) set?  Also needs sftp and rsync...
	ArrayList<String> execCmd = new ArrayList<String>();  //need to use ArrayList to dynamically append.
	int port = 2222;
	
	public SSHService(LogInterface logger)
	{
		m_logger = logger;
	}	
	
	public SSHService(int p, LogInterface logger)
	{
		this(logger);
		port = p;
	}	
	public SSHService(int p, String binPath, String basePath, LogInterface logger)
	{	
		this(p, logger);
		sshdPath = binPath;		
	}
	
	public SSHService(int p, List<String> sshOptions, LogInterface logger)
	{
		this(p, logger);
		sshdArgs = sshOptions;
	}

	@Override
	public String getDescription() {
		return "SSHService";
	}

	@Override
	public String getConfigUrl() {
		return "ssh://localhost:" + port;
	}

	@Override
	public void start() {
		m_logger.log("SSHService.start()");
		execCmd = new ArrayList<String>(); //
		execCmd.add(sshdPath + sshdName);
		execCmd.addAll(sshdArgs);
		execCmd.add(""+port);
		
		if ( server == null ) {			
			server = new SSHServer(m_logger, execCmd);
		}
		if ( !isRunning() ) {
			server.start();
		}
		else {
			m_logger.log("SSHService.start(): SSH server is already running.");
		}
	}

	public boolean isRunning() {
		if (server == null)
			return false;
		return server.running;
	}
	@Override
	public void stop() {
		m_logger.log("SSHService.stop()");
		if (server != null)
			server.stopProcess();
		server = null;
	}
}
