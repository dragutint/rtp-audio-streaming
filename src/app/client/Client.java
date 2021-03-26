package app.client;


import app.common.RTCPPacket;
import app.common.RTPPacket;
import app.common.RTSPRequestEnum;
import app.common.RTSPStateEnum;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

public class Client {

    private final ClientForm form;

    DatagramPacket receivedUdpPacket;            //UDP packet received from the server
    DatagramSocket rtpSocket;        //socket to be used to send and receive UDP packets
    static int RTP_RCV_PORT = 25000; //port where the app.client will receive the RTP packets
    RTSPController rtspController;

    Timer timer; //timer used to receive data from the UDP socket
    byte[] buf;  //buffer used to store data received from the server 

    static RTSPStateEnum state;

    //RTCP variables
    //----------------
    DatagramSocket RTCPsocket;          //UDP socket for sending RTCP packets
    static int RTCP_RCV_PORT = 19001;   //port where the app.client will receive the RTP packets
    static int RTCP_PERIOD = 400;       //How often to send RTCP packets
    RtcpSender rtcpSender;

    //Statistics variables:
    //------------------
    double statDataRate;        //Rate of video data received in bytes/s
    int statTotalBytes;         //Total number of bytes received in a session
    double statStartTime;       //Time in milliseconds when start is pressed
    double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
    float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
    int statCumLost;            //Number of packets lost
    int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
    int statHighSeqNb;          //Highest sequence number received in session

    FrameSynchronizer fsynch;

    public Client() throws IOException {
        form = new ClientForm();
        rtspController = new RTSPController();

        form.initListenerSetupButton(new setupButtonListener());
        form.initListenerPlayButton(new playButtonListener());
        form.initListenerPauseButton(new pauseButtonListener());
        form.initListenerTeardownButton(new tearButtonListener());
        form.initListenerDescribeButton(new describeButtonListener());

        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        rtcpSender = new RtcpSender(400);

        buf = new byte[15000];

        fsynch = new FrameSynchronizer(100);

        state = RTSPStateEnum.INIT;
    }

