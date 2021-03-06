package server.audio;

import common.Constants;
import common.RTPPacket;
import lombok.extern.log4j.Log4j2;
import server.comm.ServerRtpController;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;

@Log4j2
public class AudioSenderListener implements ActionListener {
    private Integer packetNumber;
    private Integer readBytesLength;
    private final ServerRtpController ctx;
    private final AudioSender audioSender;

    public AudioSenderListener(AudioSender audioSender, Integer packetNumber, Integer readBytesLength, ServerRtpController ctx) {
        this.packetNumber = packetNumber;
        this.readBytesLength = readBytesLength;
        this.ctx = ctx;
        this.audioSender = audioSender;
    }

    public void actionPerformed(ActionEvent e) {
        AudioFormat adFormat = Constants.getAudioFormat();

        byte[] frame = new byte[10000];

        packetNumber++;

        try {
            byte[] audioData = AudioStorage.getInstance().getBuffer();
            InputStream byteInputStream = new ByteArrayInputStream(audioData);
            AudioInputStream inputStream = new AudioInputStream(byteInputStream, adFormat, audioData.length / adFormat.getFrameSize());

            if(readBytesLength != 0)
                inputStream.skip(readBytesLength / adFormat.getFrameSize());

            int frameLength = inputStream.read(frame, 0, frame.length);
            readBytesLength += frameLength;

            if(frameLength == -1) {
                audioSender.stopTimer();
                return;
            }

            RTPPacket rtpPacket = new RTPPacket(ctx.getAudioType(), packetNumber, packetNumber * AudioSender.FRAME_PERIOD, frame, frameLength);

            int packetLength = rtpPacket.getLength();
            byte[] packetByteArray = rtpPacket.getPacket();

            DatagramPacket packetForSending = new DatagramPacket(packetByteArray, packetLength, ctx.getClientIPAddr(), ctx.getRtpDestPort());

            if(!ctx.getRtpSocket().isClosed())
                ctx.getRtpSocket().send(packetForSending);
            else
                return;

            log.debug("Send frame #" + packetNumber + ", Frame size: " + frameLength + " (" + frame.length + ")");
            rtpPacket.printHeader();
        } catch (Exception ex) {
            log.error("Exception caught: ", ex);
            System.exit(0);
        }
    }

}
