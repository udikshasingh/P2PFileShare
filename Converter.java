
public class Converter 
{
	
	
	public static int byteArrToInt(byte[] b) 
	{
        return byteArrToInt(b, 0);
    }

    
    public static int byteArrToInt(byte[] b, int ofs)
    {
        int val = 0;
        for (int i = 0; i < 4; i++)
        {
            int shift = (4 - 1 - i) * 8;
            val += (b[i + ofs] & 0x000000FF) << shift;
        }
        return val;
    }

	public static byte[] intToByteArr(int val)
	{
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) 
        {
            int ofs = (b.length - 1 - i) * 8;
            b[i] = (byte) ((val >>> ofs) & 0xFF);
        }
        return b;
    }

	static String byteArrayToHexString(byte in[]) 
	{
	    byte byt = 0x00;

	    int i = 0; 

	    if (in == null || in.length <= 0)
	        return null;
	    String hexDigits[] = {"0", "1", "2","3", "4", "5", "6", "7", "8","9", "A", "B", "C", "D", "E","F"};

	    StringBuffer out = new StringBuffer(in.length * 2);

	    for (i = 0; i < in.length; i++) 
	    {
	        byt = (byte) (in[i] & 0xF0); 
	        byt = (byte) (byt >>> 4);

	        byt = (byte) (byt & 0x0F);    
	        out.append(hexDigits[ (int) byt]);

	        byt = (byte) (in[i] & 0x0F); 

	        out.append(hexDigits[ (int) byt]); 

	    }

	    String rslt = new String(out);

	    return rslt;
	}

}
