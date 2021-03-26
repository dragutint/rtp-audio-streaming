package app.client;

import app.common.RTSPRequestEnum;
import app.common.RTSPStateEnum;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;

public class RTSPController {
    public static int RTP_RCV_PORT = 25000;

    private final InetAddress serverIPAddress;

    private final BufferedReader rtspBufferedReader;
    private final BufferedWriter rtspBufferedWriter;
    private final String videoFilePath;
    private String rtspSessionID;
    private int rtspSequence = 0;

    final String CRLF = "\r\n";

    public RTSPController() throws IOException {
        videoFilePath = "movie.Mjpeg";

        int RTSP_server_port = 1051;
        String ServerHost = "localhost";
        serverIPAddress = InetAddress.getByName(ServerHost);

        Socket rtspSocket = new Socket(serverIPAddress, RTSP_server_port);

        rtspBufferedReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
        rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));
    }

    public void sendRequest(RTSPRequestEnum request) {
        try {
            this.rtspSequence++;

            rtspBufferedWriter.write(request + " " + videoFilePath + " RTSP/1.0" + CRLF);
            rtspBufferedWriter.write("CSeq: " + rtspSequence + CRLF);

            if (request == RTSPRequestEnum.SETUP) {
                rtspBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
            } else if (request == RTSPRequestEnum.DESCRIBE) {
                rtspBufferedWriter.write("Accept: application/sdp" + CRLF);
            } else {
                rtspBufferedWriter.write("Session: " + rtspSessionID + CRLF);
            }

            rtspBufferedWriter.flush();
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }

    public int parseServerResponse(RTSPStateEnum state) {
        int replyCode = 0;

        try {
            String statusLine = rtspBufferedReader.readLine();
            System.out.println("RTSP app.client.Client - Received from app.server.Server:");
            System.out.println(statusLine);

            StringTokenizer tokens = new StringTokenizer(statusLine);
            tokens.nextToken();
            replyCode = Integer.parseInt(tokens.nextToken());

            if (replyCode == 200) {
                String sequenceNumberLine = rtspBufferedReader.readLine();
                System.out.println(sequenceNumberLine);

                String sessionLine = rtspBufferedReader.readLine();
                System.out.println(sessionLine);

                tokens = new StringTokenizer(sessionLine);
                String temp = tokens.nextToken();


                if (state == RTSPStateEnum.INIT && temp.compareTo("Session:") == 0) {
                    rtspSessionID = tokens.nextToken();
                } else if (temp.compareTo("Content-Base:") == 0) {

                    String newLine;
                    for (int i = 0; i < 6; i++) {
                        newLine = rtspBufferedReader.readLine();
                        System.out.println(newLine);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }

        return (replyCode);
    }

    public void setRtspSequence(int number) {
        this.rtspSequence = number;
    }

    public InetAddress getServerIPAddress() {
        return serverIPAddress;
    }
}
