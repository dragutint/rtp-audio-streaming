package server.audio;

import common.Constants;
import lombok.extern.log4j.Log4j2;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Log4j2
public class AudioPlayer {
    private AudioInputStream inputStream;
    private SourceDataLine sourceLine;

    public void playAudio() {
        try {
            byte[] audioData = AudioStorage.getInstance().getBuffer();
            InputStream byteInputStream = new ByteArrayInputStream(audioData);
            AudioFormat adFormat = Constants.getAudioFormat();
            inputStream = new AudioInputStream(byteInputStream, adFormat, audioData.length / adFormat.getFrameSize());

            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, adFormat);
            sourceLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceLine.open(adFormat);
            sourceLine.start();
            Thread playThread = new Thread(new PlayThread());
            playThread.start();
        } catch (Exception e) {
            log.error("Exception, ex: ", e);
            System.exit(0);
        }
    }

    class PlayThread extends Thread {
        byte[] tempBuffer = new byte[10000];

        public void run() {
            try {
                int cnt;
                while ((cnt = inputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
                    if (cnt > 0)
                        sourceLine.write(tempBuffer, 0, cnt);
                }
            } catch (Exception e) {
                log.error("Exception, e: ", e);
                System.exit(0);
            }
        }
    }
}
