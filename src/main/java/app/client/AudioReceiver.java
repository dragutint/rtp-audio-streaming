package app.client;

import app.common.Constants;
import app.common.RTPPacket;
import lombok.extern.log4j.Log4j2;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Log4j2
public class AudioReceiver extends Thread {
    private final DatagramSocket rtpSocket;
    private AudioInputStream inputStream;
    private SourceDataLine sourceLine;

    public AudioReceiver(DatagramSocket rtpSocket){
        this.rtpSocket = rtpSocket;
    }

    @Override
    public void run() {
        try {
            byte[] receiveData = new byte[20000];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                rtpSocket.receive(receivePacket);
                RTPPacket rtpPacket = new RTPPacket(receivePacket.getData(), receivePacket.getLength());

                int seqNb = rtpPacket.getSequenceNumber();
                log.debug("Got RTP packet with SeqNum # " + seqNb
                        + " TimeStamp " + rtpPacket.getTimeStamp() + " ms, of type "
                        + rtpPacket.getPayloadType());
                rtpPacket.printHeader();

                try {
                    byte[] audioData = rtpPacket.getPayload();
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
                    log.error("Exception ", e);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class PlayThread extends Thread {

        byte[] tempBuffer = new byte[10000];

        public void run() {
            try {
                int cnt;
                while ((cnt = inputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
                    if (cnt > 0) {
                        sourceLine.write(tempBuffer, 0, cnt);
                    }
                }
            } catch (Exception e) {
                log.error("Exception :", e);
                System.exit(0);
            }
        }
    }
}