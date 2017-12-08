
/* ------------------
   Client
   usage: java Client [Server hostname] [Server RTSP listening port] [Video file requested]
   ---------------------- */

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Client{
	

	
  //GUI
  //----
  JFrame f = new JFrame("Client");
  JButton setupButton = new JButton("Setup");
  JButton playButton = new JButton("Play");
  JButton pauseButton = new JButton("Pause");
  JButton tearButton = new JButton("Teardown");
  JButton optionButton = new JButton("Options");
  JButton describeButton = new JButton("Describe");
  JPanel mainPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JPanel stats = new JPanel();
  JLabel iconLabel = new JLabel();
  ImageIcon icon;
  JLabel lost = new JLabel("Lost:");
  JLabel korr = new JLabel("Korrigierbar:");

  /*
  Anzahl erhaltener/verlorener Medienpakete + prozentuale Angabe
  Anzahl korrigierter/unkorrigierbarer Medienpakete
  Die Anzeige sollte bis zum Ende des Videos sekündlich aktualisiert werden und dann auf dem Gesamtstand stehen bleiben.
*/


  //RTP variables:
  //----------------
  DatagramPacket rcvdp; //UDP packet received from the server
  DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
  static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets
  
  Timer timer; //timer used to receive data from the UDP socket
  byte[] buf; //buffer used to store data received from the server 
 
  //RTSP variables
  //----------------
  //rtsp states 
  final static int INIT = 0;
  final static int READY = 1;
  final static int PLAYING = 2;
  static int state; //RTSP state == INIT or READY or PLAYING
  Socket RTSPsocket; //socket used to send/receive RTSP messages
  //input and output stream filters
  static BufferedReader RTSPBufferedReader;
  static BufferedWriter RTSPBufferedWriter;
  static String VideoFileName; //video file to request to the server
  int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session
  int RTSPid = 0; //ID of the RTSP session (given by the RTSP Server)
  int desc = 0;
  String descString = new String();
  int val_lost;
  
  
  
  final static String CRLF = "\r\n";

  //Video constants:
  //------------------
  static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
 
  //--------------------------
  //Constructor
  //--------------------------
  public Client() {

    //build GUI
    //--------------------------
 
    //Frame
    f.addWindowListener(new WindowAdapter() {
       public void windowClosing(WindowEvent e) {
	 System.exit(0);
       }
    });

    //Buttons
    buttonPanel.setLayout(new GridLayout(2,0));
    buttonPanel.add(setupButton);
    buttonPanel.add(playButton);
    buttonPanel.add(pauseButton);
    buttonPanel.add(tearButton);
    buttonPanel.add(describeButton);
    buttonPanel.add(optionButton);

    setupButton.addActionListener(new setupButtonListener());
    playButton.addActionListener(new playButtonListener());
    pauseButton.addActionListener(new pauseButtonListener());
    tearButton.addActionListener(new tearButtonListener());

    describeButton.addActionListener(new describeButtonListener());
    optionButton.addActionListener(new optionButtonListener());
    //Image display label
    iconLabel.setIcon(null);
    
    //frame layout
    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(iconLabel,BorderLayout.NORTH);
    mainPanel.add(stats,BorderLayout.NORTH);
    mainPanel.add(buttonPanel,BorderLayout.SOUTH);
    iconLabel.setBounds(0,0,380,280);
    buttonPanel.setBounds(0,280,380,50);

    stats.setLayout(new GridLayout(1,3));
    stats.add(lost);
    stats.add(korr);
    
    f.getContentPane().add(mainPanel);
    f.setSize(new Dimension(390,370));
    f.setVisible(true);

    //init timer
    //--------------------------
    timer = new Timer(20, new timerListener());
    timer.setInitialDelay(0);
    timer.setCoalesce(true);

    //allocate enough memory for the buffer used to receive data from the server
    buf = new byte[15000];    
  }

  //------------------------------------
  //main
  //------------------------------------
  public static void main(String argv[]) throws Exception
  {
    //Create a Client object
    Client theClient = new Client();
    
    //get server RTSP port and IP address from the command line
    //------------------
    int RTSP_server_port = Integer.parseInt(argv[1]);
    String ServerHost = argv[0];
    InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);

    //get video filename to request:
    VideoFileName = argv[2];

    //Establish a TCP connection with the server to exchange RTSP messages
    //------------------
    theClient.RTSPsocket = new Socket(ServerIPAddr, RTSP_server_port);

    //Set input and output stream filters:
    RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPsocket.getInputStream()) );
    RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsocket.getOutputStream()) );

    //init RTSP state:
    state = INIT;
  }


  //------------------------------------
  //Handler for buttons
  //------------------------------------

  //.............
  //TO COMPLETE
  //.............

  //Handler for Setup button
  //-----------------------
  class setupButtonListener implements ActionListener{
    public void actionPerformed(ActionEvent e){

      System.out.println("Setup Button pressed !");      

      if (state == INIT) 
      {

		  //Init non-blocking RTPsocket that will be used to receive data
		  try{
	
		    //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
		    RTPsocket = new DatagramSocket(RTP_RCV_PORT);
	
		    //set TimeOut value of the socket to 5msec.
		    RTPsocket.setSoTimeout(5);
	
		  }
		  catch (SocketException se)
		    {
		      System.out.println("Socket exception: "+se);
		      System.exit(0);
		    }
	
		  //init RTSP sequence number
		  RTSPSeqNb = 1;
		 
		  //Send SETUP message to the server
		  send_RTSP_request("SETUP");
	
		  //Wait for the response 
		  if (parse_server_response() != 200)
		    System.out.println("Invalid Server Response");
		  else 
		    {
		      //change RTSP state and print new state 
		      state = READY;
		      System.out.println("New RTSP state: READY\n");
		    }
	}//else if state != INIT then do nothing
    }
  }
  
  //Handler for Play button
  //-----------------------
  class playButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

      System.out.println("Play Button pressed !"); 

      if (state == READY) 
	{
	  //increase RTSP sequence number
	  RTSPSeqNb++;


	  //Send PLAY message to the server
	  send_RTSP_request("PLAY");

	  //Wait for the response 
	  if (parse_server_response() != 200)
		  System.out.println("Invalid Server Response");
	  else 
	    {
	      //change RTSP state and print out new state
	      state = PLAYING;
	      System.out.println("New RTSP state: PLAYING\n");

	      //start the timer
	      timer.start();
	    }
	}//else if state != READY then do nothing
    }
  }


  //Handler for Pause button
  //-----------------------
  class pauseButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

      System.out.println("Pause Button pressed !");   

      if (state == PLAYING) 
	{
	  //increase RTSP sequence number
    	  RTSPSeqNb++;

	  //Send PAUSE message to the server
	  send_RTSP_request("PAUSE");
	
	  //Wait for the response 
	 if (parse_server_response() != 200)
		  System.out.println("Invalid Server Response");
	  else 
	    {
	      //change RTSP state and print out new state
	      state = READY;
	      System.out.println("New RTSP state: READY\n");
	      
	      //stop the timer
	      timer.stop();
	    }
	}
      //else if state != PLAYING then do nothing
    }
  }

  //Handler for Teardown button
  //-----------------------
  class tearButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){

      System.out.println("Teardown Button pressed !");  

      //increase RTSP sequence number
      RTSPSeqNb++;
      

      //Send TEARDOWN message to the server
      send_RTSP_request("TEARDOWN");

      //Wait for the response 
      if (parse_server_response() != 200)
	System.out.println("Invalid Server Response");
      else 
	{     
	  //change RTSP state and print out new state
	  state = INIT;
	  System.out.println("New RTSP state: INIT");

	  //stop the timer
	  timer.stop();

	  //exit
	  System.exit(0);
	}
    }
  }
  //Händeler für Optionsbutton
  //-----------------------------------------------------
  class optionButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){
        //increase RTSP sequence number
        RTSPSeqNb++;
        //Send OPTIONS message to the server
        send_RTSP_request("OPTIONS");
      //Wait for the response 
        if (parse_server_response() != 200) {
            System.out.println("Invalid Server Response");
        }
        else {     
            System.out.println("Received response for OPTIONS");
        }
    }
  }
  
  //Händler für Describe Button
  //------------------------------------------------------
    class describeButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e){
    	System.out.println("Sending DESCRIBE request");  
    	desc=1;
        //increase RTSP sequence number
        RTSPSeqNb++;

        //Send DESCRIBE message to the server
        send_RTSP_request("DESCRIBE");
        descString = new String("DESCRIBE");
        //Wait for the response 
        if (parse_server_response() != 200) {
            System.out.println("Invalid Server Response");
        }
        else {     
            System.out.println("Received response for DESCRIBE");
        }
    }
  }

  


  //------------------------------------
  //Handler for timer
  //------------------------------------
  
  class timerListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {

      //Construct a DatagramPacket to receive data from the UDP socket
      rcvdp = new DatagramPacket(buf, buf.length);

      try{
	//receive the DP from the socket:
	RTPsocket.receive(rcvdp);
 
	//create an RTPpacket object from the DP
	RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

	//print important header fields of the RTP packet received: 
	System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());

	//print header bitstream:
	rtp_packet.printheader();

	//get the payload bitstream from the RTPpacket object
	int payload_length = rtp_packet.getpayload_length();
	byte [] payload = new byte[payload_length];
	rtp_packet.getpayload(payload);

	//get an Image object from the payload bitstream
	Toolkit toolkit = Toolkit.getDefaultToolkit();
	Image image = toolkit.createImage(payload, 0, payload_length);

	//display the image as an ImageIcon object
	icon = new ImageIcon(image);
	iconLabel.setIcon(icon);
      }
      catch (InterruptedIOException iioe){
	//System.out.println("Nothing to read");
      }
      catch (IOException ioe) {
	System.out.println("||Handler for timer||Exception caught: "+ioe);
      }
    }
  }

  //------------------------------------
  //Parse Server Response
  //------------------------------------
  private int parse_server_response() 
  {
    int reply_code = 0;

    try{
      //parse status line and extract the reply_code:
      String StatusLine = RTSPBufferedReader.readLine();
      //System.out.println("RTSP Client - Received from Server:");
      System.out.println(StatusLine);
    
      StringTokenizer tokens = new StringTokenizer(StatusLine);
      tokens.nextToken(); //skip over the RTSP version
      reply_code = Integer.parseInt(tokens.nextToken());
      
      //if reply code is OK get and print the 2 other lines
      if (reply_code == 200)
	{
	  String SeqNumLine = RTSPBufferedReader.readLine();
	  System.out.println(SeqNumLine);
	  
	  String SessionLine = RTSPBufferedReader.readLine();
	  System.out.println(SessionLine);
	
	  if (state == INIT) {
		  tokens = new StringTokenizer(SessionLine);
		  tokens.nextToken(); //skip over the Session:
		  RTSPid = Integer.parseInt(tokens.nextToken());
	  }
	  if(descString.compareTo("DESCRIBE") == 0) {
		  for(int i = 0;i<3;i++) {
		      String descLine = RTSPBufferedReader.readLine();
		      //System.out.println("RTSP Client - Received from Server:");
		      System.out.println(descLine);
		  }
	  }
	}
    }
    catch(Exception ex)
      {
	System.out.println("||Parse Server Response||Exception caught: "+ex);
	System.exit(0);
      }
    
    return(reply_code);
  }

  //------------------------------------
  //Send RTSP Request
  //------------------------------------

  //.............
  //TO COMPLETE
  //.............
  
  private void send_RTSP_request(String request_type)
  {
    try{
      //Use the RTSPBufferedWriter to write to the RTSP socket

      //write the request line:
      RTSPBufferedWriter.write(request_type + " " + VideoFileName + " RTSP/1.0" + "\r\n");

      //write the CSeq line: 
      RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + "\r\n");

      //check if request_type is equal to "SETUP" and in this case write the Transport: line advertising to the server the port used to receive the RTP packets RTP_RCV_PORT
      if(request_type == "SETUP"){
    	  RTSPBufferedWriter.write("Transport: RTP/UDP); client_port= "+ RTP_RCV_PORT + "\r\n");
      }else{
    	  RTSPBufferedWriter.write("Session: " + RTSPid + "\r\n");
      } 
      //otherwise, write the Session line from the RTSPid field



      RTSPBufferedWriter.flush();
    }
    catch(Exception ex)
      {
	System.out.println("||Send RTSP Request||Exception caught: "+ex);
	System.exit(0);
      }
  }

}//end of Class Client

