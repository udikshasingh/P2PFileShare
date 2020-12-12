import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;


public class BitOperator implements MessageConstants 
{
	public Piece[] pieces;
	public int size;

	public BitOperator() 
	{
		size = (int) Math.ceil(((double) Configurations.fileSize / (double) Configurations.pieceSize));
		this.pieces = new Piece[size];

		for (int i = 0; i < this.size; i++)
			this.pieces[i] = new Piece();

	}
	
	public void setSize(int size) 
	{
		this.size = size;
	}

	public int getSize() 
	{
		return size;
	}
	
	public void setPieces(Piece[] pieces) 
	{
		this.pieces = pieces;
	}

	public Piece[] getPieces() 
	{
		return pieces;
	}
	
	public byte[] encode()
	{
		return this.getBytes();
	}
	
	public static BitOperator decode(byte[] b)
	{
		BitOperator returnBitField = new BitOperator();
		for(int i = 0 ; i < b.length; i ++)
		{
			int cnt = 7;
			while(cnt >=0)
			{
				int tst = 1 << cnt;
				if(i * 8 + (8-cnt-1) < returnBitField.size)
				{
					if((b[i] & (tst)) != 0)
						returnBitField.pieces[i * 8 + (8-cnt-1)].isPresent = 1;
					else
						returnBitField.pieces[i * 8 + (8-cnt-1)].isPresent = 0;
				}
				cnt--;
			}
		}
		
		return returnBitField;
	}
	
	
	public synchronized boolean compare(BitOperator yourBitField) 
	{
		int yourSize = yourBitField.getSize();
		

		for (int i = 0; i < yourSize; i++) 
		{
			if (yourBitField.getPieces()[i].getIsPresent() == 1
					&& this.getPieces()[i].getIsPresent() == 0) 
			{
				return true;
			} else
				continue;
		}

		return false;
	}

	public synchronized int returnFirstDiff(BitOperator yourBitField) 
	{
		int mySize = this.getSize();
		int yourSize = yourBitField.getSize();

		if (mySize >= yourSize) 
		{
			for (int i = 0; i < yourSize; i++) 
			{
				if (yourBitField.getPieces()[i].getIsPresent() == 1
						&& this.getPieces()[i].getIsPresent() == 0) 
				{
					return i;
				}
			}
		} 
		else 
		{
			for (int i = 0; i < mySize; i++) 
			{
				if (yourBitField.getPieces()[i].getIsPresent() == 1
						&& this.getPieces()[i].getIsPresent() == 0) 
				{
					return i;
				}
			}
		}
		
		return -1;
	}

	public byte[] getBytes() 
	{
		int s = this.size / 8;
		if (size % 8 != 0)
			s = s + 1;
		byte[] iP = new byte[s];
		int tempI = 0;
		int cnt = 0;
		int Cnt;
		for (Cnt = 1; Cnt <= this.size; Cnt++)
		{
			int tempP = this.pieces[Cnt-1].isPresent;
			tempI = tempI << 1;
			if (tempP == 1) 
			{
				tempI = tempI + 1;
			} else
				tempI = tempI + 0;

			if (Cnt % 8 == 0 && Cnt!=0) 
			{
				iP[cnt] = (byte) tempI;
				cnt++;
				tempI = 0;
			}
			
		}
		if ((Cnt-1) % 8 != 0) 
		{
			int tempShift = ((size) - (size / 8) * 8);
			tempI = tempI << (8 - tempShift);
			iP[cnt] = (byte) tempI;
		}
		return iP;
	}

	
	 static String byteArrayToHexString(byte in[]) 
	 {
	    byte ch = 0x00;

	    int i = 0; 

	    if (in == null || in.length <= 0)
	        return null;
	    String pseudo[] = {"0", "1", "2","3", "4", "5", "6", "7", "8","9", "A", "B", "C", "D", "E","F"};

	    StringBuffer out = new StringBuffer(in.length * 2);

	    while (i < in.length) 
	    {
	        ch = (byte) (in[i] & 0xF0); 
	        ch = (byte) (ch >>> 4);
	        // shift the bits down

	        ch = (byte) (ch & 0x0F);    
	        // must do this is high order bit is on!

	        out.append(pseudo[ (int) ch]);

	        ch = (byte) (in[i] & 0x0F); 

	        out.append(pseudo[ (int) ch]); 
	        i++;
	    }

	    String result = new String(out);

	    return result;
	}
    
	
	public void initOwnBitfield(String OwnPeerId, int hasFile) 
	{

		if (hasFile != 1) 
		{

			// If the file not exists
			for (int i = 0; i < this.size; i++) 
			{
				this.pieces[i].setIsPresent(0);
				this.pieces[i].setFromPeerID(OwnPeerId);
			}

		} 
		else 
		{

			// If the file exists
			for (int i = 0; i < this.size; i++) 
			{
				this.pieces[i].setIsPresent(1);
				this.pieces[i].setFromPeerID(OwnPeerId);
			}

		}

	}

