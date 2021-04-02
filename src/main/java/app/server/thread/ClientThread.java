package app.server.thread;

import app.common.RTSPRequestEnum;
import app.common.RTSPStateEnum;
import app.server.comm.RtpController;
import app.server.comm.ServerRtspController;
import app.server.audio.AudioSender;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.Socket;

@Log4j2
public class ClientThread extends Thread {

    private final RtpController rtpCtx;
    private final ServerRtspController rtspCtx;

    public ClientThread(Socket rtspSocket) throws IOException {
        this.rtpCtx = new RtpController(rtspSocket.getInetAddress());
        this.rtspCtx = new ServerRtspController(rtspSocket, rtpCtx);
    }

    @SneakyThrows
    @Override
    public void run() {
        boolean done = false;

        while (!done) {
            RTSPRequestEnum req = rtspCtx.parseRequest();

            if (RTSPRequestEnum.SETUP.equals(req)) {
                done = true;

                rtspCtx.setState(RTSPStateEnum.READY);
                log.debug("New RTSP state: READY");

                rtspCtx.sendResponse();
            }
        }

        while (true) {
            RTSPRequestEnum req = rtspCtx.parseRequest();

            if (RTSPRequestEnum.PLAY.equals(req) && RTSPStateEnum.READY.equals(rtspCtx.getState())) {
                rtspCtx.sendResponse();

                new AudioSender(rtpCtx).start();

                rtspCtx.setState(RTSPStateEnum.PLAYING);
                log.debug("New RTSP state: PLAYING");
            } else if (RTSPRequestEnum.PAUSE.equals(req) && RTSPStateEnum.PLAYING.equals(rtspCtx.getState())) {
                rtspCtx.sendResponse();

                rtspCtx.setState(RTSPStateEnum.READY);
                log.debug("New RTSP state: READY");
            } else if (RTSPRequestEnum.TEARDOWN.equals(req)) {
                rtspCtx.sendResponse();

                rtspCtx.closeSocket();
                rtpCtx.closeSocket();

                System.exit(0);
            } else if (RTSPRequestEnum.DESCRIBE.equals(req)) {
                log.debug("Received DESCRIBE request");
            }
        }
    }
}
