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
	 static boolean running = true;
	 static String PeerID_this = null;
	 static int peerState = -1;
	RandomAccessFile raf;
	
	public MessageProcessor(String PeerID_pthis) {
		PeerID_this = PeerID_pthis;
	}
	
	public MessageProcessor() {
		PeerID_this = null;
	}

	public void run() {
		Data d;
		Source dataWrapper;
		String msgType;
		String rPeerId;		
		while(running) {
			Source msg = null;
			if(!peerProcess.queue.isEmpty()) {
				msg = peerProcess.queue.remove();
			}
			dataWrapper = msg;
			while(dataWrapper == null) {
				Thread.currentThread();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				Source msg2 = null;
				if(!peerProcess.queue.isEmpty())
					msg2 = peerProcess.queue.remove();
				dataWrapper = msg2;
			}
			d = dataWrapper.data;
			msgType = d.typeOfMsg;
			rPeerId = dataWrapper.source;
			int state = peerProcess.peersMap.get(rPeerId).state;
			if(msgType.equals("4") && state != 14) {
				peerProcess.print(peerProcess.peerId + " receieved HAVE message from Peer " + rPeerId); 
				if(isInterested(d, rPeerId)) {
					sendInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
					peerProcess.peersMap.get(rPeerId).state = 9;
				}	
				else {
					sendNotInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
					peerProcess.peersMap.get(rPeerId).state = 13;
				}
			}
			else {
			 switch (state) { 
			 case 2:
			   if (msgType.equals("5")) {
		 		  peerProcess.print(peerProcess.peerId + " receieved a BITFIELD message from Peer " + rPeerId);
	 			  sendBitField(peerProcess.socketMap.get(rPeerId), rPeerId);
 				  peerProcess.peersMap.get(rPeerId).state = 3;  
			   }
			   break;
			 case 3:
			   if (msgType.equals("3")) {
					peerProcess.print(peerProcess.peerId + " receieved a NOT INTERESTED message from Peer " + rPeerId);
					peerProcess.peersMap.get(rPeerId).isInterested = 0;
					peerProcess.peersMap.get(rPeerId).state = 5;
					peerProcess.peersMap.get(rPeerId).isHandShaked = 1;
			   }
			   else if (msgType.equals("2")) {
					peerProcess.print(peerProcess.peerId + " receieved an INTERESTED message from Peer " + rPeerId);
					peerProcess.peersMap.get(rPeerId).isInterested = 1;
					peerProcess.peersMap.get(rPeerId).isHandShaked = 1;
					if(!peerProcess.prefMap.containsKey(rPeerId) && !peerProcess.unchokedNeighbors.containsKey(rPeerId)) {
						
						peerProcess.print(peerProcess.peerId + " sending CHOKE message to Peer " + rPeerId);
						Data data = new Data("0");
						byte[] msgByte = Data.encodeMessage(d);
						try {
							OutputStream out = (peerProcess.socketMap.get(rPeerId)).getOutputStream();
							out.write(msgByte);
						} catch (IOException e) {}
						
						peerProcess.peersMap.get(rPeerId).isChoked = 1;
						peerProcess.peersMap.get(rPeerId).state  = 6;
					}
					else {
						peerProcess.peersMap.get(rPeerId).isChoked = 0;
						sendUnChoke(peerProcess.socketMap.get(rPeerId), rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 4 ;
					}
			   }
			   break;
			 case 4:
				 if (msgType.equals("6")) {
						sendPeice(peerProcess.socketMap.get(rPeerId), d, rPeerId);
						if(!peerProcess.prefMap.containsKey(rPeerId) && !peerProcess.unchokedNeighbors.containsKey(rPeerId)) {
							
							peerProcess.print(peerProcess.peerId + " sending CHOKE message to Peer " + rPeerId);
							Data data = new Data("0");
							byte[] msgByte = Data.encodeMessage(d);
							try {
								OutputStream out = (peerProcess.socketMap.get(rPeerId)).getOutputStream();
								out.write(msgByte);
								} catch (IOException e) {}
							
							peerProcess.peersMap.get(rPeerId).isChoked = 1;
							peerProcess.peersMap.get(rPeerId).state = 6;
						}  
				 }
				 break;
			 case 8:
				 if (msgType.equals("5")) {
						if(isInterested(d,rPeerId)) {
							sendInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 9;
						}	
						else {
							sendNotInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 13;
						}
				 }
				 break;
			 case 9:
				 if (msgType.equals("0")) {
						peerProcess.print(peerProcess.peerId + " is CHOKED by Peer " + rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 14;
				 }
				 else if (msgType.equals("1")) {
						peerProcess.print(peerProcess.peerId + " is UNCHOKED by Peer " + rPeerId);
						int firstdiff = peerProcess.bit.returnFirstDiff(peerProcess.peersMap.get(rPeerId).bitField);
						if(firstdiff != -1) {
							sendRequest(peerProcess.socketMap.get(rPeerId), firstdiff, rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 11;
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
						long timeLapse = peerProcess.peersMap.get(rPeerId).finishTime.getTime() - peerProcess.peersMap.get(rPeerId).startTime.getTime() ;
						peerProcess.peersMap.get(rPeerId).dataRate = ((double)(buffer.length + 4 + 1)/(double)timeLapse) * 100;
						Piece p = Piece.decodePiece(buffer);
						peerProcess.bit.updateBitField(rPeerId, p);			
						int toGetPeiceIndex = peerProcess.bit.returnFirstDiff(peerProcess.peersMap.get(rPeerId).bitField);
						
						if(toGetPeiceIndex != -1) {
							sendRequest(peerProcess.socketMap.get(rPeerId),toGetPeiceIndex, rPeerId);
							peerProcess.peersMap.get(rPeerId).state  = 11;
							peerProcess.peersMap.get(rPeerId).startTime = new Date();
						}
						else
							peerProcess.peersMap.get(rPeerId).state = 13;
						try  {
							String st;
							BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
							while ((st = in.readLine()) != null) {
								String[]args = st.trim().split("\\s+");
								String peerID = args[0];
								int isCompleted = Integer.parseInt(args[3]);
								if(isCompleted == 1) {
									peerProcess.peersMap.get(peerID).isCompleted = 1;
									peerProcess.peersMap.get(peerID).isInterested = 0;
									peerProcess.peersMap.get(peerID).isChoked = 0;
								}
							}
							in.close();
						}
						catch (Exception e) {}
						for (String key : peerProcess.peersMap.keySet()) {
							RemotePeerInfo pref = peerProcess.peersMap.get(key);
							if(key.equals(peerProcess.peerId))continue;
							if (pref.isCompleted == 0 && pref.isChoked == 0 && pref.isHandShaked == 1) {
								peerProcess.print(peerProcess.peerId + " sending HAVE message to Peer " + key);
								byte[] encodedBitField = peerProcess.bit.getBytes();
								Data data = new Data("4", encodedBitField);
								try {
									OutputStream out = peerProcess.socketMap.get(key).getOutputStream();
									out.write(Data.encodeMessage(d));
								} catch (IOException e) {}
								encodedBitField = null;
								
								peerProcess.peersMap.get(key).state = 3;
							} 
						}
						buffer = null;
						d = null;
				 }
				 else if (msgType.equals("0")) {
						peerProcess.print(peerProcess.peerId + " is CHOKED by Peer " + rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 14;
				 }
				 break; 
			 case 14:
				 if (msgType.equals("4")) {
						if(isInterested(d,rPeerId)) {
							sendInterested(peerProcess.socketMap.get(rPeerId), rPeerId);
							peerProcess.peersMap.get(rPeerId).state = 9;
						}	
						else {
							peerProcess.print(peerProcess.peerId + " sending a NOT INTERESTED message to Peer " + rPeerId);
							Data data =  new Data("3");
							byte[] msgByte = Data.encodeMessage(d);
							try {
								OutputStream out = peerProcess.socketMap.get(rPeerId).getOutputStream();
								out.write(msgByte);
								} catch (IOException e) {}
							peerProcess.peersMap.get(rPeerId).state = 13;
						}
				 }
				 else if (msgType.equals("1")) {
						peerProcess.print(peerProcess.peerId + " is UNCHOKED by Peer " + rPeerId);
						peerProcess.peersMap.get(rPeerId).state = 14;
				 }
				 break; 
			 }
			}
		}
	}
	 
	private void sendRequest(Socket socket, int pieceNo, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending a REQUEST message to Peer " + remotePeerID);
		byte[] pieceByte = new byte[4];
		for (int i = 0; i < 4; i++)
			pieceByte[i] = 0;
		byte[] pieceIndexByte = Conversion.intToByteArray(pieceNo);
		System.arraycopy(pieceIndexByte, 0, pieceByte, 0, pieceIndexByte.length);
		Data d = new Data("6", pieceByte);
		byte[] b = Data.encodeMessage(d);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(b);
			} catch (IOException e) {}
		pieceByte = null;
		pieceIndexByte = null;
		b = null;
		d = null;
	}

	private void sendPeice(Socket socket, Data d, String remotePeerID)  {
		byte[] bytePieceIndex = d.content;
		int pieceIndex = Conversion.byteArrayToInt(bytePieceIndex, 0);
		peerProcess.print(peerProcess.peerId + " sending a PIECE message for piece " + pieceIndex + " to Peer " + remotePeerID);
		byte[] byteRead = new byte[peerProcess.pieceSize];
		int noBytesRead = 0;
		File file = new File(peerProcess.peerId,peerProcess.fileName);
		try {
			raf = new RandomAccessFile(file,"r");
			raf.seek(pieceIndex*peerProcess.pieceSize);
			noBytesRead = raf.read(byteRead, 0, peerProcess.pieceSize);
		} 
		catch (IOException e) {}
		byte[] buffer = new byte[noBytesRead + 4];
		System.arraycopy(bytePieceIndex, 0, buffer, 0, 4);
		System.arraycopy(byteRead, 0, buffer, 4, noBytesRead);
		Data sendMessage = new Data("7", buffer);
		byte[] b =  Data.encodeMessage(sendMessage);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(b);
			} catch (IOException e) {
			}
		buffer = null;
		byteRead = null;
		b = null;
		bytePieceIndex = null;
		sendMessage = null;
		try {
			raf.close();
		}
		catch(Exception e){}
	}
	
	private void sendNotInterested(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending a NOT INTERESTED message to Peer " + remotePeerID);
		Data d =  new Data("3");
		byte[] msgByte = Data.encodeMessage(d);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(msgByte);
			} catch (IOException e) {
			}
	}

	private void sendInterested(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending an INTERESTED message to Peer " + remotePeerID);
		Data d =  new Data("2");
		byte[] msgByte = Data.encodeMessage(d);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(msgByte);
			} catch (IOException e) {}
	}

	private boolean isInterested(Data d, String rPeerId) {		
		Conversion b = Conversion.convert(d.content);
		peerProcess.peersMap.get(rPeerId).bitField = b;		
		int yourSize = b.len;
		boolean flag = false;
		for (int i = 0; i < yourSize; i++) 	{
			if (b.arr[i].getIsPresent() == 1 && peerProcess.bit.arr[i].getIsPresent() == 0) 
				flag = true;
			else
				continue;
		}		
		return flag;
	}

	private void sendUnChoke(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending UNCHOKE message to Peer " + remotePeerID);
		Data d = new Data("1");
		byte[] msgByte = Data.encodeMessage(d);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(msgByte);
			} catch (IOException e) {}
	}

	private void sendBitField(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending BITFIELD message to Peer " + remotePeerID);
		byte[] encodedBitField = peerProcess.bit.getBytes();
		Data d = new Data("5", encodedBitField);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(Data.encodeMessage(d));
			} catch (IOException e) {}
		encodedBitField = null;
	}
	
}