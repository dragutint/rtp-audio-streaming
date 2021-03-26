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

public class ClientController {

    private final ClientForm form;
    private final RTSPController rtspController;
    private final FrameSynchronizer frameSynchronizer;

    DatagramPacket receivedUdpPacket;            //UDP packet received from the server
    DatagramSocket rtpSocket;        //socket to be used to send and receive UDP packets

    Timer videStreamTimer; //timer used to receive data from the UDP socket
    byte[] buf;  //buffer used to store data received from the server

    private RTSPStateEnum state;

    DatagramSocket rtcpSocket;              //UDP socket for sending RTCP packets
    int RTCP_RCV_PORT = 19001;              //port where the app.client will receive the RTP packets
    RTCPSender rtcpSender;

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


    public ClientController() throws IOException {
        form = new ClientForm();
        rtspController = new RTSPController();

        form.initListenerSetupButton(new SetupButtonListener());
        form.initListenerPlayButton(new PlayButtonListener());
        form.initListenerPauseButton(new PauseButtonListener());
        form.initListenerTeardownButton(new TearButtonListener());
        form.initListenerDescribeButton(new DescribeButtonListener());

        videStreamTimer = new Timer(20, new VideoStreamListener());
        videStreamTimer.setInitialDelay(0);
        videStreamTimer.setCoalesce(true);

        int RTCP_INTERVAL = 400;
        rtcpSender = new RTCPSender(RTCP_INTERVAL);

        buf = new byte[15000];

        frameSynchronizer = new FrameSynchronizer(100);

        state = RTSPStateEnum.INIT;
    }

    private class SetupButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Setup Button pressed !");

            if (state == RTSPStateEnum.INIT) {
                try {
                    rtpSocket = new DatagramSocket(RTSPController.RTP_RCV_PORT);
                    rtpSocket.setSoTimeout(5);

                    rtcpSocket = new DatagramSocket();
                } catch (SocketException se) {
                    System.out.println("Socket exception: " + se);
                    System.exit(0);
                }

                rtspController.setRtspSequence(1);
                rtspController.sendRequest(RTSPRequestEnum.SETUP);

                if (rtspController.parseServerResponse(state) != 200)
                    System.out.println("Invalid app.server.Server Response");
                else {
                    state = RTSPStateEnum.READY;
                    System.out.println("New RTSP state: READY");
                }
            }
        }
    }

    private class PlayButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Play Button pressed!");

            statStartTime = System.currentTimeMillis();

            if (state == RTSPStateEnum.READY) {
                rtspController.sendRequest(RTSPRequestEnum.PLAY);

                if (rtspController.parseServerResponse(state) != 200)
                    System.out.println("Invalid app.server.Server Response");
                else {
                    state = RTSPStateEnum.PLAYING;
                    System.out.println("New RTSP state: PLAYING");

                    videStreamTimer.start();
                    rtcpSender.startSend();
                }
            }
        }
    }

    private class PauseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Pause Button pressed!");

            if (state == RTSPStateEnum.PLAYING) {
                rtspController.sendRequest(RTSPRequestEnum.PAUSE);

                if (rtspController.parseServerResponse(state) != 200)
                    System.out.println("Invalid app.server.Server Response");
                else {
                    state = RTSPStateEnum.READY;
                    System.out.println("New RTSP state: READY");

                    videStreamTimer.stop();
                    rtcpSender.stopSend();
                }
            }
        }
    }

    private class TearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Teardown Button pressed !");

            rtspController.sendRequest(RTSPRequestEnum.TEARDOWN);

            if (rtspController.parseServerResponse(state) != 200)
                System.out.println("Invalid app.server.Server Response");
            else {
                state = RTSPStateEnum.INIT;
                System.out.println("New RTSP state: INIT");

                videStreamTimer.stop();
                rtcpSender.stopSend();

                System.exit(0);
            }
        }
    }


    private class DescribeButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Sending DESCRIBE request");

            rtspController.sendRequest(RTSPRequestEnum.DESCRIBE);

            if (rtspController.parseServerResponse(state) != 200)
                System.out.println("Invalid app.server.Server Response");
            else
                System.out.println("Received response for DESCRIBE");
        }
    }

    private class VideoStreamListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            receivedUdpPacket = new DatagramPacket(buf, buf.length);

            try {
                rtpSocket.receive(receivedUdpPacket);

                double curTime = System.currentTimeMillis();
                statTotalPlayTime += curTime - statStartTime;
                statStartTime = curTime;

                RTPPacket rtpPacket = new RTPPacket(receivedUdpPacket.getData(), receivedUdpPacket.getLength());
                int seqNb = rtpPacket.getSequenceNumber();

                System.out.println("Got RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtpPacket.getTimeStamp() + " ms, of type "
                        + rtpPacket.getPayloadType());
                rtpPacket.printHeader();

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
                statTotalBytes += rtpPacket.getPayloadLength();
                updateStatsLabel();

                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                frameSynchronizer.addFrame(toolkit.createImage(rtpPacket.getPayload(), 0, rtpPacket.getPayloadLength()), seqNb);

                //display the image as an ImageIcon object
                form.setIcon(frameSynchronizer.nextFrame());
            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }
    }

    private class RTCPSender implements ActionListener {
        private final Timer rtcpTimer;
        int interval;

        private int lastHighSeqNb;      // The last highest Seq number received
        private int lastCumLost;        // The last cumulative packets lost

        Random randomGenerator;         // For testing only

        public RTCPSender(int interval) {
            this.interval = interval;
            rtcpTimer = new Timer(interval, this);
            rtcpTimer.setInitialDelay(0);
            rtcpTimer.setCoalesce(true);
            randomGenerator = new Random();
        }

        public void actionPerformed(ActionEvent e) {

            // Calculate the stats for this period
            // Stats variables
            // Number of RTP packets expected since the last RTCP packet
            int numPacketsExpected = statHighSeqNb - lastHighSeqNb;
            // Number of RTP packets lost since the last RTCP packet
            int numPacketsLost = statCumLost - lastCumLost;
            // The last fraction lost
            float lastFractionLost = numPacketsExpected == 0 ? 0f : (float) numPacketsLost / numPacketsExpected;
            lastHighSeqNb = statHighSeqNb;
            lastCumLost = statCumLost;

            RTCPPacket rtcpPacket = new RTCPPacket(lastFractionLost, statCumLost, statHighSeqNb);

            try {
                DatagramPacket dp = new DatagramPacket(rtcpPacket.getPacket(), rtcpPacket.getLength(), rtspController.getServerIPAddress(), RTCP_RCV_PORT);
                rtcpSocket.send(dp);
            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }

        public void startSend() {
            rtcpTimer.start();
        }

        public void stopSend() {
            rtcpTimer.stop();
        }
    }

    private void updateStatsLabel() {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        form.setTotalBytesText("Total Bytes Received: " + statTotalBytes);
        form.setPacketLossRateText("Packet Lost Rate: " + formatter.format(statFractionLost));
        form.setDataRateText("Data Rate: " + formatter.format(statDataRate) + " bytes/s");
    }
}
