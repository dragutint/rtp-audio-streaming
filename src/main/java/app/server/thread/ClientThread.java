package app.server.thread;

import app.common.RTSPRequestEnum;
import app.common.RTSPStateEnum;
import app.server.comm.ServerRtpController;
import app.server.comm.ServerRtspController;
import app.server.audio.AudioSender;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.Socket;

@Log4j2
public class ClientThread extends Thread {

    private final ServerRtpController rtpController;
    private final ServerRtspController rtspController;
    private AudioSender audioSender = null;

    public ClientThread(Socket rtspSocket) throws IOException {
        this.rtpController = new ServerRtpController(rtspSocket.getInetAddress());
        this.rtspController = new ServerRtspController(rtspSocket, rtpController);
    }

    @SneakyThrows
    @Override
    public void run() {
        boolean done = false;

        while (!done) {
            RTSPRequestEnum req = rtspController.parseRequest();

            if (RTSPRequestEnum.SETUP.equals(req)) {
                done = true;

                rtspController.setState(RTSPStateEnum.READY);
                log.debug("New RTSP state: READY");

                rtspController.sendResponse();
            }
        }

        while (true) {
            RTSPRequestEnum req = rtspController.parseRequest();

            if (RTSPRequestEnum.PLAY.equals(req) && RTSPStateEnum.READY.equals(rtspController.getState())) {
                rtspController.sendResponse();

                if(audioSender == null) {
                    audioSender = new AudioSender(rtpController);
                    audioSender.start();
                } else
                    audioSender.play();

                rtspController.setState(RTSPStateEnum.PLAYING);
                log.debug("New RTSP state: PLAYING");
            } else if (RTSPRequestEnum.PAUSE.equals(req) && RTSPStateEnum.PLAYING.equals(rtspController.getState())) {
                rtspController.sendResponse();

                audioSender.pause();

                rtspController.setState(RTSPStateEnum.READY);
                log.debug("New RTSP state: READY");
            } else if (RTSPRequestEnum.TEARDOWN.equals(req)) {
                rtspController.sendResponse();

                rtspController.closeSocket();
                rtpController.closeSocket();

                System.exit(0);
            } else if (RTSPRequestEnum.DESCRIBE.equals(req)) {
                log.debug("Received DESCRIBE request");
            }
        }
    }
}