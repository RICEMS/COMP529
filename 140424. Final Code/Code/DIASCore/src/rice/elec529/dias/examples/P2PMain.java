package rice.elec529.dias.examples;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeIdFactory;
import rice.pastry.standard.RandomNodeIdFactory;

import rice.elec529.dias.examples.LogFixture;
import rice.elec529.dias.examples.P2P;
import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.P2PInterface;

/**
 * Example usage: (in folder COMP529/Code/src) 
 * javac -classpath ./../lib/FreePastry-2.1.jar rice/elec529/dias/examples/P2P.java rice/elec529/dias/examples/P2PMain.java rice/elec529/dias/examples/LogFixture.java rice/elec529/dias/interfaces/P2PInterface.java rice/elec529/dias/interfaces/LogInterface.java
 * java -cp .:./../lib/FreePastry-2.1.jar rice.elec529.dias.examples.P2PMain Clayton 18010 water.clear.rice.edu 18010
 * java -cp .:./../lib/FreePastry-2.1.jar rice.elec529.dias.examples.P2PMain Ellis 18011 water.clear.rice.edu 18010
 * java -cp .:./../lib/FreePastry-2.1.jar rice.elec529.dias.examples.P2PMain Adriana 18012 water.clear.rice.edu 18010
 */
public class P2PMain {
	public static void main(String[] args) throws Exception {
		
		P2P myNode = null;
		
		// Loads pastry settings
		P2P.env = new Environment();

		// disable the UPnP setting (in case you are testing this on a NATted LAN)
		P2P.env.getParameters().setString("nat_search_policy","never");
		
		try {
			// the port to use locally
			P2P.bindport = Integer.parseInt(args[1]);
		
			// build the bootaddress from the command line args
			InetAddress bootaddr = InetAddress.getByName(args[2]);
			int bootport = Integer.parseInt(args[3]);
			P2P.bootaddress = new InetSocketAddress(bootaddr,bootport);
		
			// launch our node!
			myNode = new P2P(new LogFixture(), args[0]);
			myNode.start();
		} catch (Exception e) {
			// remind user how to use
			System.out.println("Usage:"); 
			System.out.println("java [-cp FreePastry-<version>.jar] rice.elec529.dias.p2p.P2PMain nodeID localbindport bootIP bootPort");
			System.out.println("example: java -cp .:FreePastry-2.1.jar rice.elec529.dias.p2p.P2PMain Shen001 18001 glass.clear.rice.edu 9001");
			System.exit(0);
		}
		
		// test process starts
			
		// wait 10 seconds
		P2P.env.getTimeSource().sleep(10000);
		
		myNode.sendMessage("Clayton", new byte[0]);
		P2P.env.getTimeSource().sleep(2000);
		myNode.sendMessage("Adriana", new byte[0]);
		P2P.env.getTimeSource().sleep(2000);
		myNode.sendMessage("Ellis", new byte[0]);
		P2P.env.getTimeSource().sleep(2000);

		P2P.env.getTimeSource().sleep(10000);
		
		myNode.stop();
		
		// test process ends
	}
}
