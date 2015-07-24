/*
 * This is a thread which launches the sshd process, then monitors its output.
 */


package rice.elec529.dias.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import rice.elec529.dias.interfaces.LogInterface;

final public class SSHServer extends Thread {

	boolean stop = false;
	boolean starting = false;
	boolean addressInUse = false;
	boolean noHostKeys = false;
	boolean running = false;
	List<String> execCmd;
	String homePath = "/sdcard/DIAS/";
	LogInterface m_logger;
	Process sshdProcess;

	public SSHServer(LogInterface logger, List<String> cmd) {
		m_logger = logger;
		execCmd = cmd;
	}

	//stop() is a Thread function
	public void stopProcess() {
		m_logger.log("SSHServer.stopProcess()");
		stop = true;
		//perhaps wait a bit?
		if (sshdProcess != null) {
			sshdProcess.destroy();
			sshdProcess = null;
		}
	}
	
	@Override
	public void run() {
		try {
			stop = false;
			starting = true;

			while ( !addressInUse && !noHostKeys && !stop) {
				ProcessBuilder pb = new ProcessBuilder(execCmd);
				pb.directory(new File(homePath));
				Map<String, String> env = pb.environment();
				//env.put("PS1", "[\\u\\@\\h \\w ]\\$ ");
				env.put("PS1", "$(precmd)$USER@$HOSTNAME:${PWD:-?} $"); //this sets what the shell shows as a prompt.  This is from SSHDroid
				env.put("SSH_SERVER_PW", "MCluster");
				env.put("USER", "root");
				env.put("PATH", "/data/data/rice.comp529.dias/bin/:/system/bin");
				env.put("LOGINSHELL", "/data/data/rice.comp529.dias/bin/ash");
				pb.redirectErrorStream(true);
				sshdProcess = pb.start();  
				BufferedReader bis = new BufferedReader(new InputStreamReader(sshdProcess.getInputStream()));

				String line;
				running = true;
				starting = false;
				while (!stop && running && !addressInUse && !noHostKeys && (line = bis.readLine()) != null) {
					if (line.matches("(?i).*address already in use.*")) {
						addressInUse = true;
					}
					if (line.matches("(?i).*no hostkeys available.*")) {
						noHostKeys = true;
					}
					if (line.matches("(?i).*terminating.*")) {
						running = false;
					}
					m_logger.log("sshd: " + line);
				}
				bis.close();
				if (noHostKeys) {
					m_logger.log("There are no SSH server hostkeys available.");
				}
				if (addressInUse) {
					m_logger.log(String.format("* another service is preventing use of port."));
				}
			}
		} catch (Exception e) {
			m_logger.log(e.toString()); //would be better to let log() take an exception...
			e.printStackTrace();
		}				
		stopProcess(); //make sure the process is null...
		starting = false;
		running = false;
		stop = false;
	}

}
