package rice.elec529.dias.jes;

import com.ericdaugherty.mail.server.Mail;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.ServicesInterface;

public class JES implements ServicesInterface
{
	private LogInterface m_logger;
	private Mail m_mail;
	private String m_directory;

	public JES(LogInterface logger, String directory)
	{
		m_logger = logger;
		m_directory = directory;
	}

	@Override
	public String getDescription()
	{
		return "JES";
	}

	@Override
	public String getConfigUrl()
	{
		return "JES EMAIL Server... Running SMPT on port 25";
		// get port #s
	}

	@Override
	public void start() {
		m_logger.log("JES.start()");	    
		try {
			/*m_mail = new Mail();
			String args[] = {m_directory}; // "/sdcard/jes/"  or "/sdcard/dias/jes/"  and then we need a conf folder etc.
			m_mail.init(args);
//			m_mail.init(args, m_logger);*/
			
			Mail.instantiate(new String[]{m_directory});

		} catch (RuntimeException e) {
			System.err.println( "The application failed to initialize." );
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void stop() {
		m_logger.log("JES.stop()");
		try {
			m_mail.shutdown();
		} catch (RuntimeException e) {
			System.err.println( "The application failed to Shut Down." );
			e.printStackTrace(System.err);
		}
	}
}

