import java.io.*;
import java.net.*;
import java.util.*;
public class peerProcess implements MessageConstants
{
	public ServerSocket listeningSocket = null;
	public int LISTENING_PORT;
	public String PEER_IP = null;
	public static String peerId;
	public int myPeerIndex;
	public Thread listeningThread; // Thread for listening to remote clients
	public static boolean isFinished = false;
	public static BitField ownBitField = null;
	public static volatile Timer timerPref;
	public static volatile Timer timerUnChok;
	public static Map<String, RemotePeerInfo> peersMap = new HashMap<>();
	public static volatile Hashtable<String, RemotePeerInfo> preferedNeighbors = new Hashtable<String, RemotePeerInfo>();
	public static volatile Hashtable<String, RemotePeerInfo> unchokedNeighbors = new Hashtable<String, RemotePeerInfo>();
	public static volatile Queue<DataMessageWrapper> messageQ = new LinkedList<DataMessageWrapper>();
	public static Hashtable<String, Socket> peerIDToSocketMap = new Hashtable<String, Socket>();
	public static Vector<Thread> receivingThread = new Vector<Thread>();
	public static Vector<Thread> sendingThread = new Vector<Thread>();
	public static Thread messageProcessor;
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
	public static class PreferedNeighbors extends TimerTask {
		public void run() 
		{
			//updates remotePeerInfoHash
			readPeerInfoAgain();
			//Enumeration<String> keys = peersMap.keys();
			int countInterested = 0;
			String strPref = "";
			for(String key : peersMap.keySet())
			{
				//String key = (String)keys.nextElement();
				RemotePeerInfo pref = peersMap.get(key);
				if(key.equals(peerId))continue;
				if (pref.isCompleted == 0 && pref.isHandShaked == 1)
				{
					countInterested++;
				} 
				else if(pref.isCompleted == 1)
				{
					try
					{
						preferedNeighbors.remove(key);
					}
					catch (Exception e) {
					}
				}
			}
			if(countInterested > Configurations.numberOfPreferredNeighbors)
			{
				boolean flag = preferedNeighbors.isEmpty();
				if(!flag)
					preferedNeighbors.clear();
				List<RemotePeerInfo> pv = new ArrayList<RemotePeerInfo>(peersMap.values());
				Collections.sort(pv, new PeerDataRateComparator(false));
				int count = 0;
				for (int i = 0; i < pv.size(); i++) 
				{
					if (count > Configurations.numberOfPreferredNeighbors - 1)
						break;
					if(pv.get(i).isHandShaked == 1 && !pv.get(i).peerId.equals(peerId) 
							&& peersMap.get(pv.get(i).peerId).isCompleted == 0)
					{
						peersMap.get(pv.get(i).peerId).isPreferredNeighbor = 1;
						preferedNeighbors.put(pv.get(i).peerId, peersMap.get(pv.get(i).peerId));
						
						count++;
						
						strPref = strPref + pv.get(i).peerId + ", ";
						
						if (peersMap.get(pv.get(i).peerId).isChoked == 1)
						{
							sendUnChoke(peerProcess.peerIDToSocketMap.get(pv.get(i).peerId), pv.get(i).peerId);
							peerProcess.peersMap.get(pv.get(i).peerId).isChoked = 0;
							sendHave(peerProcess.peerIDToSocketMap.get(pv.get(i).peerId), pv.get(i).peerId);
							peerProcess.peersMap.get(pv.get(i).peerId).state = 3;
						}
						
						
					}
				}
			}
			else
			{
				//keys = peersMap.keys();
				for (String key : peersMap.keySet())
				{
					//String key = (String)keys.nextElement();
					RemotePeerInfo pref = peersMap.get(key);
					if(key.equals(peerId)) continue;
					
					if (pref.isCompleted == 0 && pref.isHandShaked == 1)
					{
						if(!preferedNeighbors.containsKey(key))
						{
							strPref = strPref + key + ", ";
							preferedNeighbors.put(key, peersMap.get(key));
							peersMap.get(key).isPreferredNeighbor = 1;
						}
						if (pref.isChoked == 1)
						{
							sendUnChoke(peerProcess.peerIDToSocketMap.get(key), key);
							peerProcess.peersMap.get(key).isChoked = 0;
							sendHave(peerProcess.peerIDToSocketMap.get(key), key);
							peerProcess.peersMap.get(key).state = 3;
						}
						
					} 
					
				}
			}
			// LOG 3: Preferred Neighbors 
			if (strPref != "")
				peerProcess.showLog(peerProcess.peerId + " has selected the preferred neighbors - " + strPref);
		}
	}
	
