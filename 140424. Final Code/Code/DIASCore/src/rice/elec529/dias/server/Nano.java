package rice.elec529.dias.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.ServicesInterface;
 
public class Nano extends NanoHTTPD implements ServicesInterface
{
	int m_port = 1082;
	String m_path = "";
	LogInterface m_log;
	
    public Nano(int port, String path, LogInterface logger)
    {
        super(port, new File(path), logger);
        m_log = logger;
        m_port = port;
        m_path = path;
    }
 
    public Response serveFile(String uri, Properties header, File homeDir,
            boolean allowDirectoryListing)
    {
//    	m_log.log("Serving: " + uri);
        return super.serveFile(uri, header, homeDir, allowDirectoryListing);
 
    }
 
    public Response serve( String uri, String method, Properties header, Properties parms, Properties files )
    {
    	m_log.log("Serve: " + uri);
        return super.serve(uri, method, header, parms, files);
    }

	@Override
	public String getDescription()
	{
		return "NanoHTTP Server";
	}

	@Override
	public String getConfigUrl()
	{
		return "http://localhost:" + m_port + "/";
	}

	@Override
	public void start()
	{
		try
		{
			super.start();
		}
		catch (Exception e)
		{
			m_log.log("Nano Error!!!: " + e.getMessage());
		}
	}
}