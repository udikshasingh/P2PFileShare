import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

public  class PreferredNeighbors extends TimerTask {
	private static void sendUnChoke(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " is sending UNCHOKE message to remote Peer " + remotePeerID);
		Data d = new Data("1");
		byte[] msgByte = Data.encodeMessage(d);
		SendData(socket, msgByte);
	}
	private static void sendHave(Socket socket, String remotePeerID) {
		byte[] encodedBitField = peerProcess.bit.getBytes();
		peerProcess.print(peerProcess.peerId + " sending HAVE message to Peer " + remotePeerID);
		Data d = new Data("5", encodedBitField);
		SendData(socket,Data.encodeMessage(d));
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
		public void run() 
		{
			//updates remotePeerInfoHash
			//readPeerInfoAgain();
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
			//Enumeration<String> keys = peersMap.keys();
			int countInterested = 0;
			String strPref = "";
			for(String key : peerProcess.peersMap.keySet())
			{
				//String key = (String)keys.nextElement();
				RemotePeerInfo pref = peerProcess.peersMap.get(key);
				if(key.equals(peerProcess.peerId))continue;
				if (pref.isCompleted == 0 && pref.isHandShaked == 1)
				{
					countInterested++;
				} 
				else if(pref.isCompleted == 1)
				{
					try
					{
						peerProcess.prefMap.remove(key);
					}
					catch (Exception e) {
					}
				}
			}
			if(countInterested > peerProcess.numberOfPreferredNeighbors)
			{
				boolean flag = peerProcess.prefMap.isEmpty();
				if(!flag)
					peerProcess.prefMap.clear();
				List<RemotePeerInfo> pv = new ArrayList<RemotePeerInfo>(peerProcess.peersMap.values());
				Collections.sort(pv, new PeerDataRateComparator(false));
				int count = 0;
				for (int i = 0; i < pv.size(); i++) 
				{
					if (count > peerProcess.numberOfPreferredNeighbors - 1)
						break;
					if(pv.get(i).isHandShaked == 1 && !pv.get(i).peerId.equals(peerProcess.peerId) 
							&& peerProcess.peersMap.get(pv.get(i).peerId).isCompleted == 0)
					{
						peerProcess.peersMap.get(pv.get(i).peerId).isPreferredNeighbor = 1;
						peerProcess.prefMap.put(pv.get(i).peerId, peerProcess.peersMap.get(pv.get(i).peerId));
						
						count++;
						
						strPref = strPref + pv.get(i).peerId + ", ";
						
						if (peerProcess.peersMap.get(pv.get(i).peerId).isChoked == 1)
						{
							sendUnChoke(peerProcess.socketMap.get(pv.get(i).peerId), pv.get(i).peerId);
							peerProcess.peersMap.get(pv.get(i).peerId).isChoked = 0;
							sendHave(peerProcess.socketMap.get(pv.get(i).peerId), pv.get(i).peerId);
							peerProcess.peersMap.get(pv.get(i).peerId).state = 3;
						}
						
						
					}
				}
			}
			else
			{
				//keys = peersMap.keys();
				for (String key : peerProcess.peersMap.keySet())
				{
					//String key = (String)keys.nextElement();
					RemotePeerInfo pref = peerProcess.peersMap.get(key);
					if(key.equals(peerProcess.peerId)) continue;
					
					if (pref.isCompleted == 0 && pref.isHandShaked == 1)
					{
						if(!peerProcess.prefMap.containsKey(key))
						{
							strPref = strPref + key + ", ";
							peerProcess.prefMap.put(key, peerProcess.peersMap.get(key));
							peerProcess.peersMap.get(key).isPreferredNeighbor = 1;
						}
						if (pref.isChoked == 1)
						{
							sendUnChoke(peerProcess.socketMap.get(key), key);
							peerProcess.peersMap.get(key).isChoked = 0;
							sendHave(peerProcess.socketMap.get(key), key);
							peerProcess.peersMap.get(key).state = 3;
						}
						
					} 
					
				}
			}
			// LOG 3: Preferred Neighbors 
			if (strPref != "")
				peerProcess.print(peerProcess.peerId + " has selected the preferred neighbors - " + strPref);
		}
		
	}