package client;


import common.RTSPRequestEnum;
import common.RTSPStateEnum;
import lombok.extern.log4j.Log4j2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;

@Log4j2
public class ClientFormController {

    private final ClientRtspController rtspController;
    private DatagramSocket rtpSocket;
    private RTSPStateEnum state;
    private ClientRtpController rtpController = null;

    public ClientFormController() throws IOException {
        ClientForm form = new ClientForm();
        rtspController = new ClientRtspController();

        form.initListenerSetupButton(new SetupButtonListener());
        form.initListenerPlayButton(new PlayButtonListener());
        form.initListenerPauseButton(new PauseButtonListener());
        form.initListenerTeardownButton(new TearButtonListener());

        state = RTSPStateEnum.INIT;
    }

    private class SetupButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            log.debug("Setup Button pressed !");

            if (state == RTSPStateEnum.INIT) {
                try {
                    rtpSocket = new DatagramSocket(ClientRtspController.RTP_RCV_PORT);
                } catch (SocketException se) {
                    log.error("Socket exception: ", se);
                    System.exit(0);
                }

                rtspController.setRtspSequence(1);
                rtspController.sendRequest(RTSPRequestEnum.SETUP);

                if (rtspController.parseServerResponse(state) != 200)
                    log.debug("Invalid app.server.Server Response");
                else {
                    state = RTSPStateEnum.READY;
                    log.debug("New RTSP state: READY");
                }
            }
        }
    }

    private class PlayButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            log.debug("Play Button pressed!");

            if (state == RTSPStateEnum.READY) {
                rtspController.sendRequest(RTSPRequestEnum.PLAY);

                if (rtspController.parseServerResponse(state) != 200)
                    log.debug("Invalid app.server.Server Response");
                else {
                    if(rtpController == null) {
                        rtpController = new ClientRtpController(rtpSocket);
                        rtpController.start();
                    } else
                        rtpController.play();
                    state = RTSPStateEnum.PLAYING;
                    log.debug("New RTSP state: PLAYING");
                }
            }
        }
    }

    private class PauseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            log.debug("Pause Button pressed!");

            if (state == RTSPStateEnum.PLAYING) {
                rtspController.sendRequest(RTSPRequestEnum.PAUSE);

                if (rtspController.parseServerResponse(state) != 200)
                    log.debug("Invalid app.server.Server Response");
                else {
                    state = RTSPStateEnum.READY;
                    rtpController.pause();
                    log.debug("New RTSP state: READY");
                }
            }
        }
    }

    private class TearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            log.debug("Teardown Button pressed !");

            rtspController.sendRequest(RTSPRequestEnum.TEARDOWN);

            if (rtspController.parseServerResponse(state) != 200)
                log.debug("Invalid app.server.Server Response");
            else {
                state = RTSPStateEnum.INIT;
                log.debug("New RTSP state: INIT");

                System.exit(0);
            }
        }
    }
}
