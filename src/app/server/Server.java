package app.server;

import app.common.RTCPPacket;
import app.common.RTPPacket;
import app.common.RTSPRequestEnum;
import app.common.RTSPStateEnum;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.Timer;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

public class Server implements ActionListener {

    ServerForm form;
    //RTP variables:
    //----------------
    DatagramSocket rtpSocket; //socket to be used to send and receive UDP packets

    InetAddress clientIPAddr;   //app.client.Client IP address
    int rtpDestPort = 0;      //destination port for RTP packets  (given by the RTSP app.client.Client)
    int RTSP_dest_port = 0;

    //Video variables:
    //----------------
    int imageNumber = 0; //image nb of the image currently transmitted
    VideoStream video; //app.server.VideoStream object used to access video frames
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    static int FRAME_PERIOD = 50; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer timer;    //timer used to send the images at the video frame rate
    int sendDelay;  //the delay to send images over the wire. Ideally should be
    //equal to the frame rate of the video file, but may be
    //adjusted when congestion is detected.

    static RTSPStateEnum state;
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    static BufferedReader rtspBufferedReader;
    static BufferedWriter rtspBufferedWriter;
    static String videoFileName; //video file requested from the app.client
    static String rtspId = UUID.randomUUID().toString(); //ID of the RTSP session
    int rtspSeqNb = 0; //Sequence number of RTSP messages within the session


    //RTCP variables
    //----------------
    static int RTCP_RCV_PORT = 19001; //port where the app.client will receive the RTP packets
    static int RTCP_PERIOD = 400;     //How often to check for control events
    DatagramSocket rtcpSocket;
    RtcpReceiver rtcpReceiver;
    int congestionLevel;

    //Performance optimization and Congestion control
    ImageTranslator imgTranslator;
    CongestionController cc;

    final static String CRLF = "\r\n";

