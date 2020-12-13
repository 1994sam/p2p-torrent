# CN Project: P2P Torrent



## Team Members

* **Bharat Kulkarni** 
	* UF ID: 5689 3029
	* bharat.kulkarni@ufl.edu
* **Krutantak Patil** 
	* UF ID: 5615 6343
	* krutantakb.patil@ufl.edu
* **Sameer Aher**
	* UF ID: 4915 9225 
	* aher.sa@ufl.edu  

## Link to Video Demonstration
* **[Code Overview & Local Demo Video](https://uflorida-my.sharepoint.com/:v:/g/personal/krutantakb_patil_ufl_edu/EVhaFHWlFzVFjHTSbrCEIywB-BdPErzINUELzBtKHRUUOQ?e=Bup9QR)**
* **[Server Demo Video](https://uflorida-my.sharepoint.com/:v:/g/personal/krutantakb_patil_ufl_edu/EbGNA43h8fpNsD7gIEdSx24Bjoa4P_8hh1ITfW8cZ1aL0g?e=PK7feb)**

## Class Structure
This section will contain details of the location of the code which executes the following tasks:

#### 1] Start the peer processes and establishing TCP among peers.
* In your video, you should clearly show that your program will read the Common.cfg to correctly set the related variables.
	*  CommonConfig.java is responsible to read the properties from Common.cfg. 
* In your video, you should clearly show that your program will read the PeerInfo.cfg to correctly and set the bitfield.
	* PeerConfig.java is responsible to read PeerInfo.java and creat PeerInfo objects.
	* Bitfield for each PeerInfo object is set in the Peer class.
* In your video, you should clearly show that your program will let each peer make TCP connects to all peers that started before it.
	* This is done in the processHandshake() in Client.java and Server.java.
* When a peer is connected to at least one other peer, it starts to exchange pieces as described in the protocol description section. A peer terminates when it finds out that all the peers, not just itself, have downloaded the complete file.
	*  VerifyCompletionTask.java is scheduled to check if all the peers have the complete file.

#### 2] After connection:
* Handshake message: Whenever a connection is established between two peers, each of the peers of the connection sends to the other one the handshake message before sending other messages.
	* Client.java performs handshake in processHandshake() as well as Server.java is responsible to read Handshake messages. 

* Exchange bitfield message.
	* readBitFieldMsg() and sendBitFieldMsg() in Client.java handle exchange of Bitfield messages. 
* Send ‘interested’ or ‘not interested’ message.
	* Interested and Not Interested messages are sent in readHaveMsg() and readBitFieldMsg() in Client.java
* Send *k* ‘unchoke’ and ‘choke’ messages every *p* seconds.
	* chokeNeighbor() and unchokeNeighbor() in Client.java which are called from setPreferredNeighbors() in Peer.java. generatePreferredNeighbors.java calls setPreferredNeighbors() every *p* seconds.
* Set optimistically unchoked neighbor every ‘*m*’ seconds.
	* This is handled in OptimisticallyUnchokingTask,java is responsible to unchoke neighbors. 

#### 3] File Exchange 
* Send ‘request’ message.
	*   requestPiece() in Client.java sends request message.
* Send ‘have’ message.
	* sendHaveMsg() in Client.java
* Send ‘not interested’ message.
	* Not Interested message is sent in readHaveMsg() and readBitFieldMsg() in Client.java
* Send ‘interested’ message.
	* Interested message is sent in readHaveMsg() and readBitFieldMsg() in Client.java
* Send ‘piece’ message.
	* readRequestMsg() in Client.java sends peice message.
* Receive ‘have’ message and update related bitfield.
	* readHaveMsg() and updateNeighborPieceIndex() in Peer.java updates the bitfield. 

#### 4] Stop service correctly.
* VerifyCompletionTask.java is scheduled to run every 5 seconds to check for completion of download at all peers to initiate shutdown.  


































