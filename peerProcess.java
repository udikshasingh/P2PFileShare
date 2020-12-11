import java.io.*;
import java.net.*;
import java.util.*;
public class peerProcess implements MessageConstants
{
	public static Map<String, RemotePeerInfo> peersMap = new HashMap<>();
	public static Map<String, RemotePeerInfo> prefMap = new HashMap<>();
	
	public ServerSocket socket = null;
	public int portNo;
	public static String peerId;
	public int serialNo;
	public Thread server; 
	public static boolean completed = false;
	public static BitField bit = null;
	public static Timer timer;
	public static volatile Timer timerUnChok;
	
	public static volatile Hashtable<String, RemotePeerInfo> unchokedNeighbors = new Hashtable<String, RemotePeerInfo>();
	public static  Queue<DataMessageWrapper> messageQ = new LinkedList<>();
	public static Map<String, Socket> socketMap = new HashMap<>();
	public static List<Thread> listeners = new ArrayList<Thread>();
	public static List<Thread> sendingThread = new ArrayList<Thread>();
	public static Thread mainThread;
	public static synchronized void offer(DataMessageWrapper msg)
	{
		messageQ.add(msg);
	}
	
	
	public static void showLog(String message)
	{
		LogGenerator.writeLog(DateUtil.getTime() + ": Peer " + message);
		System.out.println(DateUtil.getTime() + ": Peer " + message);
	}
	
	
	
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
			timer.schedule(new UnChoked(), Configurations.optimisticUnchokingInterval * 1000 * 0, Configurations.optimisticUnchokingInterval * 1000);
			
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
