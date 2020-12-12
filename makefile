JC = javac
JFLAGS = -g
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Conversion.java \
	Data.java \
	Source.java \
	Handshake.java \
	Server.java \
	MessageProcessor.java \
	PeerDataRateComparator.java \
	peerProcess.java \
	Piece.java \
	RemotePeerHandler.java \
	RemotePeerInfo.java \
	startRemotePeers.java
default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class