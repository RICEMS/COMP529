package rice.elec529.dias.interfaces;

public interface P2PInterface {
	public String getDescription();
	public void start();
	public void stop();
	public void onConnected();
	public void onDisconnected();
	public void onPeerJoined(String nodeId);
	public void onMessageReceived(String fromNodeId, byte data[]);
	public int sendMessage(String toNodeId, byte data[]);
}
