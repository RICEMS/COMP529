package rice.elec529.dias.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import jsocks.SOCKS;
import jsocks.socks.ProxyServer;
import jsocks.socks.server.IdentAuthenticator;
import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.NameResolver;
import rice.elec529.dias.interfaces.ServicesInterface;

public class JSocks implements ServicesInterface
{
	private LogInterface m_logger;
	private ProxyServer m_server;
	int m_port;
	
	public JSocks(int port, LogInterface logger)
	{
		m_port = port;
		m_logger = logger;

		Properties pr = new Properties();
		pr.put("iddleTimeout", "600000");
		pr.put("acceptTimeout", "60000");
		pr.put("udpTimeout", "600000");
		pr.put("range", ".");
		
		IdentAuthenticator auth = new IdentAuthenticator();
		SOCKS.addAuth(auth,pr);
		SOCKS.serverInit(pr);

		m_server = new ProxyServer(auth);
		ProxyServer.setLog(m_logger);
	}

	@Override
	public String getDescription() {
		return "JSocks running on port: " + m_port;
	}

	@Override
	public String getConfigUrl() {
		return "http://localhost:8080/";
	}

	@Override
	public void start() {
		m_logger.log("JSocks.start()");
		InetAddress localIP = null;
		m_server.start(m_port, 5, localIP);
	}

	@Override
	public void stop() {
		m_logger.log("JSocks.stop()");
		m_server.stop();
	}

}
