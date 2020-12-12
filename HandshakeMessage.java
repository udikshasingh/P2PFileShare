/**
 * Imports the packages 
 */
import java.io.*;


public class HandshakeMessage  
{
	// Attributes
	private byte[] header = new byte[18];
	private byte[] peerID = new byte[4];
	private byte[] zeroBits = new byte[10];
	private String messageHeader;
	private String messagePeerID;

	public HandshakeMessage(){
		
	}
	
	/* Class constructor
	 * 
	 * Header Handshake header string
	 * PeerId Peer ID
	 * 
	 */
	public HandshakeMessage(String Header, String PeerId) {

		try {
			this.messageHeader = Header;
			this.header = Header.getBytes("UTF8");
			if (this.header.length > 18)
				throw new Exception("Header is too large.");

			this.messagePeerID = PeerId;
			this.peerID = PeerId.getBytes("UTF8");
			if (this.peerID.length > 18)
				throw new Exception("Peer ID is too large.");

			this.zeroBits = "0000000000".getBytes("UTF8");
		} catch (Exception e) {
			peerProcess.print(e.toString());
		}

	}

	// Set the handShakeHeader
	public void setHeader(byte[] handShakeHeader) {
		try {
			this.messageHeader = (new String(handShakeHeader, "UTF8")).toString().trim();
			this.header = this.messageHeader.getBytes();
		} catch (UnsupportedEncodingException e) {
			peerProcess.print(e.toString());
		}
	}


	// Set the peerID 
	public void setPeerID(byte[] peerID) {
		try {
			this.messagePeerID = (new String(peerID, "UTF8")).toString().trim();
			this.peerID = this.messagePeerID.getBytes();

		} catch (UnsupportedEncodingException e) {
			peerProcess.print(e.toString());
		}
	}
	
	// Set the messagePeerID
	public void setPeerID(String messagePeerID) {
		try {
			this.messagePeerID = messagePeerID;
			this.peerID = messagePeerID.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			peerProcess.print(e.toString());
		}
	}

	// Set the messageHeader 
	public void setHeader(String messageHeader) {
		try {
			this.messageHeader = messageHeader;
			this.header = messageHeader.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			peerProcess.print(e.toString());
		}
	}
	
	// return the handShakeHeader
	public byte[] getHeader() {
		return header;
	}
	
	// return the peerID
	public byte[] getPeerID() {
		return peerID;
	}

	// Set the zeroBits 
	public void setZeroBits(byte[] zeroBits) {
		this.zeroBits = zeroBits;
	}

	// return the zeroBits
	public byte[] getZeroBits() {
		return zeroBits;
	}

	// return the messageHeader
	public String getHeaderString() {
		return messageHeader;
	}

	// return the messagePeerID
	public String getPeerIDString() {
		return messagePeerID;
	}

	// Return the toString method of the Object
	public String toString() {
		return ("[HandshakeMessage] : Peer Id - " + this.messagePeerID
				+ ", Header - " + this.messageHeader);
	}

	// Decodes the byte array HandshakeMessage and loads to the object HandshakeMessage
	public static HandshakeMessage decodeMessage(byte[] receivedMessage) {

		HandshakeMessage handshakeMessage = null;
		byte[] msgHeader = null;
		byte[] msgPeerID = null;

		try {
			// Initial check
			if (receivedMessage.length != 32)
				throw new Exception("Byte array length not matching.");

			// VAR initialization
			handshakeMessage = new HandshakeMessage();
			msgHeader = new byte[18];
			msgPeerID = new byte[4];

			// Decode the received message
			System.arraycopy(receivedMessage, 0, msgHeader, 0,
					18);
			System.arraycopy(receivedMessage, 18
					+ 10, msgPeerID, 0,
					4);

			// Populate handshakeMessage entity
			handshakeMessage.setHeader(msgHeader);
			handshakeMessage.setPeerID(msgPeerID);

		} catch (Exception e) {
			peerProcess.print(e.toString());
			handshakeMessage = null;
		}
		return handshakeMessage;
	}

	// Encodes a given message in the format HandshakeMessage
	public static byte[] encodeMessage(HandshakeMessage handshakeMessage) {

		byte[] sendMessage = new byte[32];

		try {
			// Encode header
			if (handshakeMessage.getHeader() == null) {
				throw new Exception("Invalid Header.");
			}
			if (handshakeMessage.getHeader().length > 18|| handshakeMessage.getHeader().length == 0)
			{
				throw new Exception("Invalid Header.");
			} else {
				System.arraycopy(handshakeMessage.getHeader(), 0, sendMessage,
						0, handshakeMessage.getHeader().length);
			}

			// Encode zero bits
			if (handshakeMessage.getZeroBits() == null) {
				throw new Exception("Invalid zero bits field.");
			} 
			if (handshakeMessage.getZeroBits().length > 10
					|| handshakeMessage.getZeroBits().length == 0) {
				throw new Exception("Invalid zero bits field.");
			} else {
				System.arraycopy(handshakeMessage.getZeroBits(), 0,
						sendMessage, 18,
						10 - 1);
			}

			// Encode peer id
			if (handshakeMessage.getPeerID() == null) 
			{
				throw new Exception("Invalid peer id.");
			} 
			else if (handshakeMessage.getPeerID().length > 4
					|| handshakeMessage.getPeerID().length == 0) 
			{
				throw new Exception("Invalid peer id.");
			} 
			else 
			{
				System.arraycopy(handshakeMessage.getPeerID(), 0, sendMessage,
						18 + 10,
						handshakeMessage.getPeerID().length);
			}

		} 
		catch (Exception e) 
		{
			peerProcess.print(e.toString());
			sendMessage = null;
		}

		return sendMessage;
	}
}
