
import java.io.UnsupportedEncodingException;

public class DataMessage 
{
    private String messageType;
    private String messageLength;
    private int dataLength = 1;
    private byte[] type = null;
	private byte[] len = null;
	private byte[] payload = null;
	
    public DataMessage(String Type) {
        
        try {
            
            if (Type == "0" || Type == "1"
                || Type == "2"
                || Type == "3")
            {
                this.setMessageLength(1);
                this.setMessageType(Type);
                this.payload = null;
            }
            else
                throw new Exception("DataMessage:: Constructor - Wrong constructor selection.");
            
            
        } catch (Exception e) {
            peerProcess.print(e.toString());
        }
        
    }


	
	public DataMessage(String Type, byte[] Payload) 
	{

		try 
		{
			if (Payload != null)
			{
				
                this.setMessageLength(Payload.length + 1);
                if (this.len.length > 4)
                    throw new Exception("DataMessage:: Constructor - message length is too large.");
                
                this.setPayload(Payload);
                
			} 
			else
			{
                if (Type == "0" || Type == "1"
                    || Type == "2"
                    || Type == "3")
                {
                    this.setMessageLength(1);
                    this.payload = null;
                }
                else
                    throw new Exception("DataMessage:: Constructor - Pay load should not be null");

				
			}

			this.setMessageType(Type);
			if (this.getMessageType().length > 1)
				throw new Exception("DataMessage:: Constructor - Type length is too large.");

		} catch (Exception e) {
			peerProcess.print(e.toString());
		}

	}

    public DataMessage()
    {
        
    }
	
    public void setMessageLength(int messageLength) {
        this.dataLength = messageLength;
        this.messageLength = ((Integer)messageLength).toString();
        this.len = Conversion.intToByteArray(messageLength);
    }
	
	public void setMessageLength(byte[] len) {

		Integer l = Conversion.byteArrayToInt(len, 0);
		this.messageLength = l.toString();
		this.len = len;
		this.dataLength = l;  
	}

	
	
	
	public byte[] getMessageLength() {
		return len;
	}
	
	public String getMessageLengthString() {
		return messageLength;
	}

	
	public int getMessageLengthInt() {
		return this.dataLength;
	}
	
	

	public void setMessageType(byte[] type) {
		try {
			this.messageType = new String(type, "UTF8");
			this.type = type;
		} catch (UnsupportedEncodingException e) {
			peerProcess.print(e.toString());
		}
	}
	
	public void setMessageType(String messageType) {
		try {
			this.messageType = messageType.trim();
			this.type = this.messageType.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			peerProcess.print(e.toString());
		}
	}
	
	public byte[] getMessageType() {
		return type;
	}

	
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	
	public byte[] getPayload() {
		return payload;
	}


	public String getMessageTypeString() {
		return messageType;
	}

	public String toString() {
		String str = null;
		try {
			str = "[DataMessage] : Message Length - "
					+ this.messageLength
					+ ", Message Type - "
					+ this.messageType
					+ ", Data - "
					+ (new String(this.payload, "UTF8")).toString()
							.trim();
		} catch (UnsupportedEncodingException e) {
			peerProcess.print(e.toString());
		}
		return str;
	}
    //encodes the object DataMessage to a byte array
    
    public static byte[] encodeMessage(DataMessage msg)
    {
        byte[] msgStream = null;
        int msgType;
        
        try
        {
            
            msgType =Integer.parseInt(msg.getMessageTypeString());
            if (msg.getMessageLength().length > 4)
                throw new Exception("Invalid message length.");
            else if (msgType < 0 || msgType > 7)
                throw new Exception("Invalid message type.");
            else if (msg.getMessageType() == null)
                throw new Exception("Invalid message type.");
            else if (msg.getMessageLength() == null)
                throw new Exception("Invalid message length.");
            
            if (msg.getPayload()!= null) {
                msgStream = new byte[4 + 1 + msg.getPayload().length];
                
                System.arraycopy(msg.getMessageLength(), 0, msgStream, 0, msg.getMessageLength().length);
                System.arraycopy(msg.getMessageType(), 0, msgStream, 4, 1);
                System.arraycopy(msg.getPayload(), 0, msgStream, 4 + 1, msg.getPayload().length);
                
                
            } else {
                msgStream = new byte[4 + 1];
                
                System.arraycopy(msg.getMessageLength(), 0, msgStream, 0, msg.getMessageLength().length);
                System.arraycopy(msg.getMessageType(), 0, msgStream, 4, 1);
                
            }
            
        }
        catch (Exception e)
        {
            peerProcess.print(e.toString());
            msgStream = null;
        }
        
        return msgStream;
    }

	
	 //decodes the byte array and send it to object DataMessage
	 
	public static DataMessage decodeMessage(byte[] Message) {

		
		DataMessage msg = new DataMessage();
		byte[] msgLength = new byte[4];
		byte[] msgType = new byte[1];
		byte[] payLoad = null;
		int len;

		try 
		{
			
			if (Message == null)
				throw new Exception("Invalid data.");
			else if (Message.length < 4 + 1)
				throw new Exception("Byte array length is too small...");

			
			System.arraycopy(Message, 0, msgLength, 0, 4);
			System.arraycopy(Message, 4, msgType, 0, 1);

			msg.setMessageLength(msgLength);
			msg.setMessageType(msgType);
			
			len = Conversion.byteArrayToInt(msgLength, 0);
			
			if (len > 1) 
			{
				payLoad = new byte[len-1];
				System.arraycopy(Message, 4 + 1,	payLoad, 0, Message.length - 4 - 1);
				msg.setPayload(payLoad);
			}
			
			payLoad = null;
		} 
		catch (Exception e) 
		{
			peerProcess.print(e.toString());
			msg = null;
		}
		return msg;
	}

	

}
