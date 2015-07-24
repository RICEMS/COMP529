package rice.elec529.dias.server;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;

import rice.environment.params.ParameterChangeListener;
import rice.environment.params.simple.SimpleParameters;

public class PastryParams extends SimpleParameters
{

	public PastryParams(Reader defaultsReader) throws Exception
	{
		this.properties = new MyProperties();
		this.defaults = new MyProperties();
		this.changeListeners = new HashSet<ParameterChangeListener>();

		this.defaults.load(defaultsReader);
		System.err.println("Configuration file not present.  Using defaults.");
	}

}
