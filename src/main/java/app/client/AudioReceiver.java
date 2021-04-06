package app.client;

import app.common.Constants;
import app.common.RTPPacket;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Log4j2
public class AudioReceiver extends Thread {
    private final DatagramSocket rtpSocket;
    private SourceDataLine sourceLine;
    private final Timer timer;

    public AudioReceiver(DatagramSocket rtpSocket){
        this.rtpSocket = rtpSocket;
        timer = new Timer(150, new AudioReceiverListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
    }

    @SneakyThrows
    @Override
    public void run() {
        AudioFormat adFormat = Constants.getAudioFormat();
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, adFormat);

        sourceLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        sourceLine.open(adFormat);
        sourceLine.start();

        timer.start();
    }

    public void pause() {
        timer.stop();
    }

    public void play() {
        timer.start();
    }

    private class AudioReceiverListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            try {
                RTPPacket rtpPacket = getRtpPacket();
                playAudio(rtpPacket);
            } catch (Exception ex) {
                log.error("Exception, ex: ", ex);
                System.exit(0);
            }
        }

        private RTPPacket getRtpPacket() throws IOException {
            byte[] receiveData = new byte[5000];

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            rtpSocket.receive(receivePacket);
            RTPPacket rtpPacket = new RTPPacket(receivePacket.getData(), receivePacket.getLength());

            int seqNb = rtpPacket.getSequenceNumber();
            log.debug("Got RTP packet with SeqNum # " + seqNb + " TimeStamp " + rtpPacket.getTimeStamp() + " ms, of type " + rtpPacket.getPayloadType());
            rtpPacket.printHeader();
            return rtpPacket;
        }

        private void playAudio(RTPPacket rtpPacket) throws IOException {
            byte[] audioData = rtpPacket.getPayload();
            InputStream byteInputStream = new ByteArrayInputStream(audioData);
            AudioFormat adFormat = Constants.getAudioFormat();
            AudioInputStream inputStream = new AudioInputStream(byteInputStream, adFormat, audioData.length / adFormat.getFrameSize());

            byte[] tempBuffer = new byte[2000];
            int cnt;
            while ((cnt = inputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
                if (cnt > 0)
                    sourceLine.write(tempBuffer, 0, cnt);
            }
        }
    }
}