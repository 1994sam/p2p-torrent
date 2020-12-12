JFLAGS = -g -cp out -d out
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

SRC_PATH = src/

CLASS_PATH = out/

CLASSES = $(shell find $(SRC_PATH) -regex ".*\.\(java\)")

default:
	$(JC) $(JFLAGS) ${CLASSES}

clean:
	rm -rf ${CLASS_PATH}

run1:
	cd src & java -cp out org.networks.java.PeerProcess 1001

run2:
	cd src & java -cp out org.networks.java.PeerProcess 1002

run3:
	cd src & java -cp out org.networks.java.PeerProcess 1003

run4:
	cd src & java -cp out org.networks.java.PeerProcess 1004

run5:
	cd src & java -cp out org.networks.java.PeerProcess 1005

run6:
	cd src & java -cp out org.networks.java.PeerProcess 1006