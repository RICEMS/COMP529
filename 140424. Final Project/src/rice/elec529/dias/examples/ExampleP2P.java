package rice.elec529.dias.examples;

import rice.elec529.dias.interfaces.LogInterface;
import rice.elec529.dias.interfaces.P2PInterface;

public class ExampleP2P implements P2PInterface {

private LogInterface m_logger;
	
	public ExampleP2P(LogInterface logger)
	{
		m_logger = logger;
	}
	
	
	@Override
	public String getDescription() {
		return "ExampleP2P";
	}

	@Override
	public void start() {
		m_logger.log("ExampleP2P.start()");
	}

	@Override
	public void stop() {
		m_logger.log("ExampleP2P.stop()");
	}

	@Override
	public void onConnected() {
		m_logger.log("ExampleP2P.onConnected()");
	}

	@Override
	public void onDisconnected() {
		m_logger.log("ExampleP2P.onDisconnected()");

	}

	@Override
	public void onPeerJoined(String nodeId) {
		m_logger.log("ExampleP2P.onPeerJoined()");
	}

	@Override
	public void onMessageReceived(String fromNodeId, byte[] data) {
		m_logger.log("ExampleP2P.onMessageReceived()");
	}

	@Override
	public int sendMessage(String toNodeId, byte[] data) {
		m_logger.log("ExampleP2P.sendMessage()");
		return 1; //success
	}

}
