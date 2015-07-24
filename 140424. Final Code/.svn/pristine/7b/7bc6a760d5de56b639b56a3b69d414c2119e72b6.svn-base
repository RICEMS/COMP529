package rice.comp529.dias;

import java.lang.Object;
import java.lang.Thread;
import android.util.Log;

public class UpdateLoop extends Object implements Runnable
{
	private boolean m_quit = false;
	private long m_sleepTime = 1000;
	private static final String TAG = "UpdateLoop";
	
	public UpdateLoop(long sleepTime)
	{
		init(sleepTime);
	}
	
	public UpdateLoop()
	{
		init(1000);
	}
	
	private void init(long sleepTime)
	{
		m_quit = false;
		m_sleepTime = sleepTime;
		Thread t = new Thread(this);
		t.start();
	}
	
	public void setSleepTime(long sleepTime)
	{
		m_sleepTime = sleepTime;
	}
	
	public void stop()
	{
		m_quit = true;
	}
	
	public void update()
	{
		Log.e(TAG, "UpdateLoop update should be overloaded.");
	}
	
	@Override
	public void run()
	{
		while (!m_quit)
		{
			update();
			try
			{
				Thread.sleep(m_sleepTime);
			}
			catch (Exception e)
			{
				Log.e(TAG, "UpdateLoop error when sleeping.  " + e.getMessage());
			}
		}
	}

}
