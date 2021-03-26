package app.common;

import java.nio.*;

public class RTCPPacket {

    final static int HEADER_SIZE = 8;
    final static int BODY_SIZE = 24;

    public int Version;            // Version number 2
    public int Padding;            // Padding of packet
    public int RC;                // Reception report count = 1 for one receiver
    public int PayloadType;        // 201 for Receiver Report
    public int length;            // 1 source is always 32 bytes: 8 header, 24 body
    public int Ssrc;            // Ssrc of sender
    public float fractionLost;    // The fraction of RTP data packets from sender lost since the previous RR packet was sent
    public int cumLost;            // The total number of RTP data packets from sender that have been lost since the beginning of reception.
    public int highSeqNb;        // Highest sequence number received

    public byte[] header;
    public byte[] body;

    public RTCPPacket(float fractionLost, int cumLost, int highSeqNb) {
        Version = 2;
        Padding = 0;
        RC = 1;
        PayloadType = 201;
        length = 32;

        this.fractionLost = fractionLost;
        this.cumLost = cumLost;
        this.highSeqNb = highSeqNb;

        header = new byte[HEADER_SIZE];
        body = new byte[BODY_SIZE];

        header[0] = (byte) (Version << 6 | Padding << 5 | RC);
        header[1] = (byte) (PayloadType & 0xFF);
        header[2] = (byte) (length >> 8);
        header[3] = (byte) (length & 0xFF);
        header[4] = (byte) (Ssrc >> 24);
        header[5] = (byte) (Ssrc >> 16);
        header[6] = (byte) (Ssrc >> 8);
        header[7] = (byte) (Ssrc & 0xFF);

        ByteBuffer bb = ByteBuffer.wrap(body);
        bb.putFloat(fractionLost);
        bb.putInt(cumLost);
        bb.putInt(highSeqNb);
    }

    public RTCPPacket(byte[] packet, int packetSize) {
        header = new byte[HEADER_SIZE];
        body = new byte[BODY_SIZE];

        System.arraycopy(packet, 0, header, 0, HEADER_SIZE);
        System.arraycopy(packet, HEADER_SIZE, body, 0, BODY_SIZE);

        Version = (header[0] & 0xFF) >> 6;
        PayloadType = header[1] & 0xFF;
        length = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
        Ssrc = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);

        ByteBuffer bb = ByteBuffer.wrap(body);
        fractionLost = bb.getFloat();
        cumLost = bb.getInt();
        highSeqNb = bb.getInt();
    }


    public byte[] getPacket() {
        byte[] packet = new byte[getLength()];
        System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        System.arraycopy(body, 0, packet, HEADER_SIZE, BODY_SIZE);
        return packet;
    }

    public int getLength() {
        return (BODY_SIZE + HEADER_SIZE);
    }

    public String toString() {
        return "[RTCP] Version: " + Version + ", Fraction Lost: " + fractionLost
                + ", Cumulative Lost: " + cumLost + ", Highest Seq Num: " + highSeqNb;
    }
}