    public Server() throws IOException {
        sendDelay = FRAME_PERIOD;
        timer = new Timer(sendDelay, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        cc = new CongestionController(600);

        rtcpReceiver = new RtcpReceiver(RTCP_PERIOD);

        imgTranslator = new ImageTranslator(0.8f);

        form = new ServerForm();
        form.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                timer.stop();
                rtcpReceiver.stopRcv();
                System.exit(0);
            }
        });

        //get RTSP socket port from the command line
        int rtspPort = 1051;
        RTSP_dest_port = rtspPort;

        //Initiate TCP connection with the app.client for the RTSP session
        ServerSocket listenSocket = new ServerSocket(rtspPort);
        RTSPsocket = listenSocket.accept();
        listenSocket.close();

        clientIPAddr = RTSPsocket.getInetAddress();

        state = RTSPStateEnum.INIT;

        rtspBufferedReader = new BufferedReader(new InputStreamReader(RTSPsocket.getInputStream()));
        rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(RTSPsocket.getOutputStream()));
    }

    //------------------------
    //Handler for timer
    //------------------------
    public void actionPerformed(ActionEvent e) {
        byte[] frame;

        if (imageNumber < VIDEO_LENGTH) {
            imageNumber++;

            try {
                byte[] buf = new byte[20000];

                int image_length = video.getNextFrame(buf);

                if (congestionLevel > 0) {
                    imgTranslator.setCompressionQuality(1.0f - congestionLevel * 0.2f);
                    frame = imgTranslator.compress(Arrays.copyOfRange(buf, 0, image_length));
                    image_length = frame.length;
                    System.arraycopy(frame, 0, buf, 0, image_length);
                }

                RTPPacket rtpPacket = new RTPPacket(MJPEG_TYPE, imageNumber, imageNumber * FRAME_PERIOD, buf, image_length);

                DatagramPacket dp = new DatagramPacket(rtpPacket.getPacket(), rtpPacket.getLength(), clientIPAddr, rtpDestPort);
                rtpSocket.send(dp);

                System.out.println("Send frame #" + imageNumber + ", Frame size: " + image_length + " (" + buf.length + ")");
                rtpPacket.printHeader();

                form.setText("Send frame #" + imageNumber);
            } catch (Exception ex) {
                System.out.println("Exception caught: " + ex);
                System.exit(0);
            }
        } else {
            timer.stop();
            rtcpReceiver.stopRcv();
        }
    }

    public void startListening() throws Exception {
        boolean done = false;
        while (!done) {
            RTSPRequestEnum req = parseRequest();

            if (RTSPRequestEnum.SETUP.equals(req)) {
                done = true;

                state = RTSPStateEnum.READY;
                System.out.println("New RTSP state: READY");

                sendResponse();

                video = new VideoStream(videoFileName);
                rtpSocket = new DatagramSocket();
                rtcpSocket = new DatagramSocket(RTCP_RCV_PORT);
            }
        }

        while (true) {
            RTSPRequestEnum req = parseRequest();

            if (RTSPRequestEnum.PLAY.equals(req) && RTSPStateEnum.READY.equals(state)) {
                sendResponse();

                timer.start();
                rtcpReceiver.startRcv();

                state = RTSPStateEnum.PLAYING;
                System.out.println("New RTSP state: PLAYING");
            } else if (RTSPRequestEnum.PAUSE.equals(req) && RTSPStateEnum.PLAYING.equals(state)) {
                sendResponse();

                timer.stop();
                rtcpReceiver.stopRcv();

                state = RTSPStateEnum.READY;
                System.out.println("New RTSP state: READY");
            } else if (RTSPRequestEnum.TEARDOWN.equals(req)) {
                sendResponse();

                timer.stop();
                rtcpReceiver.stopRcv();

                RTSPsocket.close();
                rtpSocket.close();

                System.exit(0);
            } else if (RTSPRequestEnum.DESCRIBE.equals(req)) {
                System.out.println("Received DESCRIBE request");
                sendDescribe();
            }
        }
    }

    //------------------------
    //Controls RTP sending rate based on traffic
    //------------------------
    class CongestionController implements ActionListener {
        private Timer ccTimer;
        int interval;   //interval to check traffic stats
        int prevLevel;  //previously sampled congestion level

        public CongestionController(int interval) {
            this.interval = interval;
            ccTimer = new Timer(interval, this);
            ccTimer.start();
        }

        public void actionPerformed(ActionEvent e) {

            //adjust the send rate
            if (prevLevel != congestionLevel) {
                sendDelay = FRAME_PERIOD + congestionLevel * (int) (FRAME_PERIOD * 0.1);
                timer.setDelay(sendDelay);
                prevLevel = congestionLevel;
                System.out.println("Send delay changed to: " + sendDelay);
            }
        }
    }

    //------------------------
    //Listener for RTCP packets sent from app.client
    //------------------------
    class RtcpReceiver implements ActionListener {
        private Timer rtcpTimer;
        private byte[] rtcpBuf;
        int interval;

        public RtcpReceiver(int interval) {
            //set timer with interval for receiving packets
            this.interval = interval;
            rtcpTimer = new Timer(interval, this);
            rtcpTimer.setInitialDelay(0);
            rtcpTimer.setCoalesce(true);

            //allocate buffer for receiving RTCP packets
            rtcpBuf = new byte[512];
        }

        public void actionPerformed(ActionEvent e) {
            //Construct a DatagramPacket to receive data from the UDP socket
            DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
            float fractionLost;

            try {
                rtcpSocket.receive(dp);   // Blocking
                RTCPPacket rtcpPkt = new RTCPPacket(dp.getData(), dp.getLength());
                System.out.println("[RTCP] " + rtcpPkt);

                //set congestion level between 0 to 4
                fractionLost = rtcpPkt.fractionLost;
                if (fractionLost >= 0 && fractionLost <= 0.01) {
                    congestionLevel = 0;    //less than 0.01 assume negligible
                } else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                    congestionLevel = 1;
                } else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                    congestionLevel = 2;
                } else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                    congestionLevel = 3;
                } else {
                    congestionLevel = 4;
                }
            } catch (InterruptedIOException iioe) {
                System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }

        public void startRcv() {
            rtcpTimer.start();
        }

        public void stopRcv() {
            rtcpTimer.stop();
        }
    }

    //------------------------------------
    //Translate an image to different encoding or quality
    //------------------------------------
    class ImageTranslator {

        private float compressionQuality;
        private ByteArrayOutputStream baos;
        private BufferedImage image;
        private Iterator<ImageWriter> writers;
        private ImageWriter writer;
        private ImageWriteParam param;
        private ImageOutputStream ios;

        public ImageTranslator(float cq) {
            compressionQuality = cq;

            try {
                baos = new ByteArrayOutputStream();
                ios = ImageIO.createImageOutputStream(baos);

                writers = ImageIO.getImageWritersByFormatName("jpeg");
                writer = (ImageWriter) writers.next();
                writer.setOutput(ios);

                param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(compressionQuality);

            } catch (Exception ex) {
                System.out.println("Exception caught: " + ex);
                System.exit(0);
            }
        }

        public byte[] compress(byte[] imageBytes) {
            try {
                baos.reset();
                image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                writer.write(null, new IIOImage(image, null, null), param);
            } catch (Exception ex) {
                System.out.println("Exception caught: " + ex);
                System.exit(0);
            }
            return baos.toByteArray();
        }

        public void setCompressionQuality(float cq) {
            compressionQuality = cq;
            param.setCompressionQuality(compressionQuality);
        }
    }

    private RTSPRequestEnum parseRequest() {
        RTSPRequestEnum request = null;
        try {
            String requestLine = rtspBufferedReader.readLine();
            System.out.println("RTSP app.server.Server - Received from app.client.Client:");
            System.out.println(requestLine);

            StringTokenizer tokens = new StringTokenizer(requestLine);
            String requestTypeString = tokens.nextToken();

            request = RTSPRequestEnum.valueOf(requestTypeString);
            if (request == RTSPRequestEnum.SETUP) {
                videoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String seqNumLine = rtspBufferedReader.readLine();
            System.out.println(seqNumLine);
            tokens = new StringTokenizer(seqNumLine);
            tokens.nextToken();
            rtspSeqNb = Integer.parseInt(tokens.nextToken());

            //get LastLine
            String lastLine = rtspBufferedReader.readLine();
            System.out.println(lastLine);

            tokens = new StringTokenizer(lastLine);
            if (request == RTSPRequestEnum.SETUP) {
                //extract RTP_dest_port from LastLine
                for (int i = 0; i < 3; i++)
                    tokens.nextToken(); //skip unused stuff
                rtpDestPort = Integer.parseInt(tokens.nextToken());
            } else if (request == RTSPRequestEnum.DESCRIBE) {
                tokens.nextToken();
                String describeDataType = tokens.nextToken();
            } else {
                //otherwise LastLine will be the SessionId line
                tokens.nextToken(); //skip Session:
                rtspId = tokens.nextToken();
            }
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }

        return request;
    }

    // Creates a DESCRIBE response string in SDP format for current media
    private String describe() {
        StringWriter writer1 = new StringWriter();
        StringWriter writer2 = new StringWriter();

        // Write the body first so we can get the size later
        writer2.write("v=0" + CRLF);
        writer2.write("m=video " + RTSP_dest_port + " RTP/AVP " + MJPEG_TYPE + CRLF);
        writer2.write("a=control:streamid=" + rtspId + CRLF);
        writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
        String body = writer2.toString();

        writer1.write("Content-Base: " + videoFileName + CRLF);
        writer1.write("Content-Type: " + "application/sdp" + CRLF);
        writer1.write("Content-Length: " + body.length() + CRLF);
        writer1.write(body);

        return writer1.toString();
    }

    private void sendResponse() {
        try {
            rtspBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
            rtspBufferedWriter.write("CSeq: " + rtspSeqNb + CRLF);
            rtspBufferedWriter.write("Session: " + rtspId + CRLF);
            rtspBufferedWriter.flush();
            System.out.println("RTSP app.server.Server - Sent response to app.client.Client.");
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }

    private void sendDescribe() {
        String des = describe();
        try {
            rtspBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
            rtspBufferedWriter.write("CSeq: " + rtspSeqNb + CRLF);
            rtspBufferedWriter.write(des);
            rtspBufferedWriter.flush();
            System.out.println("RTSP app.server.Server - Sent response to app.client.Client.");
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }
}
