import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
	ServerSocket listener;
	String pid;
	Socket socket;
	Thread serverProcess;
	
	public Server(ServerSocket socket, String peerID) {
		this.listener = socket;
		this.pid = peerID;
	}
	
	public void run() 
	{
		while(true) {
			try {
				socket = listener.accept();
				serverProcess = new Thread(new RemotePeerHandler(socket,0,pid));
				peerProcess.print(pid + " Connection is established");
				peerProcess.sendingThread.add(serverProcess);
				serverProcess.start(); 
			}
			catch(Exception e) {}
		}
	}
	
	public void shutdown()
	{
		try {
			if(!socket.isClosed())
			socket.close();
		} 
		catch (IOException e) 
		{}
	}
}


