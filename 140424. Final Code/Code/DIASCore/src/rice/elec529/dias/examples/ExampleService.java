package rice.elec529.dias.examples;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.ServicesInterface;

public class ExampleService implements ServicesInterface {
	
	private LogInterface m_logger;
	
	public ExampleService(LogInterface logger)
	{
		m_logger = logger;
	}

	@Override
	public String getDescription() {
		return "ExampleService";
	}

	@Override
	public String getConfigUrl() {
		return "http://localhost:8080/";
	}

	@Override
	public void start() {
		m_logger.log("ExampleService.start()");
	}

	@Override
	public void stop() {
		m_logger.log("ExampleService.stop()");
	}

}
