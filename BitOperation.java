import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;


public class BitOperation implements MessageConstants 
{
	public Piece[] pieces;
	public int size;

	public BitOperation() 
	{
		size = (int) Math.ceil(((double) CommonProperties.fileSize / (double) CommonProperties.pieceSize));
		this.pieces = new Piece[size];

		for (int i = 0; i < this.size; i++)
			this.pieces[i] = new Piece();

	}
	
	public int getSize() 
	{
		return size;
	}


	public void setSize(int size) 
	{
		this.size = size;
	}

	
	public Piece[] getPieces() 
	{
		return pieces;
	}

	public void setPieces(Piece[] pieces) 
	{
		this.pieces = pieces;
	}
	
	public byte[] encode()
	{
		return this.getBytes();
	}
	
	public static BitOperation decode(byte[] b)
	{
		BitOperation returnBitOperation = new BitOperation();
		for(int i = 0 ; i < b.length; i ++)
		{
			int count = 7;
			while(count >=0)
			{
				int test = 1 << count;
				if(i * 8 + (8-count-1) < returnBitOperation.size)
				{
					if((b[i] & (test)) != 0)
						returnBitOperation.pieces[i * 8 + (8-count-1)].isPresent = 1;
					else
						returnBitOperation.pieces[i * 8 + (8-count-1)].isPresent = 0;
				}
				count--;
			}
		}
		
		return returnBitOperation;
	}
	
	
	public synchronized boolean compare(BitOperation yourBitOperation) 
	{
		int yourSize = yourBitOperation.getSize();
		

		for (int i = 0; i < yourSize; i++) 
		{
			if (yourBitOperation.getPieces()[i].getIsPresent() == 1
					&& this.getPieces()[i].getIsPresent() == 0) 
			{
				return true;
			} 
			else
				continue;
		}

		return false;
	}

	public synchronized int returnFirstDiff(BitOperation yourBitOperation) 
	{
		int mySize = this.getSize();
		int yourSize = yourBitOperation.getSize();

		if (mySize >= yourSize) 
		{
			for (int i = 0; i < yourSize; i++) 
			{
				if (yourBitOperation.getPieces()[i].getIsPresent() == 1
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
				if (yourBitOperation.getPieces()[i].getIsPresent() == 1
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
		int tempInt = 0;
		int count = 0;
		int Cnt;
		for (Cnt = 1; Cnt <= this.size; Cnt++)
		{
			int tempP = this.pieces[Cnt-1].isPresent;
			tempInt = tempInt << 1;
			if (tempP == 1) 
			{
				tempInt = tempInt + 1;
			} 
			else
				tempInt = tempInt + 0;

			if (Cnt % 8 == 0 && Cnt!=0) 
			{
				iP[count] = (byte) tempInt;
				count++;
				tempInt = 0;
			}
			
		}
		if ((Cnt-1) % 8 != 0) 
		{
			int tempShift = ((size) - (size / 8) * 8);
			tempInt = tempInt << (8 - tempShift);
			iP[count] = (byte) tempInt;
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

	        ch = (byte) (ch & 0x0F);    

	        out.append(pseudo[ (int) ch]);

	        ch = (byte) (in[i] & 0x0F); 

	        out.append(pseudo[ (int) ch]); 
	        i++;
	    }

	    String rslt = new String(out);

	    return rslt;
	}
    
	
	public void initOwnBitOperation(String OwnPeerId, int hasFile) 
	{

		if (hasFile != 1) 
		{

			for (int i = 0; i < this.size; i++) 
			{

				this.pieces[i].setIsPresent(0);
				this.pieces[i].setFromPeerID(OwnPeerId);
			}

		} 
		else 
		{

			for (int i = 0; i < this.size; i++) 
			{
				this.pieces[i].setIsPresent(1);
				this.pieces[i].setFromPeerID(OwnPeerId);
			}

		}

	}

	 
	public synchronized void updateBitOperation(String peerId, Piece piece) 
	{
		try 
		{
			if (peerProcess.ownBitOperation.pieces[piece.pieceIndex].isPresent == 1) 
			{
				peerProcess.showLog(peerId + " Piece already received!!");
			} 
			else 
			{
				String fileName = CommonProperties.fileName;
				File file = new File(peerProcess.peerID, fileName);
				int off = piece.pieceIndex * CommonProperties.pieceSize;
				RandomAccessFile raf = new RandomAccessFile(file, "rw");
				byte[] byteWrite;
				byteWrite = piece.filePiece;
				
				raf.seek(off);
				raf.write(byteWrite);

				this.pieces[piece.pieceIndex].setIsPresent(1);
				this.pieces[piece.pieceIndex].setFromPeerID(peerId);
				raf.close();
				
				peerProcess.showLog(peerProcess.peerID
						+ " has downloaded the PIECE " + piece.pieceIndex
						+ " from Peer " + peerId
						+ ". Now the number of pieces it has is "
						+ peerProcess.ownBitOperation.ownPieces());

				if (peerProcess.ownBitOperation.isCompleted()) {
					peerProcess.remotePeerInfoHash.get(peerProcess.peerID).isInterested = 0;
					peerProcess.remotePeerInfoHash.get(peerProcess.peerID).isCompleted = 1;
					peerProcess.remotePeerInfoHash.get(peerProcess.peerID).isChoked = 0;
					updatePeerInfo(peerProcess.peerID, 1);
					
					peerProcess.showLog(peerProcess.peerID + " has DOWNLOADED the complete file.");
				}
			}

		} 
		catch (Exception e) 
		{
			peerProcess.showLog(peerProcess.peerID
					+ " EROR in updating bitfield " + e.getMessage());
		}

	}
    public int ownPieces()
    {
        int count = 0;
        for (int i = 0; i < this.size; i++)
            if (this.pieces[i].isPresent == 1)
                count++;
        
        return count;
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
		BufferedWriter out = null;
		BufferedReader in = null;
		
		try 
		{
			in= new BufferedReader(new FileReader("PeerInfo.cfg"));
		
			String line;
			StringBuffer buffer = new StringBuffer();
		
			while((line = in.readLine()) != null) 
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
			
			in.close();
		
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
