
public class Converter 
{
	
	
    public static int byteArrayToInt(byte[] byt) {
        return byteArrayToInt(byt, 0);
    }

    

	public static byte[] intToByteArray(int val)
	{
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) 
        {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((val >>> offset) & 0xFF);
        }
        return b;
	}
	

	public static int byteArrayToInt(byte[] byt, int offset)
    {
        int val = 0;
        for (int i = 0; i < 4; i++)
        {
            int shift = (4 - 1 - i) * 8;
            val += (byt[i + offset] & 0x000000FF) << shift;
        }
        return val;
    }
    
    static String byteArrayToHexString(byte inp[]) 
	 {
	    byte hexB = 0x00;

	    int i = 0; 

	    if (inp == null || inp.length <= 0)
	        return null;
	    String hexDigits[] = {"0", "1", "2","3", "4", "5", "6", "7", "8","9", "A", "B", "C", "D", "E","F"};

	    StringBuffer out = new StringBuffer(inp.length * 2);

	    for (i = 0; i < inp.length; i++) 
	    {
	        hexB = (byte) (inp[i] & 0xF0); 
	        hexB = (byte) (hexB >>> 4);
	        // shift the bits down

	        hexB = (byte) (hexB & 0x0F);    
	        // must do this is high order bit is on!

	        out.append(hexDigits[ (int) hexB]);

	        hexB = (byte) (inp[i] & 0x0F); 

	        out.append(hexDigits[ (int) hexB]); 

	    }

	    String result = new String(out);

	    return result;
	}
    
}
