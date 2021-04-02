package app.server.audio;

import app.common.Constants;
import app.common.RTPPacket;
import app.server.comm.RtpController;
import lombok.Data;
import lombok.SneakyThrows;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;

@Data
public class AudioSender extends Thread {
    private RtpController ctx;

    public AudioSender(RtpController ctx){
        this.ctx = ctx;
    }

    @SneakyThrows
    @Override
    public void run() {
        byte[] tempBuffer = new byte[20000];

        while(true) {
            byte[] audioData = AudioStorage.getInstance().getBuffer();
            InputStream byteInputStream = new ByteArrayInputStream(audioData);
            AudioFormat audioFormat = Constants.getAudioFormat();
            AudioInputStream inputStream = new AudioInputStream(byteInputStream, audioFormat, audioData.length / audioFormat.getFrameSize());

            int cnt;
            while ((cnt = inputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
                if (cnt > 0) {
                    RTPPacket rtpPacket = new RTPPacket(ctx.getAudioType(), 0, 0, tempBuffer, cnt);
                    DatagramPacket sendPacket = new DatagramPacket(rtpPacket.getPacket(), rtpPacket.getLength(), ctx.getClientIPAddr(), ctx.getRtpDestPort());
                    ctx.getRtpSocket().send(sendPacket);
                }
            }
        }
    }
}