package client;

import common.Constants;
import common.RTSPRequestEnum;
import common.RTSPStateEnum;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;

@Log4j2
public class ClientRtspController {
    public static int RTP_RCV_PORT = 25000;

    private final BufferedReader rtspBufferedReader;
    private final BufferedWriter rtspBufferedWriter;
    private String rtspSessionID;
    private int rtspSequence = 0;

    public ClientRtspController() throws IOException {
        int rtspServerPort = 1051;
        String serverHost = "localhost";
        InetAddress serverIPAddress = InetAddress.getByName(serverHost);

        Socket rtspSocket = new Socket(serverIPAddress, rtspServerPort);

        rtspBufferedReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
        rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));
    }

    public void sendRequest(RTSPRequestEnum request) {
        try {
            this.rtspSequence++;

            rtspBufferedWriter.write(request + " RTSP/1.0" + Constants.CRLF);
            rtspBufferedWriter.write("CSeq: " + rtspSequence + Constants.CRLF);

            if (request == RTSPRequestEnum.SETUP) {
                rtspBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + Constants.CRLF);
            } else {
                rtspBufferedWriter.write("Session: " + rtspSessionID + Constants.CRLF);
            }

            rtspBufferedWriter.flush();
        } catch (Exception ex) {
            log.error("Exception caught: ", ex);
            System.exit(0);
        }
    }

    public int parseServerResponse(RTSPStateEnum state) {
        int replyCode = 0;

        try {
            String statusLine = rtspBufferedReader.readLine();
            log.debug("RTSP client - Received from server:");
            log.debug(statusLine);

            StringTokenizer tokens = new StringTokenizer(statusLine);
            tokens.nextToken();
            replyCode = Integer.parseInt(tokens.nextToken());

            if (replyCode == 200) {
                String sequenceNumberLine = rtspBufferedReader.readLine();
                log.debug(sequenceNumberLine);

                String sessionLine = rtspBufferedReader.readLine();
                log.debug(sessionLine);

                tokens = new StringTokenizer(sessionLine);
                String temp = tokens.nextToken();

                if (state == RTSPStateEnum.INIT && temp.compareTo("Session:") == 0) {
                    rtspSessionID = tokens.nextToken();
                } else if (temp.compareTo("Content-Base:") == 0) {
                    String newLine;
                    for (int i = 0; i < 6; i++) {
                        newLine = rtspBufferedReader.readLine();
                        log.debug(newLine);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Exception: ", ex);
            System.exit(0);
        }

        return (replyCode);
    }

    public void setRtspSequence(int number) {
        this.rtspSequence = number;
    }
}
