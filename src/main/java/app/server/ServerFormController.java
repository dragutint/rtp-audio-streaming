package app.server;

import app.server.audio.AudioPlayer;
import app.server.audio.AudioRecorder;
import app.server.thread.ServerThread;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.awt.event.*;

@Log4j2
public class ServerFormController {
    private final AudioRecorder audioRecorder;
    private final AudioPlayer audioPlayer;

    @SneakyThrows
    public ServerFormController() {
        ServerForm form = new ServerForm();
        form.initListenerStartStreamButton(new StartStreamActionListener());
        form.initListenerStopStreamButton(new StopStreamActionListener());
        form.initListenerPlayStreamButton(new PlayStreamActionListener());

        form.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        audioRecorder = new AudioRecorder();
        audioPlayer = new AudioPlayer();
    }

    public void startListening() throws Exception {
        new ServerThread().start();
    }

    class StartStreamActionListener implements ActionListener {
        @SneakyThrows
        public void actionPerformed(ActionEvent e) {
            audioRecorder.start();
        }
    }

    class StopStreamActionListener implements ActionListener {
        @SneakyThrows
        public void actionPerformed(ActionEvent e) {
            audioRecorder.stopRecording();
        }
    }

    class PlayStreamActionListener implements ActionListener {
        @SneakyThrows
        public void actionPerformed(ActionEvent e) {
            audioPlayer.playAudio();
        }
    }
}
