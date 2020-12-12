import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.TimerTask;
import java.util.Vector;

public class UnChoked  extends TimerTask {
	private static void sendUnChoke(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " is sending UNCHOKE message to remote Peer " + remotePeerID);
		DataMessage d = new DataMessage("1");
		byte[] msgByte = DataMessage.encodeMessage(d);
		SendData(socket, msgByte);
	}
	private static void sendHave(Socket socket, String remotePeerID) {
		byte[] encodedBitField = peerProcess.bit.getBytes();
		peerProcess.print(peerProcess.peerId + " sending HAVE message to Peer " + remotePeerID);
		DataMessage d = new DataMessage("5", encodedBitField);
		SendData(socket,DataMessage.encodeMessage(d));
		encodedBitField = null;
	}
	private static int SendData(Socket socket, byte[] encodedBitField) {
		try {
		OutputStream out = socket.getOutputStream();
		out.write(encodedBitField);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	public static void readPeerInfoAgain()
	{
		try 
		{
			String st;
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			while ((st = in.readLine()) != null)
			{
				String[]args = st.trim().split("\\s+");
				String peerID = args[0];
				int isCompleted = Integer.parseInt(args[3]);
				if(isCompleted == 1)
				{
					peerProcess.peersMap.get(peerID).isCompleted = 1;
					peerProcess.peersMap.get(peerID).isInterested = 0;
					peerProcess.peersMap.get(peerID).isChoked = 0;
				}
			}
			in.close();
		}
		catch (Exception e) {
			peerProcess.print(peerProcess.peerId + e.toString());
		}
	}
	public void run() 
	{
		
		//updates remotePeerInfoHash
		//readPeerInfoAgain();
		if(!peerProcess.unchokedNeighbors.isEmpty())
			peerProcess.unchokedNeighbors.clear();
		//Enumeration<String> keys = peersMap.keys();
		Vector<RemotePeerInfo> peers = new Vector<RemotePeerInfo>();
		for(String key : peerProcess.peersMap.keySet())
		{
			//String key = (String)keys.nextElement();
			RemotePeerInfo pref = peerProcess.peersMap.get(key);
			if (pref.isChoked == 1 
					&& !key.equals(peerProcess.peerId) 
					&& pref.isCompleted == 0 
					&& pref.isHandShaked == 1)
				peers.add(pref);
		}
		
		// Randomize the vector elements 	
		if (peers.size() > 0)
		{
			Collections.shuffle(peers);
			RemotePeerInfo p = peers.firstElement();
			
			peerProcess.peersMap.get(p.peerId).isOptUnchokedNeighbor = 1;
			peerProcess.unchokedNeighbors.put(p.peerId, peerProcess.peersMap.get(p.peerId));
			// LOG 4:
			peerProcess.print(peerProcess.peerId + " has the optimistically unchoked neighbor " + p.peerId);
			
			if (peerProcess.peersMap.get(p.peerId).isChoked == 1)
			{
				peerProcess.peersMap.get(p.peerId).isChoked = 0;
				sendUnChoke(peerProcess.socketMap.get(p.peerId), p.peerId);
				sendHave(peerProcess.socketMap.get(p.peerId), p.peerId);
				peerProcess.peersMap.get(p.peerId).state = 3;
			}
		}
		
	}
}
