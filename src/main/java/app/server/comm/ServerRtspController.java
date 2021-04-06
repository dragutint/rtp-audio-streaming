package app.server.comm;

import app.common.Constants;
import app.common.RTSPRequestEnum;
import app.common.RTSPStateEnum;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.UUID;

@Log4j2
@Data
public class ServerRtspController {
    private ServerRtpController rtpCtx;
    private Socket rtspSocket;
    private BufferedReader rtspBufferedReader;
    private BufferedWriter rtspBufferedWriter;
    private RTSPStateEnum state;
    private String rtspId = UUID.randomUUID().toString();
    private int rtspSeqNb = 0;

    public ServerRtspController(Socket rtspSocket, ServerRtpController rtpCtx) throws IOException {
        state = RTSPStateEnum.INIT;
        this.rtspSocket = rtspSocket;
        this.rtpCtx = rtpCtx;

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
            log.debug("RTSP app.server.Server - Received from app.client.Client:");
            log.debug(requestLine);

            StringTokenizer tokens = new StringTokenizer(requestLine);
            String requestTypeString = tokens.nextToken();

            request = RTSPRequestEnum.valueOf(requestTypeString);

            //parse the SeqNumLine and extract CSeq field
            String seqNumLine = rtspBufferedReader.readLine();
            log.debug(seqNumLine);
            tokens = new StringTokenizer(seqNumLine);
            tokens.nextToken();
            this.rtspSeqNb = Integer.parseInt(tokens.nextToken());
            //get LastLine
            String lastLine = rtspBufferedReader.readLine();
            log.debug(lastLine);

            tokens = new StringTokenizer(lastLine);
            if (request == RTSPRequestEnum.SETUP) {
                //extract RTP_dest_port from LastLine
                for (int i = 0; i < 3; i++)
                    tokens.nextToken(); //skip unused stuff
                rtpCtx.setRtpDestPort(Integer.parseInt(tokens.nextToken()));
            } else {
                //otherwise LastLine will be the SessionId line
                tokens.nextToken(); //skip Session:
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
            log.debug("RTSP app.server.Server - Sent response to app.client.Client.");
        } catch (Exception ex) {
            log.error("Exception caught: ", ex);
            System.exit(0);
        }
    }
}
