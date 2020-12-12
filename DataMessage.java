
import java.io.UnsupportedEncodingException;

public class DataMessage 
{
     String typeOfMsg;
     String size;
     int sized = 1;
     byte[] bytearrt = null;
	 byte[] bytearrl = null;
	 byte[] content = null;
	
	 public DataMessage()
	    {}
	 
    public DataMessage(String Type) {
        try {   
        	this.sized = 1;
        	this.size = ((Integer)1).toString();
        	this.bytearrl = Conversion.intToByteArray(1);
        	try {
    			this.typeOfMsg = Type.trim();
    			this.bytearrt = this.typeOfMsg.getBytes("UTF8");
    		} catch (UnsupportedEncodingException e) {}
            this.content = null;
        } 
        catch (Exception e) {}
    }

	public DataMessage(String Type, byte[] bytearr) {
		try {	
			if (bytearr != null) {
				this.sized = bytearr.length + 1;
		        this.size = ((Integer)(bytearr.length + 1)).toString();
		        this.bytearrl = Conversion.intToByteArray(bytearr.length + 1);
		        this.content = bytearr;
			} 
			else {
				this.sized = 1;
		        this.size = ((Integer)1).toString();
		        this.bytearrl = Conversion.intToByteArray(1);
                this.content = null;
            }
			try {
				this.typeOfMsg = Type.trim();
				this.bytearrt = this.typeOfMsg.getBytes("UTF8");
			} catch (UnsupportedEncodingException e) {}
		} catch (Exception e) {}
	}
    
    public static byte[] encodeMessage(DataMessage data) {
        byte[] bytearray = null;
        try { 
            int num = Integer.parseInt(data.typeOfMsg);
            if (data.content!= null) {
                bytearray = new byte[4 + 1 + data.content.length];
                System.arraycopy(data.bytearrl, 0, bytearray, 0, data.bytearrl.length);
                System.arraycopy(data.bytearrt, 0, bytearray, 4, 1);
                System.arraycopy(data.content, 0, bytearray, 4 + 1, data.content.length);   
            } else {
                bytearray = new byte[4 + 1];
                System.arraycopy(data.bytearrl, 0, bytearray, 0, data.bytearrl.length);
                System.arraycopy(data.bytearrt, 0, bytearray, 4, 1);
            }
        }
        catch (Exception e) {}
        return bytearray;
    }
	 
	public static DataMessage decodeMessage(byte[] Message) {	
		DataMessage msg = new DataMessage();
		byte[] msgLength = new byte[4];
		byte[] msgType = new byte[1];
		byte[] payLoad = null;
		int len;
		try {	
			System.arraycopy(Message, 0, msgLength, 0, 4);
			System.arraycopy(Message, 4, msgType, 0, 1);
			Integer l = Conversion.byteArrayToInt(msgLength, 0);
			msg.size = l.toString();
			msg.bytearrl = msgLength;
			msg.sized = l;  
			try {
				msg.typeOfMsg = new String(msgType, "UTF8");
				msg.bytearrt = msgType;
			} catch (UnsupportedEncodingException e) {}
			len = Conversion.byteArrayToInt(msgLength, 0);
			if (len > 1) {
				payLoad = new byte[len-1];
				System.arraycopy(Message, 4 + 1,	payLoad, 0, Message.length - 4 - 1);
				msg.content = payLoad;
			}
			payLoad = null;
		} 
		catch (Exception e) {
			msg = null;
		}
		return msg;
	}
}
