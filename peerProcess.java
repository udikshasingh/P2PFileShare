import java.io.*;
import java.net.*;
import java.util.*;
public class peerProcess implements MessageConstants
{
	public static Map<String, RemotePeerInfo> peersMap = new HashMap<>();
	public static Map<String, RemotePeerInfo> prefMap = new HashMap<>();
	
	public ServerSocket socket = null;
	public int portNo;
	//public String PEER_IP = null;
	public static String peerId;
	public int serialNo;
	public Thread server; 
	public static boolean completed = false;
	public static BitField bit = null;
	public static Timer timer;
	public static volatile Timer timerUnChok;
	
	public static volatile Hashtable<String, RemotePeerInfo> unchokedNeighbors = new Hashtable<String, RemotePeerInfo>();
	public static volatile Queue<DataMessageWrapper> messageQ = new LinkedList<DataMessageWrapper>();
	public static Hashtable<String, Socket> peerIDToSocketMap = new Hashtable<String, Socket>();
	public static List<Thread> listeners = new ArrayList<Thread>();
	public static Vector<Thread> sendingThread = new Vector<Thread>();
	public static Thread mainThread;
	public static synchronized void addToMsgQueue(DataMessageWrapper msg)
	{
		messageQ.add(msg);
	}
	
	public static synchronized DataMessageWrapper removeFromMsgQueue()
	{
		DataMessageWrapper msg = null;
		if(!messageQ.isEmpty())
		{
			msg = messageQ.remove();
		}
		return msg;
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
					peersMap.get(peerID).isCompleted = 1;
					peersMap.get(peerID).isInterested = 0;
					peersMap.get(peerID).isChoked = 0;
				}
			}
			in.close();
		}
		catch (Exception e) {
			showLog(peerId + e.toString());
		}
	}
	/**
	 * Class that handles the preferred neighbors information
	 * Adding the preferred neighbors with highest data rate to the corresponding list
	 */
