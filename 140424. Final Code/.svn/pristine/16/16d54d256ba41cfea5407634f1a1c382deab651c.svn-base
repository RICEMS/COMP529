package rice.elec529.dias;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.ServicesInterface;

public class Service implements Runnable
{
	private ServicesInterface m_service;
	public static LogInterface log = null;
	private Thread m_t;

	public void stop()
	{
		log.log("Stopping " + m_service.getDescription());
		m_service.stop();
	}
	
	public void start()
	{
		m_t.start();
		
		//  Might need to delete this for Android.
		try
		{
			Thread.currentThread().sleep(1500);
		}
		catch (Exception e)
		{
			log.error("Failed to sleep.", e);
		}
	}

	public Service(ServicesInterface service)
	{
		m_service = service;

		log.log(service.getDescription() + " " + "Starting.");
		m_t = new Thread(this, service.getDescription());
	}

	public void run()
	{
		m_service.start();
	}

}
