package rice.comp529.dias;

import java.io.IOException;

import rice.elec529.dias.interfaces.LogInterface;

import org.apache.commons.logging.Log;

public class LogAndroid extends LogInterface implements Log
{
	EventLog m_eventWindow;
	EventLog m_logWindow;
	private static final String TAG = "Dias Log";

	public LogAndroid(EventLog eventWindow, EventLog logWindow)
	{
		m_eventWindow = eventWindow;
		m_logWindow = logWindow;
	}
	
	//  LogInterface
	@Override
	public void log(String s)
	{
		if (s == null)
			return;
		android.util.Log.i(TAG, s);
		m_logWindow.addEvent(s);
	}
	
	@Override
	public void logEvent(String s)
	{
		m_eventWindow.addEvent(s);
		log(s);
	}

	private StringBuffer sb = null;
	//  OutputStream from LogInterface
	@Override
	public void write(int b) throws IOException
	{
		if (sb == null)
			sb = new StringBuffer();
		
		sb.append(b);
		if ((b > 127) || (b == '\n') || (b == '\r') || (b == 10) || (b==13))
		{
			log(sb.toString());
			sb = null;
		}
	}
	
	/**  Implementers should call one of the above!  **/
	
	public void println(String s)
	{
		log(s);
	}
	
	//  Apache Log

	@Override
	public void debug(Object arg0) {
		log("debug: " + arg0.toString());
	}

	@Override
	public void debug(Object arg0, Throwable arg1) {
		log("debug: " + arg0.toString() + " " + arg1.getMessage());
	}

	@Override
	public void error(Object arg0) {
		log("error: " + arg0.toString());		
	}

	@Override
	public void error(Object arg0, Throwable arg1) {
		log("error: " + arg0.toString() + " " + arg1.getMessage());		
	}

	@Override
	public void fatal(Object arg0) {
		log("fatal: " + arg0.toString());
	}

	@Override
	public void fatal(Object arg0, Throwable arg1) {
		log("fatal: " + arg0.toString() + " " + arg1.getMessage());
	}

	@Override
	public void info(Object arg0) {
		log("info: " + arg0.toString());
	}

	@Override
	public void info(Object arg0, Throwable arg1) {
		log("info: " + arg0.toString() + " " + arg1.getMessage());
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isFatalEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public void trace(Object arg0) {
		debug(arg0);
	}

	@Override
	public void trace(Object arg0, Throwable arg1) {
		debug(arg0, arg1);
	}

	@Override
	public void warn(Object arg0) {
		log("warn: " + arg0.toString());
	}

	@Override
	public void warn(Object arg0, Throwable arg1) {
		log("warn: " + arg0.toString() + " " + arg1.getMessage());
	}

}
