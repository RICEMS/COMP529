package rice.elec529.dias.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.ServicesInterface;
import rice.elec529.dias.rsync.RsyncServer;

//import android.util.Log;


public class JettyService implements ServicesInterface{


		int port = 8090; 
		private LogInterface m_logger;
		private Server server;
	public JettyService()
	{
	}			
	public JettyService(LogInterface logger)
	{
		this();
		m_logger = logger;
		
	}		
	
	public JettyService(int p, LogInterface logger)
	{
		this(logger);
		port = p;
	}	
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Jetty Server";
	}

	@Override
	public String getConfigUrl() {
		// TODO Auto-generated method stub
		return "https://localhost:"+port;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
		Handler handler = new AbstractHandler() {

			@Override
			public void handle(String arg0, Request arg1,
					HttpServletRequest request, HttpServletResponse servletResponse)
							throws IOException, ServletException{
				// TODO Auto-generated method stub
				servletResponse.setContentType("text/html");
				servletResponse.setStatus(HttpServletResponse.SC_OK);
				servletResponse.getWriter().println("<h1>Hello World</h1>");
				((Request) request).setHandled(true);
				
			}

		};

		this.server = new Server(port);
		server.setHandler(handler);
		try {
			server.start();
			//Log.e("JT", "started");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		try {
			this.server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
}
