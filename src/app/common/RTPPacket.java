package app.common;

import java.util.*;

public class RTPPacket {
    static int HEADER_SIZE = 12;

    // RTP header fields
    private int Version;
    private int Padding;
    private int Extension;
    private int CC;
    private int Marker;
    private int PayloadType;
    private int SequenceNumber;
    private int TimeStamp;
    private int Ssrc;

    public byte[] header;

    public int payloadSize;
    public byte[] payload;

    public RTPPacket(int PType, int Framenb, int Time, byte[] data, int dataLength) {
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 1337;

        SequenceNumber = Framenb;
        TimeStamp = Time;
        PayloadType = PType;

        header = new byte[HEADER_SIZE];

        header[0] = (byte) (Version << 6 | Padding << 5 | Extension << 4 | CC);
        header[1] = (byte) (Marker << 7 | PayloadType & 0x000000FF);
        header[2] = (byte) (SequenceNumber >> 8);
        header[3] = (byte) (SequenceNumber & 0xFF);
        header[4] = (byte) (TimeStamp >> 24);
        header[5] = (byte) (TimeStamp >> 16);
        header[6] = (byte) (TimeStamp >> 8);
        header[7] = (byte) (TimeStamp & 0xFF);
        header[8] = (byte) (Ssrc >> 24);
        header[9] = (byte) (Ssrc >> 16);
        header[10] = (byte) (Ssrc >> 8);
        header[11] = (byte) (Ssrc & 0xFF);

        payloadSize = dataLength;
        payload = new byte[dataLength];

        payload = Arrays.copyOf(data, payloadSize);
    }

    public RTPPacket(byte[] packet, int packetSize) {
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;

        if (packetSize >= HEADER_SIZE) {
            header = new byte[HEADER_SIZE];
            for (int i = 0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            payloadSize = packetSize - HEADER_SIZE;
            payload = new byte[payloadSize];
            for (int i = HEADER_SIZE; i < packetSize; i++)
                payload[i - HEADER_SIZE] = packet[i];

            Version = (header[0] & 0xFF) >>> 6;
            PayloadType = header[1] & 0x7F;
            SequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            TimeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);
        }
    }

    public byte[] getPayload() {
        byte[] data = new byte[getLength()];

        if (payloadSize >= 0) System.arraycopy(payload, 0, data, 0, payloadSize);

        return data;
    }

    public int getPayloadLength() {
        return (payloadSize);
    }

    public int getLength() {
        return (payloadSize + HEADER_SIZE);
    }

    public byte[] getPacket() {
        byte[] packet = new byte[getLength()];

        if (HEADER_SIZE >= 0) System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        if (payloadSize >= 0) System.arraycopy(payload, 0, packet, HEADER_SIZE, payloadSize);

        return packet;
    }

    public int getTimeStamp() {
        return (TimeStamp);
    }

    public int getSequenceNumber() {
        return (SequenceNumber);
    }

    public int getPayloadType() {
        return (PayloadType);
    }

    public void printHeader() {
        System.out.print("[RTP-Header] ");
        System.out.println("Version: " + Version
                + ", Padding: " + Padding
                + ", Extension: " + Extension
                + ", CC: " + CC
                + ", Marker: " + Marker
                + ", PayloadType: " + PayloadType
                + ", SequenceNumber: " + SequenceNumber
                + ", TimeStamp: " + TimeStamp);

    }
}