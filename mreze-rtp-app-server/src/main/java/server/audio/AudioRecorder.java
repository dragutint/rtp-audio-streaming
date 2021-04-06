package server.audio;

import common.Constants;
import lombok.extern.log4j.Log4j2;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

@Log4j2
public class AudioRecorder extends Thread {
    private final TargetDataLine targetDataLine;
    private final byte[] tempBuffer = new byte[20000];
    boolean stopRecording = false;

    public AudioRecorder() throws LineUnavailableException {
        AudioFormat adFormat = Constants.getAudioFormat();
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
        targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        targetDataLine.open(adFormat);
        targetDataLine.start();
    }

    @Override
    public void run() {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

        try {
            while (!stopRecording) {
                int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                if (cnt > 0)
                    AudioStorage.getInstance().push(tempBuffer, cnt);
            }
            byteOutputStream.close();
        } catch (Exception e) {
            log.error("Error, ex: ", e);
            System.exit(0);
        }
    }

    public void stopRecording(){
        this.stopRecording = true;
    }
}
