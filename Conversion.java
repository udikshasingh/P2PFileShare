import java.io.*;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.RandomAccessFile;


public class Conversion 
{
	public Piece[] arr;
	public int len;

	public Conversion() {
		len = (int) Math.ceil(((double) peerProcess.fileSize / (double) peerProcess.pieceSize));
		this.arr = new Piece[len];

		for (int i = 0; i < this.len; i++)
			this.arr[i] = new Piece();

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

	public static Conversion convert(byte[] bytearr) {
		Conversion byteobj = new Conversion();
		for(int i = 0 ; i < bytearr.length; i ++) {
			int n = 7;
			while(n >= 0) {
				int bit = 1 << n;
				if(i * 8 + (8-n-1) < byteobj.len) {
					if((bytearr[i] & (bit)) != 0)
						byteobj.arr[i * 8 + (8-n-1)].isPresent = 1;
					else
						byteobj.arr[i * 8 + (8-n-1)].isPresent = 0;
				}
				n--;
			}
		}
		return byteobj;
	}

	public synchronized int returnFirstDiff(Conversion bits) {
		int len1 = this.len;
		int len2 = bits.len;
		if (len1 >= len2) {
			for (int i = 0; i < len2; i++) {
				if (bits.arr[i].getIsPresent() == 1 && this.arr[i].getIsPresent() == 0)
					return i;	
			}
		} 
		else {
			for (int i = 0; i < len1; i++) {
				if (bits.arr[i].getIsPresent() == 1 && this.arr[i].getIsPresent() == 0) 
					return i;
			}
		}
		return -1;
	}

	public byte[] getBytes() {
		int segment = this.len / 8;
		if (len % 8 != 0)
			segment = segment + 1;
		byte[] bytearr = new byte[segment];
		int n1 = 0;
		int n2 = 0;
		int n3;
		for (n3 = 1; n3 <= this.len; n3++) {
			int present = this.arr[n3-1].isPresent;
			n1 = n1 << 1;
			if (present == 1) 
				n1 = n1 + 1;
			else
				n1 = n1 + 0;
			if (n3 % 8 == 0 && n3!=0) {
				bytearr[n2] = (byte) n1;
				n2++;
				n1 = 0;
			}	
		}
		if ((n3-1) % 8 != 0) {
			int bits = ((len) - (len / 8) * 8);
			n1 = n1 << (8 - bits);
			bytearr[n2] = (byte) n1;
		}
		return bytearr;
	}
	 
	public synchronized void updateBitField(String pid, Piece pc) {
		try {
			if (peerProcess.bit.arr[pc.pieceIndex].isPresent == 1) 
				peerProcess.print(pid + " Piece already received..!");
			else {
				String str = peerProcess.fileName;
				File str2 = new File(peerProcess.peerId, str);
				int num = pc.pieceIndex * peerProcess.pieceSize;
				RandomAccessFile file = new RandomAccessFile(str2, "rw");
				byte[] bytearr = pc.filePiece;
				file.seek(num);
				file.write(bytearr);
				this.arr[pc.pieceIndex].setIsPresent(1);
				this.arr[pc.pieceIndex].setFromPeerID(pid);
				file.close();
				int num2 = 0;
		        for (int i = 0; i < this.len; i++) {
		        	 if (this.arr[i].isPresent == 1)
			                num2++;
		        }				
				peerProcess.print(peerProcess.peerId + " has downloaded the PIECE " + pc.pieceIndex	+ " from Peer " + pid + ". Number of pieces left =  " + num2);
				boolean flag = true;
				for (int i = 0; i < this.len; i++) {
						if (this.arr[i].isPresent == 0) 
			                flag = false;
			    }
				if (flag) {
					peerProcess.peersMap.get(peerProcess.peerId).isInterested = 0;
					peerProcess.peersMap.get(peerProcess.peerId).isCompleted = 1;
					peerProcess.peersMap.get(peerProcess.peerId).isChoked = 0;
					try {
						BufferedReader br = new BufferedReader(new FileReader("PeerInfo.cfg"));
						String string;
						StringBuffer sbr = new StringBuffer();
						while((string = br.readLine()) != null) {
							if(string.trim().split("\\s+")[0].equals(peerProcess.peerId))
								sbr.append(string.trim().split("\\s+")[0] + " " + string.trim().split("\\s+")[1] + " " + string.trim().split("\\s+")[2] + " " + 1);
							else
								sbr.append(string);
							sbr.append("\n");
						}
						br.close();
						BufferedWriter bw= new BufferedWriter(new FileWriter("PeerInfo.cfg"));
						bw.write(sbr.toString());	
						bw.close();
					} 
					catch (Exception e) {}
					peerProcess.print(peerProcess.peerId + " has DOWNLOADED the complete file.");
				}
			}
		} 
		catch (Exception e) {}
	}

}