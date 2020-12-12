import java.io.*;


public class HandshakeMessage  
{
	 public byte[] bytearray = new byte[18];
	 public byte[] bytearray_id = new byte[4];
	 public byte[] bits = new byte[10];
	 public String header;
	 public String peerId;

	public HandshakeMessage(){}
	
	public HandshakeMessage(String str, String pid) {
		try {
			this.header = str;
			this.bytearray = str.getBytes("UTF8");
			if (this.bytearray.length > 18)
				throw new Exception("Header is too large.");

			this.peerId = pid;
			this.bytearray_id = pid.getBytes("UTF8");
			this.bits = "0000000000".getBytes("UTF8");
		} catch (Exception e) {
			peerProcess.print(e.toString());
		}

	}

	public static HandshakeMessage convertToMessage(byte[] bytearr) {
		HandshakeMessage hsmsg = null;
		try {
			if (bytearr.length != 32)
				throw new Exception("Byte array length not matching.");
			hsmsg = new HandshakeMessage();
			byte[] arr1 = new byte[18];
			byte[] arr2 = new byte[4];
			System.arraycopy(bytearr, 0, arr1, 0, 18);
			System.arraycopy(bytearr, 18 + 10, arr2, 0, 4);
			try {
				hsmsg.header = (new String(arr1, "UTF8")).toString().trim();
				hsmsg.bytearray = hsmsg.header.getBytes();
			} catch (UnsupportedEncodingException e) {}
			try {
				hsmsg.peerId = (new String(arr2, "UTF8")).toString().trim();
				hsmsg.bytearray_id = hsmsg.peerId.getBytes();
	
			} catch (UnsupportedEncodingException e) {}
		} catch (Exception e) {
			hsmsg = null;
		}
		return hsmsg;
	}

	public static byte[] convertToByteArray(HandshakeMessage handshakeMessage) {
		byte[] bytearr = new byte[32];
		try {
			System.arraycopy(handshakeMessage.bytearray, 0, bytearr, 0, handshakeMessage.bytearray.length);
			System.arraycopy(handshakeMessage.bits, 0, bytearr, 18, 10 - 1);
			System.arraycopy(handshakeMessage.bytearray_id, 0, bytearr, 18 + 10, handshakeMessage.bytearray_id.length);
		} 
		catch (Exception e) {
			bytearr = null;
		}
		return bytearr;
	}
}
