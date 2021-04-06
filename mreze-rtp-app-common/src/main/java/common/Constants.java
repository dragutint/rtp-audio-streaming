package common;

import javax.sound.sampled.AudioFormat;

public class Constants {
    public static final String CRLF = "\r\n";

    public static AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleInBits, channels, signed, bigEndian);
    }
}
