import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;


public class ConnectionManager implements Runnable 
{
	 static boolean up = true;
	 static String peerid = null;
	 static int condition = -1;
	RandomAccessFile newfile;
	
	public ConnectionManager(String peerID) {
		peerid = peerID;
	}
	
	public ConnectionManager() {
		peerid = null;
	}

	public void run() {
		Data payload;
		Source source;
		String str1;
		String destinationpid;		
		while(up) {
			Source s1 = null;
			if(!peerProcess.queue.isEmpty()) {
				s1 = peerProcess.queue.remove();
			}
			source = s1;
			while(source == null) {
				Thread.currentThread();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				Source s2 = null;
				if(!peerProcess.queue.isEmpty())
					s2 = peerProcess.queue.remove();
				source = s2;
			}
			payload = source.data;
			str1 = payload.typeOfMsg;
			destinationpid = source.source;
			int state = peerProcess.peersMap.get(destinationpid).state;
			if(str1.equals("4") && state != 14) {
				peerProcess.print(peerProcess.peerId + " receieved HAVE message from Peer " + destinationpid); 
				if(findInterest(payload, destinationpid)) {
					interested(peerProcess.socketMap.get(destinationpid), destinationpid);
					peerProcess.peersMap.get(destinationpid).state = 9;
				}	
				else {
					notInterested(peerProcess.socketMap.get(destinationpid), destinationpid);
					peerProcess.peersMap.get(destinationpid).state = 13;
				}
			}
			else {
			 switch (state) { 
			 case 2:
			   if (str1.equals("5")) {
		 		  peerProcess.print(peerProcess.peerId + " receieved a BITFIELD message from Peer " + destinationpid);
	 			  bit(peerProcess.socketMap.get(destinationpid), destinationpid);
 				  peerProcess.peersMap.get(destinationpid).state = 3;  
			   }
			   break;
			 case 3:
			   if (str1.equals("3")) {
					peerProcess.print(peerProcess.peerId + " receieved a NOT INTERESTED message from Peer " + destinationpid);
					peerProcess.peersMap.get(destinationpid).isInterested = 0;
					peerProcess.peersMap.get(destinationpid).state = 5;
					peerProcess.peersMap.get(destinationpid).isHandShaked = 1;
			   }
			   else if (str1.equals("2")) {
					peerProcess.print(peerProcess.peerId + " receieved an INTERESTED message from Peer " + destinationpid);
					peerProcess.peersMap.get(destinationpid).isInterested = 1;
					peerProcess.peersMap.get(destinationpid).isHandShaked = 1;
					if(!peerProcess.prefMap.containsKey(destinationpid) && !peerProcess.unchokedNeighbors.containsKey(destinationpid)) {
						
						peerProcess.print(peerProcess.peerId + " sending CHOKE message to Peer " + destinationpid);
						Data data = new Data("0");
						byte[] msgByte = Data.encodeMessage(payload);
						try {
							OutputStream out = (peerProcess.socketMap.get(destinationpid)).getOutputStream();
							out.write(msgByte);
						} catch (IOException e) {}
						
						peerProcess.peersMap.get(destinationpid).isChoked = 1;
						peerProcess.peersMap.get(destinationpid).state  = 6;
					}
					else {
						peerProcess.peersMap.get(destinationpid).isChoked = 0;
						unchoke(peerProcess.socketMap.get(destinationpid), destinationpid);
						peerProcess.peersMap.get(destinationpid).state = 4 ;
					}
			   }
			   break;
			 case 4:
				 if (str1.equals("6")) {
						piece(peerProcess.socketMap.get(destinationpid), payload, destinationpid);
						if(!peerProcess.prefMap.containsKey(destinationpid) && !peerProcess.unchokedNeighbors.containsKey(destinationpid)) {
							
							peerProcess.print(peerProcess.peerId + " sending CHOKE message to Peer " + destinationpid);
							Data data = new Data("0");
							byte[] msgByte = Data.encodeMessage(payload);
							try {
								OutputStream out = (peerProcess.socketMap.get(destinationpid)).getOutputStream();
								out.write(msgByte);
								} catch (IOException e) {}
							
							peerProcess.peersMap.get(destinationpid).isChoked = 1;
							peerProcess.peersMap.get(destinationpid).state = 6;
						}  
				 }
				 break;
			 case 8:
				 if (str1.equals("5")) {
						if(findInterest(payload,destinationpid)) {
							interested(peerProcess.socketMap.get(destinationpid), destinationpid);
							peerProcess.peersMap.get(destinationpid).state = 9;
						}	
						else {
							notInterested(peerProcess.socketMap.get(destinationpid), destinationpid);
							peerProcess.peersMap.get(destinationpid).state = 13;
						}
				 }
				 break;
			 case 9:
				 if (str1.equals("0")) {
						peerProcess.print(peerProcess.peerId + " is CHOKED by Peer " + destinationpid);
						peerProcess.peersMap.get(destinationpid).state = 14;
				 }
				 else if (str1.equals("1")) {
						peerProcess.print(peerProcess.peerId + " is UNCHOKED by Peer " + destinationpid);
						int firstdiff = peerProcess.bit.returnFirstDiff(peerProcess.peersMap.get(destinationpid).bitField);
						if(firstdiff != -1) {
							request(peerProcess.socketMap.get(destinationpid), firstdiff, destinationpid);
							peerProcess.peersMap.get(destinationpid).state = 11;
							peerProcess.peersMap.get(destinationpid).startTime = new Date();
						}
						else
							peerProcess.peersMap.get(destinationpid).state = 13;
				 }
				 break;
			 case 11:
				 if (str1.equals("7")) {
					    byte[] bytearr = payload.content;						
						peerProcess.peersMap.get(destinationpid).finishTime = new Date();
						long diff = peerProcess.peersMap.get(destinationpid).finishTime.getTime() - peerProcess.peersMap.get(destinationpid).startTime.getTime() ;
						peerProcess.peersMap.get(destinationpid).dataRate = ((double)(bytearr.length + 4 + 1)/(double)diff) * 100;
						Piece piece = Piece.decodePiece(bytearr);
						peerProcess.bit.updateBitField(destinationpid, piece);			
						int n = peerProcess.bit.returnFirstDiff(peerProcess.peersMap.get(destinationpid).bitField);
						if(n != -1) {
							request(peerProcess.socketMap.get(destinationpid),n, destinationpid);
							peerProcess.peersMap.get(destinationpid).state  = 11;
							peerProcess.peersMap.get(destinationpid).startTime = new Date();
						}
						else
							peerProcess.peersMap.get(destinationpid).state = 13;
						try  {
							String str2;
							BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));
							while ((str2 = br.readLine()) != null) {
								String[] arr = str2.trim().split("\\s+");
								String pId = arr[0];
								int flag = Integer.parseInt(arr[3]);
								if(flag == 1) {
									peerProcess.peersMap.get(pId).isCompleted = 1;
									peerProcess.peersMap.get(pId).isInterested = 0;
									peerProcess.peersMap.get(pId).isChoked = 0;
								}
							}
							br.close();
						}
						catch (Exception e) {}
						for (String line : peerProcess.peersMap.keySet()) {
							RemotePeerInfo rmi = peerProcess.peersMap.get(line);
							if(line.equals(peerProcess.peerId))continue;
							if (rmi.isCompleted == 0 && rmi.isChoked == 0 && rmi.isHandShaked == 1) {
								peerProcess.print(peerProcess.peerId + " sending HAVE message to Peer " + line);
								byte[] barr = peerProcess.bit.getBytes();
								Data data = new Data("4", barr);
								try {
									OutputStream os = peerProcess.socketMap.get(line).getOutputStream();
									os.write(Data.encodeMessage(payload));
								} catch (IOException e) {}
								barr = null;
								peerProcess.peersMap.get(line).state = 3;
							} 
						}
						bytearr = null;
						payload = null;
				 }
				 else if (str1.equals("0")) {
						peerProcess.print(peerProcess.peerId + " is CHOKED by Peer " + destinationpid);
						peerProcess.peersMap.get(destinationpid).state = 14;
				 }
				 break; 
			 case 14:
				 if (str1.equals("4")) {
						if(findInterest(payload,destinationpid)) {
							interested(peerProcess.socketMap.get(destinationpid), destinationpid);
							peerProcess.peersMap.get(destinationpid).state = 9;
						}	
						else {
							peerProcess.print(peerProcess.peerId + " sending a NOT INTERESTED message to Peer " + destinationpid);
							Data data =  new Data("3");
							byte[] msgByte = Data.encodeMessage(payload);
							try {
								OutputStream out = peerProcess.socketMap.get(destinationpid).getOutputStream();
								out.write(msgByte);
								} catch (IOException e) {}
							peerProcess.peersMap.get(destinationpid).state = 13;
						}
				 }
				 else if (str1.equals("1")) {
						peerProcess.print(peerProcess.peerId + " is UNCHOKED by Peer " + destinationpid);
						peerProcess.peersMap.get(destinationpid).state = 14;
				 }
				 break; 
			 }
			}
		}
	}
	 
	private void request(Socket socket, int pieceNo, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending a REQUEST message to Peer " + remotePeerID);
		byte[] barray = new byte[4];
		for (int i = 0; i < 4; i++)
			barray[i] = 0;
		byte[] barr2 = Conversion.intToByteArray(pieceNo);
		System.arraycopy(barr2, 0, barray, 0, barr2.length);
		Data data = new Data("6", barray);
		byte[] b = Data.encodeMessage(data);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(b);
			} catch (IOException e) {}
		barray = null;
		barr2 = null;
		b = null;
		data = null;
	}

	private void piece(Socket socket, Data d, String remotePeerID)  {
		byte[] barr1 = d.content;
		int n1 = Conversion.byteArrayToInt(barr1, 0);
		peerProcess.print(peerProcess.peerId + " sending a PIECE message for piece " + n1 + " to Peer " + remotePeerID);
		byte[] barr2 = new byte[peerProcess.pieceSize];
		int n2 = 0;
		File f1 = new File(peerProcess.peerId,peerProcess.fileName);
		try {
			newfile = new RandomAccessFile(f1,"r");
			newfile.seek(n1*peerProcess.pieceSize);
			n2 = newfile.read(barr2, 0, peerProcess.pieceSize);
		} 
		catch (IOException e) {}
		byte[] barr3 = new byte[n2 + 4];
		System.arraycopy(barr1, 0, barr3, 0, 4);
		System.arraycopy(barr2, 0, barr3, 4, n2);
		Data d1 = new Data("7", barr3);
		byte[] barr4 =  Data.encodeMessage(d1);
		try {
			OutputStream out = socket.getOutputStream();
			out.write(barr4);
			} catch (IOException e) {
			}
		barr3 = null;
		barr2 = null;
		barr4 = null;
		barr1 = null;
		d1 = null;
		try {
			newfile.close();
		}
		catch(Exception e){}
	}
	
	private void notInterested(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending a NOT INTERESTED message to Peer " + remotePeerID);
		Data data =  new Data("3");
		byte[] barr1 = Data.encodeMessage(data);
		try {
			OutputStream os = socket.getOutputStream();
			os.write(barr1);
			} catch (IOException e) {}
	}

	private void interested(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending an INTERESTED message to Peer " + remotePeerID);
		Data data =  new Data("2");
		byte[] barr1 = Data.encodeMessage(data);
		try {
			OutputStream os = socket.getOutputStream();
			os.write(barr1);
			} catch (IOException e) {}
	}

	private boolean findInterest(Data d, String rPeerId) {		
		Conversion conversion = Conversion.convert(d.content);
		peerProcess.peersMap.get(rPeerId).bitField = conversion;		
		int n1 = conversion.len;
		boolean flag = false;
		for (int i = 0; i < n1; i++) 	{
			if (conversion.arr[i].getIsPresent() == 1 && peerProcess.bit.arr[i].getIsPresent() == 0) 
				flag = true;
			else
				continue;
		}		
		return flag;
	}

	private void unchoke(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending UNCHOKE message to Peer " + remotePeerID);
		Data data = new Data("1");
		byte[] barr1 = Data.encodeMessage(data);
		try {
			OutputStream os = socket.getOutputStream();
			os.write(barr1);
			} catch (IOException e) {}
	}

	private void bit(Socket socket, String remotePeerID) {
		peerProcess.print(peerProcess.peerId + " sending BITFIELD message to Peer " + remotePeerID);
		byte[] barr1 = peerProcess.bit.getBytes();
		Data data = new Data("5", barr1);
		try {
			OutputStream os = socket.getOutputStream();
			os.write(Data.encodeMessage(data));
			} catch (IOException e) {}
		barr1 = null;
	}
	
}