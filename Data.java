
import java.io.UnsupportedEncodingException;

public class Data 
{
     String typeOfMsg;
     String size;
     int sized = 1;
     byte[] bytearrt = null;
	 byte[] bytearrl = null;
	 byte[] content = null;
	
	 public Data()
	    {}
	 
    public Data(String str) {
        try {   
        	this.sized = 1;
        	this.size = ((Integer)1).toString();
        	this.bytearrl = Conversion.intToByteArray(1);
        	try {
    			this.typeOfMsg = str.trim();
    			this.bytearrt = this.typeOfMsg.getBytes("UTF8");
    		} catch (UnsupportedEncodingException e) {}
            this.content = null;
        } 
        catch (Exception e) {}
    }

	public Data(String str, byte[] bytearr) {
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
				this.typeOfMsg = str.trim();
				this.bytearrt = this.typeOfMsg.getBytes("UTF8");
			} catch (UnsupportedEncodingException e) {}
		} catch (Exception e) {}
	}
    
    public static byte[] encodeMessage(Data datamsg) {
        byte[] bytearray = null;
        try { 
            int num = Integer.parseInt(datamsg.typeOfMsg);
            if (datamsg.content!= null) {
                bytearray = new byte[4 + 1 + datamsg.content.length];
                System.arraycopy(datamsg.bytearrl, 0, bytearray, 0, datamsg.bytearrl.length);
                System.arraycopy(datamsg.bytearrt, 0, bytearray, 4, 1);
                System.arraycopy(datamsg.content, 0, bytearray, 4 + 1, datamsg.content.length);   
            } else {
                bytearray = new byte[4 + 1];
                System.arraycopy(datamsg.bytearrl, 0, bytearray, 0, datamsg.bytearrl.length);
                System.arraycopy(datamsg.bytearrt, 0, bytearray, 4, 1);
            }
        }
        catch (Exception e) {}
        return bytearray;
    }
	 
	public static Data decodeMessage(byte[] bytearray) {	
		Data obj = new Data();
		byte[] arr1 = new byte[4];
		byte[] arr2 = new byte[1];
		byte[] arr3 = null;
		try {	
			int len;
			System.arraycopy(bytearray, 0, arr1, 0, 4);
			System.arraycopy(bytearray, 4, arr2, 0, 1);
			Integer l = Conversion.byteArrayToInt(arr1, 0);
			obj.size = l.toString();
			obj.bytearrl = arr1;
			obj.sized = l;  
			try {
				obj.typeOfMsg = new String(arr2, "UTF8");
				obj.bytearrt = arr2;
			} catch (UnsupportedEncodingException e) {}
			len = Conversion.byteArrayToInt(arr1, 0);
			if (len > 1) {
				arr3 = new byte[len-1];
				System.arraycopy(bytearray, 4 + 1,	arr3, 0, bytearray.length - 4 - 1);
				obj.content = arr3;
			}
			arr3 = null;
		} 
		catch (Exception e) {
			obj = null;
		}
		return obj;
	}
}