//	public static class PreferedNeighbors extends TimerTask {
//		public void run() 
//		{
//			//updates remotePeerInfoHash
//			readPeerInfoAgain();
//			//Enumeration<String> keys = peersMap.keys();
//			int countInterested = 0;
//			String strPref = "";
//			for(String key : peersMap.keySet())
//			{
//				//String key = (String)keys.nextElement();
//				RemotePeerInfo pref = peersMap.get(key);
//				if(key.equals(peerId))continue;
//				if (pref.isCompleted == 0 && pref.isHandShaked == 1)
//				{
//					countInterested++;
//				} 
//				else if(pref.isCompleted == 1)
//				{
//					try
//					{
//						prefMap.remove(key);
//					}
//					catch (Exception e) {
//					}
//				}
//			}
//			if(countInterested > Configurations.numberOfPreferredNeighbors)
//			{
//				boolean flag = prefMap.isEmpty();
//				if(!flag)
//					prefMap.clear();
//				List<RemotePeerInfo> pv = new ArrayList<RemotePeerInfo>(peersMap.values());
//				Collections.sort(pv, new PeerDataRateComparator(false));
//				int count = 0;
//				for (int i = 0; i < pv.size(); i++) 
//				{
//					if (count > Configurations.numberOfPreferredNeighbors - 1)
//						break;
//					if(pv.get(i).isHandShaked == 1 && !pv.get(i).peerId.equals(peerId) 
//							&& peersMap.get(pv.get(i).peerId).isCompleted == 0)
//					{
//						peersMap.get(pv.get(i).peerId).isPreferredNeighbor = 1;
//						prefMap.put(pv.get(i).peerId, peersMap.get(pv.get(i).peerId));
//						
//						count++;
//						
//						strPref = strPref + pv.get(i).peerId + ", ";
//						
//						if (peersMap.get(pv.get(i).peerId).isChoked == 1)
//						{
//							sendUnChoke(peerProcess.peerIDToSocketMap.get(pv.get(i).peerId), pv.get(i).peerId);
//							peerProcess.peersMap.get(pv.get(i).peerId).isChoked = 0;
//							sendHave(peerProcess.peerIDToSocketMap.get(pv.get(i).peerId), pv.get(i).peerId);
//							peerProcess.peersMap.get(pv.get(i).peerId).state = 3;
//						}
//						
//						
//					}
//				}
//			}
//			else
//			{
//				//keys = peersMap.keys();
//				for (String key : peersMap.keySet())
//				{
//					//String key = (String)keys.nextElement();
//					RemotePeerInfo pref = peersMap.get(key);
//					if(key.equals(peerId)) continue;
//					
//					if (pref.isCompleted == 0 && pref.isHandShaked == 1)
//					{
//						if(!prefMap.containsKey(key))
//						{
//							strPref = strPref + key + ", ";
//							prefMap.put(key, peersMap.get(key));
//							peersMap.get(key).isPreferredNeighbor = 1;
//						}
//						if (pref.isChoked == 1)
//						{
//							sendUnChoke(peerProcess.peerIDToSocketMap.get(key), key);
//							peerProcess.peersMap.get(key).isChoked = 0;
//							sendHave(peerProcess.peerIDToSocketMap.get(key), key);
//							peerProcess.peersMap.get(key).state = 3;
//						}
//						
//					} 
//					
//				}
//			}
//			// LOG 3: Preferred Neighbors 
//			if (strPref != "")
//				peerProcess.showLog(peerProcess.peerId + " has selected the preferred neighbors - " + strPref);
//		}
//	}
	
	private static void sendUnChoke(Socket socket, String remotePeerID) {
		showLog(peerId + " is sending UNCHOKE message to remote Peer " + remotePeerID);
		DataMessage d = new DataMessage(DATA_MSG_UNCHOKE);
		byte[] msgByte = DataMessage.encodeMessage(d);
		SendData(socket, msgByte);
	}
	private static void sendHave(Socket socket, String remotePeerID) {
		byte[] encodedBitField = peerProcess.bit.encode();
		showLog(peerId + " sending HAVE message to Peer " + remotePeerID);
		DataMessage d = new DataMessage(DATA_MSG_HAVE, encodedBitField);
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


	public static class UnChokedNeighbors extends TimerTask {

		public void run() 
		{
			//updates remotePeerInfoHash
			readPeerInfoAgain();
			if(!unchokedNeighbors.isEmpty())
				unchokedNeighbors.clear();
			//Enumeration<String> keys = peersMap.keys();
			Vector<RemotePeerInfo> peers = new Vector<RemotePeerInfo>();
			for(String key : peersMap.keySet())
			{
				//String key = (String)keys.nextElement();
				RemotePeerInfo pref = peersMap.get(key);
				if (pref.isChoked == 1 
						&& !key.equals(peerId) 
						&& pref.isCompleted == 0 
						&& pref.isHandShaked == 1)
					peers.add(pref);
			}
			
			// Randomize the vector elements 	
			if (peers.size() > 0)
			{
				Collections.shuffle(peers);
				RemotePeerInfo p = peers.firstElement();
				
				peersMap.get(p.peerId).isOptUnchokedNeighbor = 1;
				unchokedNeighbors.put(p.peerId, peersMap.get(p.peerId));
				// LOG 4:
				peerProcess.showLog(peerProcess.peerId + " has the optimistically unchoked neighbor " + p.peerId);
				
				if (peersMap.get(p.peerId).isChoked == 1)
				{
					peerProcess.peersMap.get(p.peerId).isChoked = 0;
					sendUnChoke(peerProcess.peerIDToSocketMap.get(p.peerId), p.peerId);
					sendHave(peerProcess.peerIDToSocketMap.get(p.peerId), p.peerId);
					peerProcess.peersMap.get(p.peerId).state = 3;
				}
			}
			
		}

	}

	
	public static void showLog(String message)
	{
		LogGenerator.writeLog(DateUtil.getTime() + ": Peer " + message);
		System.out.println(DateUtil.getTime() + ": Peer " + message);
	}
	
	
	
	//@SuppressWarnings("deprecation")
	@SuppressWarnings("deprecation")
	public static void main(String[] args) 
	{
		peerProcess peerProcess = new peerProcess();
		peerId = args[0];
		try
		{
			LogGenerator.start("log_peer_" + peerId +".log");
			showLog(peerId + " has now started");
			try {
				BufferedReader br = new BufferedReader(new FileReader("Common.cfg"));
				String str;
				while ((str = br.readLine()) != null) {
					String[] configs = str.split("\\s+");
					String key = configs[0];
					String value = configs[1];
					if (key.equals("NumberOfPreferredNeighbors")) {
						Configurations.numberOfPreferredNeighbors = Integer.parseInt(value);
					} else if (key.equals("UnchokingInterval")) {
						Configurations.unchokingInterval = Integer.parseInt(value);
					} else if (key.equals("OptimisticUnchokingInterval")) {
						Configurations.optimisticUnchokingInterval = Integer.parseInt(value);
					} else if (key.equals("FileName")) {
						Configurations.fileName = value;
					} else if (key.equals("FileSize")) {
						Configurations.fileSize = Integer.parseInt(value);
					} else if (key.equals("PieceSize")) {
						Configurations.pieceSize = Integer.parseInt(value);
					}
				}
				br.close();
			} catch (Exception e) {
				showLog(peerId + e.toString());
			}
			
			try {
				BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));
				int i = 0;
				String str;
				while ((str = br.readLine()) != null) {
					String[] configs = str.split("\\s+");
					boolean flag = (Integer.parseInt(configs[3]) == 1) ? true : false;
					peersMap.put(configs[0], new RemotePeerInfo(configs[0],configs[1], configs[2], flag, i));
					i++;
				}
				br.close();
			} catch (Exception e) {
				showLog(peerId + e.toString());
			}
						
			for (String key : peersMap.keySet()) {
				if(!key.equals(peerId))
					prefMap.put(key, peersMap.get(key));		
			}
			
			boolean isPrimary = false;

			
			for (String key : peersMap.keySet())
			{
				RemotePeerInfo remotePeerInfo = peersMap.get(key);
				if(remotePeerInfo.peerId.equals(peerId))
				{
					peerProcess.portNo = Integer.parseInt(remotePeerInfo.peerPort);
					peerProcess.serialNo = remotePeerInfo.serialNo;
					if(remotePeerInfo.isPrimary) {
						isPrimary = true;
						break;
					}
				}
			}
			
			bit = new BitField();
			int flag = isPrimary?1:0;
			bit.initOwnBitfield(peerId, flag);
			
			mainThread = new Thread(new MessageProcessor(peerId));
			mainThread.start();
			
			if(isPrimary)
			{
				try
				{
					peerProcess.socket = new ServerSocket(peerProcess.portNo);
					peerProcess.server = new Thread(new ListeningThread(peerProcess.socket, peerId));
					peerProcess.server.start();
				}
				catch(SocketTimeoutException e)
				{
					showLog(peerId + e.toString());
					LogGenerator.stop();
					System.exit(0);
				}
				catch(IOException e)
				{
					showLog(peerId + e.toString());
					LogGenerator.stop();
					System.exit(0);
				}
			}
			else
			{	
				try {

					File dataFile = new File(peerId, Configurations.fileName);
					OutputStream outputstream = new FileOutputStream(dataFile, true);
					byte data = 0;
					int size = Configurations.fileSize;
					for (int i = 0; i < size; i++)
						outputstream.write(data);
						outputstream.close();
				} 
				catch (Exception e) {
					showLog(peerId + " Error in making the file : " + e.getMessage());
				}
				for (String key : peersMap.keySet())
				{
					RemotePeerInfo peerInfo = peersMap.get(key);
					if(peerProcess.serialNo > peerInfo.serialNo)
					{
						String ip = peerInfo.peerAddress;
						int port = Integer.parseInt(peerInfo.getPeerPort());
						Thread thread = new Thread(new RemotePeerHandler(ip, port, 1,peerId));
						listeners.add(thread);
						thread.start();
					}
				}

				try {
					peerProcess.socket = new ServerSocket(peerProcess.portNo);
					peerProcess.server = new Thread(new ListeningThread(peerProcess.socket, peerId));
					peerProcess.server.start();
				}
				catch(SocketTimeoutException e)
				{
					showLog(peerId + " gets time out exception in Starting the listening thread: " + e.toString());
					LogGenerator.stop();
					System.exit(0);
				}
				catch(IOException e)
				{
					showLog(peerId + " gets exception in Starting the listening thread: " + peerProcess.portNo + " "+ e.toString());
					LogGenerator.stop();
					System.exit(0);
				}
			}
			
			timer = new Timer();
			timer.schedule(new PreferredNeighbors(), Configurations.unchokingInterval * 1000 * 0, Configurations.unchokingInterval * 1000);

			timer = new Timer();
			timer.schedule(new UnChokedNeighbors(), Configurations.optimisticUnchokingInterval * 1000 * 0, Configurations.optimisticUnchokingInterval * 1000);
			
			while(true)
			{
				completed = completed();
				if (completed) {
					showLog("DOWNLOAD COMPLETE!!.");

					timer.cancel();
					timer.cancel();
					try {
						Thread.currentThread();
						Thread.sleep(2000);
					} catch (InterruptedException e) {}

					if (peerProcess.server.isAlive())
						peerProcess.server.stop();

					if (mainThread.isAlive())
						mainThread.stop();

					for (int i = 0; i < listeners.size(); i++)
						if (listeners.get(i).isAlive())
							listeners.get(i).stop();

					for (int i = 0; i < sendingThread.size(); i++)
						if (sendingThread.get(i).isAlive())
							sendingThread.get(i).stop();

					break;
				} else {
					try {
						Thread.currentThread();
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		catch(Exception e)
		{
			showLog(peerId + " Exception in ending : " + e.getMessage() );
		}
		finally
		{
			LogGenerator.stop();
			System.exit(0);
		}
	}


	
	public static synchronized boolean completed() {

		String str;
		int remaining = 1;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));

			while ((str = br.readLine()) != null) {
				String completedbit = str.trim().split("\\s+")[3];
				remaining = remaining * Integer.parseInt(completedbit);
			}
			if (remaining == 0) {
				br.close();
				return false;
			} else {
				br.close();
				return true;
			}

		} catch (Exception e) {
			showLog(e.toString());
			return false;
		}

	}

}
