/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.	All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice	University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/ 
package rice.elec529.dias.examples;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import rice.environment.Environment;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.NodeHandleSet;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.P2PInterface;

/**
 * Setup a FreePastry node using the Socket Protocol.
 * TODO: handle exceptions
 */
public class P2P implements P2PInterface, NodeIdFactory 
{

	private LogInterface m_logger;
	
	// TODO: the following 3 params should be hard coded, and need to change to private non-static
	static public int bindport;
	static public InetSocketAddress bootaddress;
	static public Environment env;
	
	private PastryNode node;
	private P2PApp app;
	private String m_nodeid;

	public String getNodeId()
	{
		return m_nodeid;
	}
	public rice.pastry.Id generateNodeId()
	{
		return string2Id(m_nodeid);
	}
	/**
	 * This constructor sets up a PastryNode.	It will bootstrap to an 
	 * existing ring if it can find one at the specified location, otherwise
	 * it will start a new ring.
	 * 
	 * @param bindport the local port to bind to 
	 * @param bootaddress the IP:port of the node to boot from
	 * @param env the environment for these nodes
	 */
	public P2P(LogInterface logger, String nodeID) throws Exception {
		m_logger = logger;
		m_nodeid = nodeID;
		
		// construct the PastryNodeFactory, this is how we use rice.pastry.socket
		PastryNodeFactory factory = new SocketPastryNodeFactory(this, bindport, env);

		// construct a node
		node = factory.newNode();
		
		// construct a new app
		app = new P2PApp(node, this);
	}
	
	@Override
	public String getDescription() {
		return "P2P";
	}
	
	
	
	@Override
	public void start(){		
		try {
			node.boot(bootaddress);
			
			// the node may require sending several messages to fully boot into the ring
			synchronized(node) {
				while(!node.isReady() && !node.joinFailed()) {
					// delay so we don't busy-wait
					node.wait(500);
					
					// abort if can't join
					if (node.joinFailed()) {
						throw new IOException("Could not join the FreePastry ring.	Reason:"+node.joinFailedReason()); 
					}
				}			 
			}
		}
		catch (Exception e) {
			m_logger.log("Patry problem: " + e.getMessage());
		}
		
		m_logger.log("Finished creating new node " + id2String(node.getId().toStringFull()) + " " + node);
		
		onConnected();
	}

	@Override
	public void stop() {
		onDisconnected();
		node.destroy();
		env.destroy();
		m_logger.log("P2P.stop()");
	}

	@Override
	public void onConnected() {
		m_logger.log("P2P.onConnected()");
	}

	@Override
	public void onDisconnected() {
		m_logger.log("P2P.onDisconnected()");

	}

	@Override
	public void onPeerJoined(String nodeId) {
		nodeId = id2String(nodeId);
		m_logger.log(m_nodeid + " P2P.onPeerJoined - " + nodeId);
	}
	
	@Override
	public void onMessageReceived (String fromNodeId, byte[] data){
		fromNodeId = id2String(fromNodeId);
		try {
			String tempstr = new String(data, "UTF-8");
			m_logger.log("Received from node " + fromNodeId + ". Data:\n" + tempstr);
		}
		catch (Exception e) {
		}
	}

	@Override
	public int sendMessage(String toNodeId, byte[] data) {
		app.routeMyMsg(string2Id(toNodeId), data);
		m_logger.log("P2P.sendMessage() from " + id2String(node.getId().toStringFull()) + " to " + toNodeId);
		// TODO: I do not think I can receive the ACK so I am not sure how to determine success.
		return 1; //success
	}
	
	
	
	public static rice.pastry.Id string2Id(String toId) {
		byte[] toIdchars = toId.getBytes();
		StringBuilder str = new StringBuilder();
		for(int i=0; i<toIdchars.length; i++)
			str.append(String.format("%02x", toIdchars[i]));
		return rice.pastry.Id.build(str.toString());
	}
	
	public static String id2String(String toString) { // This is different from toStringFull
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < toString.length(); i+=2) {
			str.append((char) Integer.parseInt(toString.substring(i, i + 2), 16));
		}
		return str.toString();
	}
}

/**
 * A P2P message.
 */
class P2PMsg implements Message {
	/**
	 * Where the Message came from.
	 */
	Id from;
	/**
	 * Where the Message is going.
	 */
	Id to;
	/**
	 * Type of message
	 */
	int type;
	/**
	 * The content.
	 */
	byte[] data;
	
	/**
	 * Constructor.
	 */
	public P2PMsg(Id from, Id to, byte[] data) {
		this.from = from;
		this.to = to;
		this.data = new byte[data.length];
		System.arraycopy(data, 0, this.data, 0, data.length);
	}
	
	public String toString() {
		return new String(data);
	}

	public Id getSenderId() {
		return from;
	}
	
	/**
	 * Use low priority to prevent interference with overlay maintenance traffic.
	 */
	public int getPriority() {
		return Message.LOW_PRIORITY;
	}
}

/**
 * A very simple application.
 */
class P2PApp implements Application {
	/**
	 * The Endpoint represents the underlieing node.	By making calls on the 
	 * Endpoint, it assures that the message will be delivered to a P2PApp on whichever
	 * node the message is intended for.
	 */
	protected Endpoint endpoint;
	
	private P2P host;

	public P2PApp(Node node, P2P host) {
		// We are only going to use one instance of this application on each PastryNode
		this.endpoint = node.buildEndpoint(this, "myinstance");
		
		this.host = host;
		
		// now we can receive messages
		this.endpoint.register();
	}

	/**
	 * Called to route a message to the id
	 */
	public void routeMyMsg(Id id,  byte[] data) {	
		Message msg = new P2PMsg(endpoint.getId(), id, data);
		endpoint.route(id, msg, null);
	}
	
	/**
	 * Called to directly send a message to the nh
	 */
	public void routeMyMsgDirect(rice.p2p.commonapi.NodeHandle nh, byte[] data) {
		Message msg = new P2PMsg(endpoint.getId(), nh.getId(), data);
		endpoint.route(null, msg, nh);
	}
		
	/**
	 * Called when we receive a message.
	 */
	public void deliver(Id id, Message message) {
		// TODO: this.endpoint is the current node, id is the nodeId of the targeted node. Should we check for it they match? Do we need to do anything if they don't?
		host.onMessageReceived(((P2PMsg)message).getSenderId().toStringFull(), message.toString().getBytes());
	}

	/**
	 * Called when you hear about a new neighbor.
	 * Don't worry about this method for now.
	 */
	public void update(rice.p2p.commonapi.NodeHandle handle, boolean joined) {
		host.onPeerJoined(handle.getId().toStringFull());
		return;
	}
	
	/**
	 * Called a message travels along your path.
	 * Don't worry about this method for now.
	 */
	public boolean forward(RouteMessage message) {
		return true;
	}
	
	public String toString() {
		return "P2PApp "+endpoint.getId();
	}

}