	private static void sendUnChoke(Socket socket, String remotePeerID) {
		showLog(peerId + " is sending UNCHOKE message to remote Peer " + remotePeerID);
		DataMessage d = new DataMessage(DATA_MSG_UNCHOKE);
		byte[] msgByte = DataMessage.encodeMessage(d);
		SendData(socket, msgByte);
	}
	private static void sendHave(Socket socket, String remotePeerID) {
		byte[] encodedBitField = peerProcess.ownBitField.encode();
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

	/**
	 * Class that handles the Optimistically unchoked neigbhbors information
	 * 1. Adding the Optimistically unchoked neighors to the corresponding
	 * list; here it is taken as the first neighbor which is in choked state
	 * 
	 */
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

	/**
	 * Methods to start and stop the Prefered Neighbors and Optimistically
	 * unchoked neigbhbors update threads
	 */
	public static void startUnChokedNeighbors() 
	{
		timerPref = new Timer();
		timerPref.schedule(new UnChokedNeighbors(),
				Configurations.optimisticUnchokingInterval * 1000 * 0,
				Configurations.optimisticUnchokingInterval * 1000);
	}

	public static void stopUnChokedNeighbors() {
		timerPref.cancel();
	}

	public static void startPreferredNeighbors() {
		timerPref = new Timer();
		timerPref.schedule(new PreferedNeighbors(),
				Configurations.unchokingInterval * 1000 * 0,
				Configurations.unchokingInterval * 1000);
	}

	public static void stopPreferredNeighbors() {
		timerPref.cancel();
	}

	/**
	 * Generates log message in following format
	 * [Time]: Peer [peer_ID] [message]
	 * @param message
	 */
	public static void showLog(String message)
	{
		LogGenerator.writeLog(DateUtil.getTime() + ": Peer " + message);
		System.out.println(DateUtil.getTime() + ": Peer " + message);
	}
	
	/**
	 * Reads the system details from the Common.cfg file 
	 * and populates to CommonProperties class static variables 
	 */
//	public static void readCommonProperties() {
//		String line;
//		try {
//			BufferedReader in = new BufferedReader(new FileReader("Common.cfg"));
//			while ((line = in.readLine()) != null) {
//				String[] tokens = line.split("\\s+");
//				if (tokens[0].equalsIgnoreCase("NumberOfPreferredNeighbors")) {
//					CommonProperties.numOfPreferredNeighbr = Integer
//							.parseInt(tokens[1]);
//				} else if (tokens[0].equalsIgnoreCase("UnchokingInterval")) {
//					CommonProperties.unchokingInterval = Integer
//							.parseInt(tokens[1]);
//				} else if (tokens[0]
//						.equalsIgnoreCase("OptimisticUnchokingInterval")) {
//					CommonProperties.optUnchokingInterval = Integer
//							.parseInt(tokens[1]);
//				} else if (tokens[0].equalsIgnoreCase("FileName")) {
//					CommonProperties.fileName = tokens[1];
//				} else if (tokens[0].equalsIgnoreCase("FileSize")) {
//					CommonProperties.fileSize = Integer.parseInt(tokens[1]);
//				} else if (tokens[0].equalsIgnoreCase("PieceSize")) {
//					CommonProperties.pieceSize = Integer.parseInt(tokens[1]);
//				}
//			}
//
//			in.close();
//		} catch (Exception ex) {
//			showLog(peerId + ex.toString());
//		}
//	}

	/**
	 * Reads the Peer details from the PeerInfo.cfg file 
	 * and populates to peerInfoVector vector
	 */
	public static void readPeerInfo() {
		String st;
		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			int i = 0;
			while ((st = in.readLine()) != null) {
				String[] tokens = st.split("\\s+");
				peersMap.put(tokens[0], new RemotePeerInfo(tokens[0],
						tokens[1], tokens[2], Integer.parseInt(tokens[3]), i));
				i++;
			}
			in.close();
		} catch (Exception ex) {
			showLog(peerId + ex.toString());
		}
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
			//readPeerInfo();
			
			try {
				BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));
				int i = 0;
				String str;
				while ((str = br.readLine()) != null) {
					String[] configs = str.split("\\s+");
					peersMap.put(configs[0], new RemotePeerInfo(configs[0],configs[1], configs[2], Integer.parseInt(configs[3]), i));
					i++;
				}
				br.close();
			} catch (Exception ex) {
				showLog(peerId + ex.toString());
			}
			
			// for the initial calculation
			initializePrefferedNeighbours();
			
			boolean isFirstPeer = false;

			//Enumeration<String> e = peersMap.keys();
			
			for (String key : peersMap.keySet())
			{
				RemotePeerInfo peerInfo = peersMap.get(key);
				if(peerInfo.peerId.equals(peerId))
				{
					// checks if the peer is the first peer or not
					peerProcess.LISTENING_PORT = Integer.parseInt(peerInfo.peerPort);
					peerProcess.myPeerIndex = peerInfo.peerIndex;
					if(peerInfo.getIsFirstPeer() == 1)
					{
						isFirstPeer = true;
						break;
					}
				}
			}
			
			// Initialize the Bit field class 
			ownBitField = new BitField();
			ownBitField.initOwnBitfield(peerId, isFirstPeer?1:0);
			
			messageProcessor = new Thread(new MessageProcessor(peerId));
			messageProcessor.start();
			
			if(isFirstPeer)
			{
				try
				{
					peerProcess.listeningSocket = new ServerSocket(peerProcess.LISTENING_PORT);
					
					//instantiates and starts Listening Thread
					peerProcess.listeningThread = new Thread(new ListeningThread(peerProcess.listeningSocket, peerId));
					peerProcess.listeningThread.start();
				}
				catch(SocketTimeoutException tox)
				{
					showLog(peerId + " gets time out expetion: " + tox.toString());
					LogGenerator.stop();
					System.exit(0);
				}
				catch(IOException ex)
				{
					showLog(peerId + " gets exception in Starting Listening thread: " + peerProcess.LISTENING_PORT + ex.toString());
					LogGenerator.stop();
					System.exit(0);
				}
			}
			// Not the first peer
			else
			{	
				createEmptyFile();
				
				//e = peersMap.keys();
				for (String key : peersMap.keySet())
				{
					RemotePeerInfo peerInfo = peersMap.get(key);
					if(peerProcess.myPeerIndex > peerInfo.peerIndex)
					{
						Thread tempThread = new Thread(new RemotePeerHandler(
								peerInfo.getPeerAddress(), Integer
										.parseInt(peerInfo.getPeerPort()), 1,
										peerId));
						receivingThread.add(tempThread);
						tempThread.start();
					}
				}

				// Spawns a listening thread
				try
				{
					peerProcess.listeningSocket = new ServerSocket(peerProcess.LISTENING_PORT);
					peerProcess.listeningThread = new Thread(new ListeningThread(peerProcess.listeningSocket, peerId));
					peerProcess.listeningThread.start();
				}
				catch(SocketTimeoutException tox)
				{
					showLog(peerId + " gets time out exception in Starting the listening thread: " + tox.toString());
					LogGenerator.stop();
					System.exit(0);
				}
				catch(IOException ex)
				{
					showLog(peerId + " gets exception in Starting the listening thread: " + peerProcess.LISTENING_PORT + " "+ ex.toString());
					LogGenerator.stop();
					System.exit(0);
				}
			}
			
			startPreferredNeighbors();
			startUnChokedNeighbors();
			
			while(true)
			{
				// checks for termination
				isFinished = isFinished();
				if (isFinished) {
					showLog("All peers have completed downloading the file.");

					stopPreferredNeighbors();
					stopUnChokedNeighbors();

					try {
						Thread.currentThread();
						Thread.sleep(2000);
					} catch (InterruptedException ex) {
					}

					if (peerProcess.listeningThread.isAlive())
						peerProcess.listeningThread.stop();

					if (messageProcessor.isAlive())
						messageProcessor.stop();

					for (int i = 0; i < receivingThread.size(); i++)
						if (receivingThread.get(i).isAlive())
							receivingThread.get(i).stop();

					for (int i = 0; i < sendingThread.size(); i++)
						if (sendingThread.get(i).isAlive())
							sendingThread.get(i).stop();

					break;
				} else {
					try {
						Thread.currentThread();
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
					}
				}
			}
		}
		catch(Exception ex)
		{
			showLog(peerId + " Exception in ending : " + ex.getMessage() );
		}
		finally
		{
			showLog(peerId + " Peer process is exiting..");
			LogGenerator.stop();
			System.exit(0);
		}
	}

	private static void initializePrefferedNeighbours() 
	{
		//Enumeration<String> keys = peersMap.keys();
		for (String key : peersMap.keySet())
		{
			//String key = (String)keys.nextElement();
			if(!key.equals(peerId))
			{
				preferedNeighbors.put(key, peersMap.get(key));		
			}
		}
	}

	/**
	 * Checks if all peer has down loaded the file
	 */
	public static synchronized boolean isFinished() {

		String line;
		int hasFileCount = 1;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					"PeerInfo.cfg"));

			while ((line = in.readLine()) != null) {
				hasFileCount = hasFileCount
						* Integer.parseInt(line.trim().split("\\s+")[3]);
			}
			if (hasFileCount == 0) {
				in.close();
				return false;
			} else {
				in.close();
				return true;
			}

		} catch (Exception e) {
			showLog(e.toString());
			return false;
		}

	}
	
	public static void createEmptyFile() {
		try {
			File dir = new File(peerId);
			dir.mkdir();

			File newfile = new File(peerId, Configurations.fileName);
			OutputStream os = new FileOutputStream(newfile, true);
			byte b = 0;
			
			for (int i = 0; i < Configurations.fileSize; i++)
				os.write(b);
			os.close();
		} 
		catch (Exception e) {
			showLog(peerId + " ERROR in creating the file : " + e.getMessage());
		}

	}
	

}
