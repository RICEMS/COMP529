package rice.elec529.dias.server;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import rice.elec529.dias.examples.LogFixture;
import rice.elec529.dias.examples.P2P;
import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.NameResolver;
import rice.elec529.dias.interfaces.P2PInterface;
import rice.environment.Environment;
import rice.environment.params.simple.SimpleParameters;

public class Pastry extends P2P
{
	public static String BootAddress = "ring.clear.rice.edu";
	public static int P2PPort = 18100;
	static LogInterface m_logger;
	private static String m_hostAddress;
	
	public Pastry(LogInterface logger, String nodeID) throws Exception {
		super(logger, nodeID);
		m_hostAddress = InetAddress.getLocalHost().getHostAddress();
		m_logger.log("Host address = " + m_hostAddress);
	}

	public void onMessageReceived (String fromNodeId, byte[] data){
		fromNodeId = id2String(fromNodeId);
		try {
			String tempstr = new String(data, "UTF-8");
			if (m_logger != null)
				m_logger.log("Received from node: " + fromNodeId + " Data: " + tempstr);
			String s = tempstr.toString();
			if (s== null)
				return;
			if (s.trim().equalsIgnoreCase("getIP"))
			{
				m_logger.log(getNodeId() + " Got get IP message.");
				m_logger.log(getNodeId()+"-"+fromNodeId.trim());
				//  Self test.
				if (fromNodeId.trim().equalsIgnoreCase(getNodeId().trim()))
				{
					m_logger.log("adding localhost");
					NameResolver.addName(fromNodeId, "127.0.0.1");
					return;
				}
				//  Other node.
				m_logger.log("sending to: " + fromNodeId + " Data: " + m_hostAddress);
				this.sendMessage(fromNodeId, m_hostAddress.getBytes());
			}
			else
			{
				NameResolver.addName(fromNodeId, tempstr);
			}
		}
		catch (Exception e) {
			m_logger.log("pastry exception to dias " + e.getMessage());
		}
	}
	
	public static P2PInterface getP2P(LogInterface logger, String nodeID, Reader r)
	{
		m_logger = logger;
		try
		{
			if (r != null)
				P2P.env = new Environment(null,null,null,null,null,new PastryParams(r), null);
			else
				// Loads pastry settings
				P2P.env = new Environment(null,null,null,null,null,new SimpleParameters(Environment.defaultParamFileArray,null), null);

			// disable the UPnP setting (in case you are testing this on a NATted LAN)
			P2P.env.getParameters().setString("nat_search_policy","never");

			// the port to use locally
			P2P.bindport = P2PPort;

			// build the boot address from the command line args
			P2P.bootaddress = new InetSocketAddress(InetAddress.getByName(BootAddress), P2PPort);

			// launch our node!
			P2P node = new Pastry(new LogFixture(), nodeID);
			
			NameResolver.setP2P(node);
			return node;
		}
		catch (Exception e)
		{
			logger.log("Pastry Error: " + e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			logger.log(errors.toString());
		}
		return null;
	}

}
