/*
 * This is a thread which launches the rsync processes at a given interval.
 *
 * Right now this is configured to launch one rsync process after the other, synchronously -- perhaps it would be better to launch them each separately.
 */


package rice.elec529.dias.rsync;

import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import rice.elec529.dias.interfaces.LogInterface;

//should be more like rsyncThread|rsyncPusher or something, since it's not really server...
final public class RsyncServer extends Thread {

	RsyncService service;
	boolean starting = false;
	boolean running = false;
	boolean stop = false;
	//String homePath = "/sdcard/DIAS/"; //this really doesn't do anything
	List<String> execCmd;
	LogInterface m_logger;
	Process rsyncProcess;
	int interval = 5000;

	public RsyncServer(LogInterface logger, List<String> cmd) {
		m_logger = logger;
		execCmd = cmd;
	}
	
	public RsyncServer(LogInterface logger, List<String> cmd, int intv) {
		m_logger = logger;
		execCmd = cmd;
		interval = intv;
	}

	protected void stopProcess() {		
		m_logger.log("RsyncServer.stopProcess()");
		stop = true;
		//perhaps wait a bit?
		if (rsyncProcess != null) {
			rsyncProcess.destroy();
			rsyncProcess = null;
		}
	}

	@Override
	public void run() {

		try {
			stop = false;
			starting = true;
			// restart if no activity in control
			m_logger.log("rsync: starting");
			while (!stop) {				
				ProcessBuilder pb = new ProcessBuilder(execCmd);
				//pb.directory(new File("/data/data/rice.comp529.dias/bin/"));
				//pb.directory(new File(homePath));
				pb.redirectErrorStream(true);  //combine stdout and stderr
				//Map<String, String> env = pb.environment();
				//env.put("PATH", "/data/data/rice.comp529.dias/bin/:/system/bin"); //this doesn't seem to actually work :/
				//env.put("HOME", homePath);
				rsyncProcess = pb.start();
				BufferedReader bis = new BufferedReader(new InputStreamReader(rsyncProcess.getInputStream()));

				String line;
				running = true;
				starting = false;
				
				m_logger.log("rsync running with command: " +pb.command());
				while (!stop && (line = bis.readLine()) != null) {
					//todo: search for common errors (e.g. public key auth not working or connection refused);
					//if (line.matches("(?i).*address already in use.*")) {
					//	addressInUse = true;
					//}
					m_logger.log("rsync: " + line);
				}				
				bis.close();				
				
				Thread.sleep(interval);					
			}
		} catch (Exception e) {
			m_logger.log(e.toString());
			e.printStackTrace();
		}
		stopProcess(); //make sure the process is null...
		starting = false;
		stop = false;
		running = false;
	}

}
