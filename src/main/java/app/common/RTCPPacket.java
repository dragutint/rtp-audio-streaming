package app.common;

import java.nio.*;

public class RTCPPacket {

    final static int HEADER_SIZE = 8;
    final static int BODY_SIZE = 24;

    public int version;            // Version number 2
    public int padding;            // Padding of packet
    public int rc;                // Reception report count = 1 for one receiver
    public int payloadType;        // 201 for Receiver Report
    public int length;            // 1 source is always 32 bytes: 8 header, 24 body
    public int ssrc;            // Ssrc of sender
    public float fractionLost;    // The fraction of RTP data packets from sender lost since the previous RR packet was sent
    public int cumLost;            // The total number of RTP data packets from sender that have been lost since the beginning of reception.
    public int highSeqNb;        // Highest sequence number received

    public byte[] header;
    public byte[] body;

    public RTCPPacket(float fractionLost, int cumLost, int highSeqNb) {
        version = 2;
        padding = 0;
        rc = 1;
        payloadType = 201;
        length = 32;

        this.fractionLost = fractionLost;
        this.cumLost = cumLost;
        this.highSeqNb = highSeqNb;

        header = new byte[HEADER_SIZE];
        body = new byte[BODY_SIZE];

        header[0] = (byte) (version << 6 | padding << 5 | rc);
        header[1] = (byte) (payloadType & 0xFF);
        header[2] = (byte) (length >> 8);
        header[3] = (byte) (length & 0xFF);
        header[4] = (byte) (ssrc >> 24);
        header[5] = (byte) (ssrc >> 16);
        header[6] = (byte) (ssrc >> 8);
        header[7] = (byte) (ssrc & 0xFF);

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

        version = (header[0] & 0xFF) >> 6;
        payloadType = header[1] & 0xFF;
        length = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
        ssrc = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);

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
        return "[RTCP] Version: " + version + ", Fraction Lost: " + fractionLost
                + ", Cumulative Lost: " + cumLost + ", Highest Seq Num: " + highSeqNb;
    }
}