	// Update the bit field class and piece information
	 
	public synchronized void updateBitField(String peerId, Piece piece) 
	{
		try 
		{
			if (peerProcess.bit.pieces[piece.pieceIndex].isPresent == 1) 
			{
				peerProcess.showLog(peerId + " Piece already received..!");
			} 
			else 
			{
				String fileName = Configurations.fileName;
				File file = new File(peerProcess.peerId, fileName);
				int off = piece.pieceIndex * Configurations.pieceSize;
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				byte[] byteWrite;
				byteWrite = piece.filePiece;
				
				raf.seek(off);
				raf.write(byteWrite);

				this.pieces[piece.pieceIndex].setIsPresent(1);
				this.pieces[piece.pieceIndex].setFromPeerID(peerId);
				raf.close();
				
				peerProcess.showLog(peerProcess.peerId
						+ " has downloaded the PIECE " + piece.pieceIndex
						+ " from Peer " + peerId
						+ ". Now the number of pieces it has is "
						+ peerProcess.bit.ownPieces());

				if (peerProcess.bit.isCompleted()) {
					peerProcess.peersMap.get(peerProcess.peerId).isInterested = 0;
					peerProcess.peersMap.get(peerProcess.peerId).isCompleted = 1;
					peerProcess.peersMap.get(peerProcess.peerId).isChoked = 0;
					updatePeerInfo(peerProcess.peerId, 1);
					
					peerProcess.showLog(peerProcess.peerId + " has DOWNLOADED the complete file.");
				}
			}

		} 
		catch (Exception e) 
		{
			peerProcess.showLog(peerProcess.peerId
					+ " EROR in updating bitfield " + e.getMessage());
		}

	}
    public int ownPieces()
    {
        int cnt = 0;
        for (int i = 0; i < this.size; i++)
            if (this.pieces[i].isPresent == 1)
                cnt++;
        
        return cnt;
    }
    
	public boolean isCompleted() 
	{
        
		for (int i = 0; i < this.size; i++) 
		{
			if (this.pieces[i].isPresent == 0) 
			{
                return false;
            }
        }
        return true;
    }
    
    
	
	// Updates PeerInfo.cfg
	public void updatePeerInfo(String clientID, int hasFile)
	{
		BufferedReader inp = null;
		BufferedWriter out = null;
		
		try 
		{
			inp = new BufferedReader(new FileReader("PeerInfo.cfg"));
		
			String line;
			StringBuffer buffer = new StringBuffer();
		
			while((line = inp.readLine()) != null) 
			{
				if(line.trim().split("\\s+")[0].equals(clientID))
				{
					buffer.append(line.trim().split("\\s+")[0] + " " + line.trim().split("\\s+")[1] + " " + line.trim().split("\\s+")[2] + " " + hasFile);
				}
				else
				{
					buffer.append(line);
					
				}
				buffer.append("\n");
			}
			
			inp.close();
		
			out= new BufferedWriter(new FileWriter("PeerInfo.cfg"));
			out.write(buffer.toString());	
			
			out.close();
		} 
		catch (Exception e) 
		{
			peerProcess.showLog(clientID + " Error in updating the PeerInfo.cfg " +  e.getMessage());
		}
	}
	
	
	
}
