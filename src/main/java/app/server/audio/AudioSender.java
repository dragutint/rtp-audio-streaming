package app.server.audio;

import app.server.comm.ServerRtpController;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;

@Log4j2
@Data
public class AudioSender extends Thread {
    private ServerRtpController ctx;
    private Timer timer;
    private int sendDelay;
    public static final int FRAME_PERIOD = 150; // Frame period of the audio to stream, in ms
    private Integer packetNumber = 0;
    private Integer readBytesLength = 0;

    public AudioSender(ServerRtpController ctx){
        this.ctx = ctx;
        sendDelay = FRAME_PERIOD;
        timer = new Timer(sendDelay, new AudioSenderListener(this, packetNumber, readBytesLength, ctx));
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
    }

    @SneakyThrows
    @Override
    public void run() {
        timer.start();
    }

    public void stopTimer(){
        this.timer.stop();
    }

    public void pause() {
        this.timer.stop();
    }

    public void play() {
        this.timer.start();
    }
}