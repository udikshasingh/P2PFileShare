import java.net.*;
import java.io.*;

public class RemotePeerHandler implements Runnable 
{
	private Socket peerSocket = null;
	private InputStream in;
	private OutputStream out;
	private int connType;
	
	private HandshakeMessage handshakeMessage;
	
	String ownPeerId, remotePeerId;
	
	final int ACTIVECONN = 1;
	final int PASSIVECONN = 0;

	public void openClose(InputStream i, Socket socket)
	{
		try {
			i.close();
			i = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public RemotePeerHandler(Socket peerSocket, int connType, String ownPeerID) {
		
		this.peerSocket = peerSocket;
		this.connType = connType;
		this.ownPeerId = ownPeerID;
		try 
		{
			in = peerSocket.getInputStream();
			out = peerSocket.getOutputStream();
		} 
		catch (Exception ex) 
		{
			peerProcess.print(this.ownPeerId + " Error : " + ex.getMessage());
		}
	}
	
	public RemotePeerHandler(String add, int port, int connType, String ownPeerID) 
	{	
		try 
		{
			this.connType = connType;
			this.ownPeerId = ownPeerID;
			this.peerSocket = new Socket(add, port);			
		} 
		catch (UnknownHostException e) 
		{
			peerProcess.print(ownPeerID + " RemotePeerHandler : " + e.getMessage());
		} 
		catch (IOException e) 
		{
			peerProcess.print(ownPeerID + " RemotePeerHandler : " + e.getMessage());
		}
		this.connType = connType;
		
		try 
		{
			in = peerSocket.getInputStream();
			out = peerSocket.getOutputStream();
		} 
		catch (Exception ex) 
		{
			peerProcess.print(ownPeerID + " RemotePeerHandler : " + ex.getMessage());
		}
	}
	
	public boolean SendHandshake() 
	{
		try 
		{
			out.write(HandshakeMessage.encodeMessage(new HandshakeMessage("P2PFILESHARINGPROJ", this.ownPeerId)));
		} 
		catch (IOException e) 
		{
			peerProcess.print(this.ownPeerId + " SendHandshake : " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean ReceiveHandshake() 
	{
		byte[] receivedHandshakeByte = new byte[32];
		try 
		{
			in.read(receivedHandshakeByte);
			handshakeMessage = HandshakeMessage.decodeMessage(receivedHandshakeByte);
			remotePeerId = handshakeMessage.getPeerIDString();
			
			//populate peerID to socket mapping
			peerProcess.socketMap.put(remotePeerId, this.peerSocket);
		} 
		catch (IOException e) 
		{
			peerProcess.print(this.ownPeerId + " ReceiveHandshake : " + e.getMessage());
			return false;
		}
		return true;
	}		

	public boolean SendRequest(int index)
	{
		try 
		{
			out.write(DataMessage.encodeMessage(new DataMessage( "6", Conversion.intToByteArray(index))));
		} 
		catch (IOException e) 
		{
			peerProcess.print(this.ownPeerId + " SendRequest : " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean SendInterested()
	{
		try{
			out.write(DataMessage.encodeMessage(new DataMessage("2")));
		} 
		catch (IOException e){
			peerProcess.print(this.ownPeerId + " SendInterested : " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean SendNotInterested()
	{
		try{
			out.write(DataMessage.encodeMessage(new DataMessage( "3")));
		} 
		catch (IOException e){
			peerProcess.print(this.ownPeerId + " SendNotInterested : " + e.getMessage());
			return false;
		}
		
		return true;
	}
	
	public boolean ReceiveUnchoke()
	{
		byte [] receiveUnchokeByte = null;
		
		try 
		{
			in.read(receiveUnchokeByte);
		} 
		catch (IOException e){
			peerProcess.print(this.ownPeerId + " ReceiveUnchoke : " + e.getMessage());
			return false;
		}
				
		DataMessage m = DataMessage.decodeMessage(receiveUnchokeByte);
		if(m.typeOfMsg.equals("1")){
			peerProcess.print(ownPeerId + "is unchoked by " + remotePeerId);
			return true;
		}
		else 
			return false;
	}
	
	public boolean ReceiveChoke()
	{
		byte [] receiveChokeByte = null;
	
		// Check whether the in stream has data to be read or not.
		try{
			if(in.available() == 0) return false;
		} 
		catch (IOException e){
			peerProcess.print(this.ownPeerId + " ReceiveChoke : " + e.getMessage());
			return false;
		}
		try{
			in.read(receiveChokeByte);
		} 
		catch (IOException e){
			peerProcess.print(this.ownPeerId + " ReceiveChoke : " + e.getMessage());
			return false;
		}
		DataMessage m = DataMessage.decodeMessage(receiveChokeByte);
		if(m.typeOfMsg.equals("0"))
		{
			// LOG 6:
			peerProcess.print(ownPeerId + " is CHOKED by " + remotePeerId);
			return true;
		}
		else 
			return false;
	}
	
	public boolean receivePeice()
	{
		byte [] receivePeice = null;
		
		try 
		{
			in.read(receivePeice);
		} 
		catch (IOException e) 
		{
			peerProcess.print(this.ownPeerId + " receivePeice : " + e.getMessage());
			return false;
		}
				
		DataMessage m = DataMessage.decodeMessage(receivePeice);
		if(m.typeOfMsg.equals("1"))
		{	
			// LOG 5:
			peerProcess.print(ownPeerId + " is UNCHOKED by " + remotePeerId);
			return true;
		}
		else 
			return false;

	}
	
	public void run() 
	{	
		byte []handshakeBuff = new byte[32];
		byte []dataBuffWithoutPayload = new byte[4 + 1];
		byte[] msgLength;
		byte[] msgType;
		DataMessageWrapper dataMsgWrapper = new DataMessageWrapper();

		try
		{
			if(this.connType == ACTIVECONN)
			{
				if(!SendHandshake())
				{
					peerProcess.print(ownPeerId + " HANDSHAKE sending failed.");
					System.exit(0);
				}
				else
				{
					peerProcess.print(ownPeerId + " HANDSHAKE has been sent...");
				}
				while(true)
				{
					in.read(handshakeBuff);
					handshakeMessage = HandshakeMessage.decodeMessage(handshakeBuff);
					if(handshakeMessage.getHeaderString().equals("P2PFILESHARINGPROJ"))
					{
						
						remotePeerId = handshakeMessage.getPeerIDString();
						
						peerProcess.print(ownPeerId + " makes a connection to Peer " + remotePeerId);
						
						peerProcess.print(ownPeerId + " Received a HANDSHAKE message from Peer " + remotePeerId);
						
						//populate peerID to socket mapping
						peerProcess.socketMap.put(remotePeerId, this.peerSocket);
						break;
					}
					else
					{
						continue;
					}		
				}
				
				// Sending BitField...
				DataMessage d = new DataMessage("5", peerProcess.bit.getBytes());
				byte  []b = DataMessage.encodeMessage(d);  
				out.write(b);
				peerProcess.peersMap.get(remotePeerId).state = 8;
			}
			//Passive connection
			else
			{
				while(true)
				{
					in.read(handshakeBuff);
					handshakeMessage = HandshakeMessage.decodeMessage(handshakeBuff);
					if(handshakeMessage.getHeaderString().equals("P2PFILESHARINGPROJ"))
					{
						remotePeerId = handshakeMessage.getPeerIDString();
						
						peerProcess.print(ownPeerId + " makes a connection to Peer " + remotePeerId);
						peerProcess.print(ownPeerId + " Received a HANDSHAKE message from Peer " + remotePeerId);
						
						//populate peerID to socket mapping
						peerProcess.socketMap.put(remotePeerId, this.peerSocket);
						break;
					}
					else
					{
						continue;
					}		
				}
				if(!SendHandshake())
				{
					peerProcess.print(ownPeerId + " HANDSHAKE message sending failed.");
					System.exit(0);
				}
				else
				{
					peerProcess.print(ownPeerId + " HANDSHAKE message has been sent successfully.");
				}
				
				peerProcess.peersMap.get(remotePeerId).state = 2;
			}
			// receive data messages continuously 
			while(true)
			{
				
				int headerBytes = in.read(dataBuffWithoutPayload);
				
				if(headerBytes == -1)
					break;

				msgLength = new byte[4];
				msgType = new byte[1];
				System.arraycopy(dataBuffWithoutPayload, 0, msgLength, 0, 4);
				System.arraycopy(dataBuffWithoutPayload, 4, msgType, 0, 1);
				DataMessage dataMessage = new DataMessage();
				//dataMessage.setMessageLength(msgLength);
				Integer l = Conversion.byteArrayToInt(msgLength, 0);
				dataMessage.size = l.toString();
				dataMessage.bytearrl = msgLength;
				dataMessage.sized = l;
				//dataMessage.setMessageType(msgType);
				try {
					dataMessage.typeOfMsg = new String(msgType, "UTF8");
					dataMessage.bytearrt = msgType;
				} catch (UnsupportedEncodingException e) {
					peerProcess.print(e.toString());
				}
				if(dataMessage.typeOfMsg.equals("0")
						||dataMessage.typeOfMsg.equals("1")
						||dataMessage.typeOfMsg.equals("2")
						||dataMessage.typeOfMsg.equals("3")){
					dataMsgWrapper.dataMsg = dataMessage;
					dataMsgWrapper.fromPeerID = this.remotePeerId;
					peerProcess.offer(dataMsgWrapper);
				}
				else {
					int bytesAlreadyRead = 0;
					int bytesRead;
					byte []dataBuffPayload = new byte[dataMessage.sized-1];
					while(bytesAlreadyRead < dataMessage.sized-1){
						bytesRead = in.read(dataBuffPayload, bytesAlreadyRead, dataMessage.sized-1-bytesAlreadyRead);
						if(bytesRead == -1)
							return;
						bytesAlreadyRead += bytesRead;
					}
					
					byte []dataBuffWithPayload = new byte [dataMessage.sized+4];
					System.arraycopy(dataBuffWithoutPayload, 0, dataBuffWithPayload, 0, 4 + 1);
					System.arraycopy(dataBuffPayload, 0, dataBuffWithPayload, 4 + 1, dataBuffPayload.length);
					
					DataMessage dataMsgWithPayload = DataMessage.decodeMessage(dataBuffWithPayload);
					dataMsgWrapper.dataMsg = dataMsgWithPayload;
					dataMsgWrapper.fromPeerID = remotePeerId;
					peerProcess.offer(dataMsgWrapper);
					dataBuffPayload = null;
					dataBuffWithPayload = null;
					bytesAlreadyRead = 0;
					bytesRead = 0;
				}
			}
		}
		catch(IOException e){
			peerProcess.print(ownPeerId + " run exception: " + e);
		}	
		
	}
	
	public void releaseSocket() {
		try {
			if (this.connType == PASSIVECONN && this.peerSocket != null) {
				this.peerSocket.close();
			}
			if (in != null) {
				in.close();
			}
			if (out != null)
				out.close();
		} catch (IOException e) {
			peerProcess.print(ownPeerId + " Release socket IO exception: " + e);
		}
	}
}