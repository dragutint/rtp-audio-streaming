package server.comm;

import common.Constants;
import common.RTSPRequestEnum;
import common.RTSPStateEnum;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.UUID;

@Log4j2
@Data
public class ServerRtspController {
    private ServerRtpController rtpController;
    private Socket rtspSocket;
    private BufferedReader rtspBufferedReader;
    private BufferedWriter rtspBufferedWriter;
    private RTSPStateEnum state;
    private String rtspId = UUID.randomUUID().toString();
    private int rtspSeqNb = 0;

    public ServerRtspController(Socket rtspSocket, ServerRtpController rtpController) throws IOException {
        state = RTSPStateEnum.INIT;
        this.rtspSocket = rtspSocket;
        this.rtpController = rtpController;

        this.rtspBufferedReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
        this.rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));
    }

    public void closeSocket() throws IOException {
        rtspSocket.close();
    }

    public RTSPRequestEnum parseRequest() {
        RTSPRequestEnum request = null;
        try {
            String requestLine = rtspBufferedReader.readLine();
            log.debug("RTSP Server - Received from client:");
            log.debug(requestLine);

            StringTokenizer tokens = new StringTokenizer(requestLine);
            String requestTypeString = tokens.nextToken();

            request = RTSPRequestEnum.valueOf(requestTypeString);

            String seqNumLine = rtspBufferedReader.readLine();
            log.debug(seqNumLine);
            tokens = new StringTokenizer(seqNumLine);
            tokens.nextToken();
            this.rtspSeqNb = Integer.parseInt(tokens.nextToken());
            String lastLine = rtspBufferedReader.readLine();
            log.debug(lastLine);

            tokens = new StringTokenizer(lastLine);
            if (request == RTSPRequestEnum.SETUP) {
                for (int i = 0; i < 3; i++)
                    tokens.nextToken();
                rtpController.setRtpDestPort(Integer.parseInt(tokens.nextToken()));
            } else {
                tokens.nextToken();
                rtspId = tokens.nextToken();
            }
        } catch (Exception ex) {
            log.error("Exception: ", ex);
            System.exit(0);
        }

        return request;
    }


    public void sendResponse() {
        try {
            rtspBufferedWriter.write("RTSP/1.0 200 OK" + Constants.CRLF);
            rtspBufferedWriter.write("CSeq: " + rtspSeqNb + Constants.CRLF);
            rtspBufferedWriter.write("Session: " + rtspId + Constants.CRLF);
            rtspBufferedWriter.flush();
            log.debug("RTSP server - sent response to client");
        } catch (Exception ex) {
            log.error("Exception caught: ", ex);
            System.exit(0);
        }
    }
}
