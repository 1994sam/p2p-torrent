JFLAGS = -g -cp .
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
			.\src\org\networks\java\model\PeerInfo.java \
			.\src\org\networks\java\helper\Const.java \
			.\src\org\networks\java\helper\CommonConfig.java \
			.\src\org\networks\java\helper\FileHandler.java \
			.\src\org\networks\java\helper\MessageStream.java \
			.\src\org\networks\java\helper\PeerConfig.java \
			.\src\org\networks\java\model\Message.java \
			.\src\org\networks\java\service\Client.java \
			.\src\org\networks\java\service\Listener.java \
			.\src\org\networks\java\service\P2PLogger.java \
			.\src\org\networks\java\service\Peer.java \
			.\src\org\networks\java\service\Server.java \
			.\src\org\networks\java\Main.java 
			
CLASS_FILES = \
			.\src\org\networks\java\model\PeerInfo.class \
			.\src\org\networks\java\helper\Const.class \
			.\src\org\networks\java\helper\CommonConfig.class \
			.\src\org\networks\java\helper\FileHandler.class \
			.\src\org\networks\java\helper\MessageStream.class \
			.\src\org\networks\java\helper\PeerConfig.class \
			.\src\org\networks\java\model\Message.class \
			.\src\org\networks\java\service\Client.class \
			.\src\org\networks\java\service\Listener.class \
			.\src\org\networks\java\service\P2PLogger.class \
			.\src\org\networks\java\service\Peer.class \
			.\src\org\networks\java\service\Server.class \
			.\src\org\networks\java\Main.class 


default: classes

classes: $(CLASSES:.java=.class)

%.class : %.java
	$(JC) $(JFLAGS) ${CLASSES}

clean:
	$(RM) ${CLASS_FILES}
run:
#	cd src & java org.networks.java.Main 0
	cd src & java org.networks.java.Main 1