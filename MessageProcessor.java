import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;


public class MessageProcessor implements Runnable 
{
	private static boolean running = true;
	private static String PeerID_this = null;
	public static int peerState = -1;
	RandomAccessFile raf;
	
	// constructor
	public MessageProcessor(String PeerID_pthis)
	{
		PeerID_this = PeerID_pthis;
	}
	
	// constructor
	public MessageProcessor()
	{
		PeerID_this = null;
	}
	
	public void pTS(String dataType, int state)
	{
		peerProcess.print("Message Processor : msgType = "+ dataType + " State = "+state);
	}

	public void run()
	{
		Data d;
		DataMessageWrapper dataWrapper;
		String msgType;
		String rPeerId;
				
		while(running)
		{
			//dataWrapper  = peerProcess.removeFromMsgQueue();
			DataMessageWrapper msg = null;
			if(!peerProcess.queue.isEmpty())
			{
				msg = peerProcess.queue.remove();
			}
			dataWrapper = msg;
			while(dataWrapper == null)
			{
				Thread.currentThread();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				   e.printStackTrace();
				}
				DataMessageWrapper msg2 = null;
				if(!peerProcess.queue.isEmpty())
				{
					msg2 = peerProcess.queue.remove();
				}
				dataWrapper = msg2;
				//dataWrapper  = peerProcess.removeFromMsgQueue();
			}
			
			d = dataWrapper.getDataMsg();
			
			msgType = d.typeOfMsg;
			rPeerId = dataWrapper.getFromPeerID();
			int state = peerProcess.peersMap.get(rPeerId).state;
			if(msgType.equals("4") && state != 14)
			{
				// LOG 7: Receive Message from peer
				peerProcess.print(peerProcess.peerId + " receieved HAVE message from Peer " + rPeerId); 
				if(isInterested(d, rPeerId))
				{
					//peerProcess.showLog(peerProcess.peerID + " is interested in Peer " + rPeerId);
					sendInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
					peerProcess.peersMap.get(rPeerId).state = 9;
				}	
				else
				{
					//peerProcess.showLog(peerProcess.peerID + "is not interested " + rPeerId);
					sendNotInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
					peerProcess.peersMap.get(rPeerId).state = 13;
				}
			}
			else
			{
			 switch (state)
			 {
			 
			 case 2:
			   if (msgType.equals("5")) {
		 		  peerProcess.print(peerProcess.peerId + " receieved a BITFIELD message from Peer " + rPeerId);
	 			  sendBitField(peerProcess.socketMap.get(rPeerId), rPeerId);
 				  peerProcess.peersMap.get(rPeerId).state = 3;  
			   }
			   break;
			 
			 case 3:
			   if (msgType.equals("3")) {
				 // LOG 9:
					peerProcess.print(peerProcess.peerId + " receieved a NOT INTERESTED message from Peer " + rPeerId);
					peerProcess.peersMap.get(rPeerId).isInterested = 0;
					peerProcess.peersMap.get(rPeerId).state = 5;
					peerProcess.peersMap.get(rPeerId).isHandShaked = 1;
			   }
			   else if (msgType.equals("2")) {
				// LOG 8:
					peerProcess.print(peerProcess.peerId + " receieved an INTERESTED message from Peer " + rPeerId);
					peerProcess.peersMap.get(rPeerId).isInterested = 1;
					peerProcess.peersMap.get(rPeerId).isHandShaked = 1;
					
					if(!peerProcess.prefMap.containsKey(rPeerId) && !peerProcess.unchokedNeighbors.containsKey(rPeerId))
					{
						sendChoke(peerProcess.socketMap.get(rPeerId), rPeerId);
						peerProcess.peersMap.get(rPeerId).isChoked = 1;
						peerProcess.peersMap.get(rPeerId).state  = 6;
					}
					else
					{
						peerProcess.peersMap.get(rPeerId).isChoked = 0;
						sendUnChoke(peerProcess.socketMap.get(rPeerId), rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 4 ;
					}
			   }
			   break;
			   
			 case 4:
				 if (msgType.equals("6")) {
					//peerProcess.showLog(peerProcess.peerID + " receieved a REQUEST message from Peer " + rPeerId);
						sendPeice(peerProcess.socketMap.get(rPeerId), d, rPeerId);
						// Decide to send CHOKE or UNCHOKE message
						if(!peerProcess.prefMap.containsKey(rPeerId) && !peerProcess.unchokedNeighbors.containsKey(rPeerId))
						{
							sendChoke(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).isChoked = 1;
							peerProcess.peersMap.get(rPeerId).state = 6;
						}  
				 }
				 break;
				 
			 case 8:
				 if (msgType.equals("5")) {
						//Decide if interested or not.
						if(isInterested(d,rPeerId))
						{
							//peerProcess.showLog(peerProcess.peerID + " is interested in Peer " + rPeerId);
							sendInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 9;
						}	
						else
						{
							//peerProcess.showLog(peerProcess.peerID + " is not interested in Peer " + rPeerId);
							sendNotInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 13;
						}
				 }
				 break;
				 
			 case 9:
				 if (msgType.equals("0")) {
					// LOG 6:
						peerProcess.print(peerProcess.peerId + " is CHOKED by Peer " + rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 14;
				 }
				 else if (msgType.equals("1")) {
					// LOG 5:
						peerProcess.print(peerProcess.peerId + " is UNCHOKED by Peer " + rPeerId);
						int firstdiff = peerProcess.bit.returnFirstDiff(peerProcess.peersMap.get(rPeerId).bitField);
						if(firstdiff != -1)
						{
							//peerProcess.showLog(peerProcess.peerID + " is Requesting PIECE " + firstdiff + " from peer " + rPeerId);
							sendRequest(peerProcess.socketMap.get(rPeerId), firstdiff, rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 11;
							// Get the time when the request is being sent.
							peerProcess.peersMap.get(rPeerId).startTime = new Date();
						}
						else
							peerProcess.peersMap.get(rPeerId).state = 13;
				 }
				 break;
				 
			 case 11:
				 if (msgType.equals("7")) {
					    byte[] buffer = d.content;						
						peerProcess.peersMap.get(rPeerId).finishTime = new Date();
						long timeLapse = peerProcess.peersMap.get(rPeerId).finishTime.getTime() - 
									peerProcess.peersMap.get(rPeerId).startTime.getTime() ;
						
						peerProcess.peersMap.get(rPeerId).dataRate = ((double)(buffer.length + 4 + 1)/(double)timeLapse) * 100;
						
						Piece p = Piece.decodePiece(buffer);
						peerProcess.bit.updateBitField(rPeerId, p);			
						
						int toGetPeiceIndex = peerProcess.bit.returnFirstDiff(peerProcess.peersMap.get(rPeerId).bitField);
						if(toGetPeiceIndex != -1)
						{
							//peerProcess.showLog(peerProcess.peerID + " Requesting piece " + toGetPeiceIndex + " from peer " + rPeerId);
							sendRequest(peerProcess.socketMap.get(rPeerId),toGetPeiceIndex, rPeerId);
							peerProcess.peersMap.get(rPeerId).state  = 11;
							// Get the time when the request is being sent.
							peerProcess.peersMap.get(rPeerId).startTime = new Date();
						}
						else
							peerProcess.peersMap.get(rPeerId).state = 13;
						
						//updates remote peerInfo
						//peerProcess.readPeerInfoAgain();
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
						
						//Enumeration<String> keys = peerProcess.peersMap.keys();
						for (String key : peerProcess.peersMap.keySet())
						{
							//String key = (String)keys.nextElement();
							RemotePeerInfo pref = peerProcess.peersMap.get(key);
							
							if(key.equals(peerProcess.peerId))continue;
							//peerProcess.showLog(peerProcess.peerID + ":::: isCompleted =" + pref.isCompleted + " isInterested =" + pref.isInterested + " isChoked =" + pref.isChoked);
							if (pref.isCompleted == 0 && pref.isChoked == 0 && pref.isHandShaked == 1)
							{
								//peerProcess.showLog(peerProcess.peerID + " isCompleted =" + pref.isCompleted + " isInterested =" + pref.isInterested + " isChoked =" + pref.isChoked);
								sendHave(peerProcess.socketMap.get(key), key);
								peerProcess.peersMap.get(key).state = 3;
								
							} 
							
						}
										
						buffer = null;
						d = null;
			
				 }
				 else if (msgType.equals("0")) {
					// LOG 6:
						peerProcess.print(peerProcess.peerId + " is CHOKED by Peer " + rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 14;
				 }
				 break;
				 
			 case 14:
				 if (msgType.equals("4")) {
						//Decide if interested or not.
						if(isInterested(d,rPeerId))
						{
							//peerProcess.showLog(peerProcess.peerID + " is interested in Peer " + rPeerId);
							sendInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 9;
						}	
						else
						{
							//peerProcess.showLog(peerProcess.peerID + " is not interested in Peer " + rPeerId);
							sendNotInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 13;
						}
				 }
				 else if (msgType.equals("1")) {
                        // LOG 5:
						peerProcess.print(peerProcess.peerId + " is UNCHOKED by Peer " + rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 14;
				 }
				 break;
				 
			 }
			}

		}
	}

	 private byte[] intTobyte(int i) {
	        return new byte[] {  
	                (byte) ((i >> 24) & 0xFF),  
	                (byte) ((i >> 16) & 0xFF),     
	                (byte) ((i >> 8) & 0xFF),     
	                (byte) (i & 0xFF)  
	            };  
	    }
	 
	 private int byteToint(byte[] b1) {
	        return  b1[3] & 0xFF |  
	                (b1[2] & 0xFF) << 8 |  
	                (b1[1] & 0xFF) << 16 |  
	                (b1[0] & 0xFF) << 24;  
	    }
	 
	private void sendRequest(Socket socket, int pieceNo, String remotePeerID) {

		// Byte2int....
		byte[] pieceByte = new byte[4];
		for (int i = 0; i < 4; i++) {
			pieceByte[i] = 0;
		}

		byte[] pieceIndexByte = Conversion.intToByteArray(pieceNo);
		System.arraycopy(pieceIndexByte, 0, pieceByte, 0,
						pieceIndexByte.length);
		Data d = new Data("6", pieceByte);
		byte[] b = Data.encodeMessage(d);
		SendData(socket, b);

		pieceByte = null;
		pieceIndexByte = null;
		b = null;
		d = null;
	}

	private void sendPeice(Socket socket, Data d, String remotePeerID)  //d == requestmessage
	{
		byte[] bytePieceIndex = d.content;
		int pieceIndex = Conversion.byteArrayToInt(bytePieceIndex, 0);
		
		peerProcess.print(peerProcess.peerId + " sending a PIECE message for piece " + pieceIndex + " to Peer " + remotePeerID);
		
		byte[] byteRead = new byte[peerProcess.pieceSize];
		int noBytesRead = 0;
		
		File file = new File(peerProcess.peerId,peerProcess.fileName);
		try 
		{
			raf = new RandomAccessFile(file,"r");
			raf.seek(pieceIndex*peerProcess.pieceSize);
			noBytesRead = raf.read(byteRead, 0, peerProcess.pieceSize);
		} 
		catch (IOException e) 
		{
			peerProcess.print(peerProcess.peerId + " ERROR in reading the file : " +  e.toString());
		}
		if( noBytesRead == 0)
		{
			peerProcess.print(peerProcess.peerId + " ERROR :  Zero bytes read from the file !");
		}
		else if (noBytesRead < 0)
		{
			peerProcess.print(peerProcess.peerId + " ERROR : File could not be read properly.");
		}
		
		byte[] buffer = new byte[noBytesRead + 4];
		System.arraycopy(bytePieceIndex, 0, buffer, 0, 4);
		System.arraycopy(byteRead, 0, buffer, 4, noBytesRead);

		Data sendMessage = new Data("7", buffer);
		byte[] b =  Data.encodeMessage(sendMessage);
		SendData(socket, b);
		
		//release memory
		buffer = null;
		byteRead = null;
		b = null;
		bytePieceIndex = null;
		sendMessage = null;
		
		try{
			raf.close();
		}
		catch(Exception e){}
	}
	
	private void sendNotInterested(Socket socket, String remotePeerID) 
	{
		peerProcess.print(peerProcess.peerId + " sending a NOT INTERESTED message to Peer " + remotePeerID);
		Data d =  new Data("3");
		byte[] msgByte = Data.encodeMessage(d);
		SendData(socket,msgByte);
	}

	private void sendInterested(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending an INTERESTED message to Peer " + remotePeerID);
		Data d =  new Data("2");
		byte[] msgByte = Data.encodeMessage(d);
		SendData(socket,msgByte);		
	}

	
	private boolean isInterested(Data d, String rPeerId) {		
		
		Conversion b = Conversion.convert(d.content);
		peerProcess.peersMap.get(rPeerId).bitField = b;

		//if(peerProcess.bit.compare(b))  return true;
		
		
		int yourSize = b.len;
		boolean flag = false;

		for (int i = 0; i < yourSize; i++) 
		{
			if (b.arr[i].getIsPresent() == 1
					&& peerProcess.bit.arr[i].getIsPresent() == 0) 
			{
				flag = true;
			} else
				continue;
		}

		//return false;
		
		
		return flag;
	}

	private void sendUnChoke(Socket socket, String remotePeerID) {

		peerProcess.print(peerProcess.peerId + " sending UNCHOKE message to Peer " + remotePeerID);
		Data d = new Data("1");
		byte[] msgByte = Data.encodeMessage(d);
		SendData(socket,msgByte);
	}

	private void sendChoke(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending CHOKE message to Peer " + remotePeerID);
		Data d = new Data("0");
		byte[] msgByte = Data.encodeMessage(d);
		SendData(socket,msgByte);
	}

	private void sendBitField(Socket socket, String remotePeerID) {
	
		peerProcess.print(peerProcess.peerId + " sending BITFIELD message to Peer " + remotePeerID);
		byte[] encodedBitField = peerProcess.bit.getBytes();

		Data d = new Data("5", encodedBitField);
		SendData(socket,Data.encodeMessage(d));
		
		encodedBitField = null;
	}
	
	
	private void sendHave(Socket socket, String remotePeerID) {
		
		peerProcess.print(peerProcess.peerId + " sending HAVE message to Peer " + remotePeerID);
		byte[] encodedBitField = peerProcess.bit.getBytes();
		Data d = new Data("4", encodedBitField);
		SendData(socket,Data.encodeMessage(d));
		
		encodedBitField = null;
	}
	
	private int SendData(Socket socket, byte[] encodedBitField) {
		try {
		OutputStream out = socket.getOutputStream();
		out.write(encodedBitField);
		} catch (IOException e) {
            e.printStackTrace();
			return 0;
		}
		return 1;
	}
	

}