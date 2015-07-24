package rice.elec529.dias.examples;

import java.io.IOException;
import rice.elec529.dias.interfaces.LogInterface;
import org.apache.commons.logging.Log;

public class LogFixture extends LogInterface implements Log
{
	//  LogInterface
	@Override
	public void log(String s)
	{
		System.out.println(s);
	}
	
	@Override
	public void logEvent(String s)
	{
		log(s);
	}

	//  OutputStream from LogInterface
	@Override
	public void write(int b) throws IOException
	{
		System.out.write(b);
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