    class setupButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            System.out.println("Setup Button pressed !");
            if (state == RTSPStateEnum.INIT) {
                //Init non-blocking RTPsocket that will be used to receive data
                try {
                    //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT
                    rtpSocket = new DatagramSocket(RTP_RCV_PORT);
                    //UDP socket for sending QoS RTCP packets
                    RTCPsocket = new DatagramSocket();
                    //set TimeOut value of the socket to 5msec.
                    rtpSocket.setSoTimeout(5);
                } catch (SocketException se) {
                    System.out.println("Socket exception: " + se);
                    System.exit(0);
                }

                //init RTSP sequence number
                rtspController.setRtspSequence(1);

                //Send SETUP message to the server
                rtspController.sendRequest(RTSPRequestEnum.SETUP);

                //Wait for the response 
                if (rtspController.parseServerResponse(state) != 200)
                    System.out.println("Invalid app.server.Server Response");
                else {
                    //change RTSP state and print new state 
                    state = RTSPStateEnum.READY;
                    System.out.println("New RTSP state: READY");
                }
            }
            //else if state != INIT then do nothing
        }
    }

    //Handler for Play button
    //-----------------------
    class playButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            System.out.println("Play Button pressed!");

            //Start to save the time in stats
            statStartTime = System.currentTimeMillis();

            if (state == RTSPStateEnum.READY) {
                //increase RTSP sequence number
                rtspController.increaseSequence();

                //Send PLAY message to the server
                rtspController.sendRequest(RTSPRequestEnum.PLAY);

                //Wait for the response 
                if (rtspController.parseServerResponse(state) != 200) {
                    System.out.println("Invalid app.server.Server Response");
                } else {
                    //change RTSP state and print out new state
                    state = RTSPStateEnum.PLAYING;
                    System.out.println("New RTSP state: PLAYING");

                    //start the timer
                    timer.start();
                    rtcpSender.startSend();
                }
            }
            //else if state != READY then do nothing
        }
    }

    //Handler for Pause button
    //-----------------------
    class pauseButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            System.out.println("Pause Button pressed!");

            if (state == RTSPStateEnum.PLAYING) {
                //increase RTSP sequence number
                rtspController.increaseSequence();

                rtspController.sendRequest(RTSPRequestEnum.PAUSE);

                //Wait for the response 
                if (rtspController.parseServerResponse(state) != 200)
                    System.out.println("Invalid app.server.Server Response");
                else {
                    //change RTSP state and print out new state
                    state = RTSPStateEnum.READY;
                    System.out.println("New RTSP state: READY");

                    //stop the timer
                    timer.stop();
                    rtcpSender.stopSend();
                }
            }
            //else if state != PLAYING then do nothing
        }
    }

    //Handler for Teardown button
    //-----------------------
    class tearButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            System.out.println("Teardown Button pressed !");

            rtspController.increaseSequence();
            rtspController.sendRequest(RTSPRequestEnum.TEARDOWN);

            //Wait for the response 
            if (rtspController.parseServerResponse(state) != 200)
                System.out.println("Invalid app.server.Server Response");
            else {
                //change RTSP state and print out new state
                state = RTSPStateEnum.INIT;
                System.out.println("New RTSP state: INIT");

                //stop the timer
                timer.stop();
                rtcpSender.stopSend();

                //exit
                System.exit(0);
            }
        }
    }

    // Get information about the data stream
    class describeButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            System.out.println("Sending DESCRIBE request");

            //increase RTSP sequence number
            rtspController.increaseSequence();

            rtspController.sendRequest(RTSPRequestEnum.DESCRIBE);

            //Wait for the response 
            if (rtspController.parseServerResponse(state) != 200) {
                System.out.println("Invalid app.server.Server Response");
            } else {
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
            receivedUdpPacket = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket, save time for stats
                rtpSocket.receive(receivedUdpPacket);

                double curTime = System.currentTimeMillis();
                statTotalPlayTime += curTime - statStartTime;
                statStartTime = curTime;

                //create an RTPpacket object from the DP
                RTPPacket rtp_packet = new RTPPacket(receivedUdpPacket.getData(), receivedUdpPacket.getLength());
                int seqNb = rtp_packet.getsequencenumber();

                //this is the highest seq num received

                //print important header fields of the RTP packet received: 
                System.out.println("Got RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                        + rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte[] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //compute stats and update the label in GUI
                statExpRtpNb++;
                if (seqNb > statHighSeqNb) {
                    statHighSeqNb = seqNb;
                }
                if (statExpRtpNb != seqNb) {
                    statCumLost++;
                }
                statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
                statFractionLost = (float) statCumLost / statHighSeqNb;
                statTotalBytes += payload_length;
                updateStatsLabel();

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                fsynch.addFrame(toolkit.createImage(payload, 0, payload_length), seqNb);

                //display the image as an ImageIcon object
                form.setIcon(fsynch.nextFrame());
            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }
    }

    //------------------------------------
    // Send RTCP control packets for QoS feedback
    //------------------------------------
    class RtcpSender implements ActionListener {

        private Timer rtcpTimer;
        int interval;

        // Stats variables
        private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
        private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
        private int lastHighSeqNb;      // The last highest Seq number received
        private int lastCumLost;        // The last cumulative packets lost
        private float lastFractionLost; // The last fraction lost

        Random randomGenerator;         // For testing only

        public RtcpSender(int interval) {
            this.interval = interval;
            rtcpTimer = new Timer(interval, this);
            rtcpTimer.setInitialDelay(0);
            rtcpTimer.setCoalesce(true);
            randomGenerator = new Random();
        }

        public void run() {
            System.out.println("RtcpSender Thread Running");
        }

        public void actionPerformed(ActionEvent e) {

            // Calculate the stats for this period
            numPktsExpected = statHighSeqNb - lastHighSeqNb;
            numPktsLost = statCumLost - lastCumLost;
            lastFractionLost = numPktsExpected == 0 ? 0f : (float) numPktsLost / numPktsExpected;
            lastHighSeqNb = statHighSeqNb;
            lastCumLost = statCumLost;

            //To test lost feedback on lost packets
            // lastFractionLost = randomGenerator.nextInt(10)/10.0f;

            RTCPPacket rtcp_packet = new RTCPPacket(lastFractionLost, statCumLost, statHighSeqNb);
            int packet_length = rtcp_packet.getlength();
            byte[] packet_bits = new byte[packet_length];
            rtcp_packet.getpacket(packet_bits);

            try {
                DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, rtspController.getServerIPAddress(), RTCP_RCV_PORT);
                RTCPsocket.send(dp);
            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }

        // Start sending RTCP packets
        public void startSend() {
            rtcpTimer.start();
        }

        // Stop sending RTCP packets
        public void stopSend() {
            rtcpTimer.stop();
        }
    }

    //------------------------------------
    //Synchronize frames
    //------------------------------------
    class FrameSynchronizer {

        private ArrayDeque<Image> queue;
        private int bufSize;
        private int curSeqNb;
        private Image lastImage;

        public FrameSynchronizer(int bsize) {
            curSeqNb = 1;
            bufSize = bsize;
            queue = new ArrayDeque<Image>(bufSize);
        }

        //synchronize frames based on their sequence number
        public void addFrame(Image image, int seqNum) {
            if (seqNum < curSeqNb) {
                queue.add(lastImage);
            } else if (seqNum > curSeqNb) {
                for (int i = curSeqNb; i < seqNum; i++) {
                    queue.add(lastImage);
                }
                queue.add(image);
            } else {
                queue.add(image);
            }
        }

        //get the next synchronized frame
        public Image nextFrame() {
            curSeqNb++;
            lastImage = queue.peekLast();
            return queue.remove();
        }
    }

    private void updateStatsLabel() {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        form.setTotalBytesText("Total Bytes Received: " + statTotalBytes);
        form.setPacketLossRateText("Packet Lost Rate: " + formatter.format(statFractionLost));
        form.setDataRateText("Data Rate: " + formatter.format(statDataRate) + " bytes/s");
    }
}
