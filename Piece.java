public class Piece 
{
	public int isPresent;
	public String fromPeerID;
	public byte [] filePiece; 			
	public int pieceIndex;
	
	
	public int getIsPresent() {
		return isPresent;
	}
	public void setIsPresent(int isPresent) {
		this.isPresent = isPresent;
	}
	public String getFromPeerID() {
		return fromPeerID;
	}
	public void setFromPeerID(String fromPeerID) {
		this.fromPeerID = fromPeerID;
	}
	public Piece()
	{
		filePiece = new byte[Configurations.pieceSize];
		pieceIndex = -1;
		isPresent = 0;
		fromPeerID = null;
	}
	/**
	 * Decodes the payload and returns a Piece with pieceIndex
	 * @param payload
	 * @return
	 */
	public static Piece decodePiece(byte []payload)
	{
		byte[] byteIndex = new byte[4];
		Piece piece = new Piece();
		System.arraycopy(payload, 0, byteIndex, 0, 4);
		piece.pieceIndex = Converter.byteArrayToInt(byteIndex);
		piece.filePiece = new byte[payload.length-4];
		System.arraycopy(payload, 4, piece.filePiece, 0, payload.length-4);
		return piece;
	}
}
