package common;

import java.util.Arrays;

public class RTPPacket {
    static int HEADER_SIZE = 12;

    // RTP header fields
    private int version;
    private int padding;
    private int extension;
    private int cc;
    private int marker;
    private int payloadType;
    private int sequenceNumber;
    private int timeStamp;
    private int ssrc; // jedinstven na nivou sesije

    public byte[] header;

    public int payloadSize;
    public byte[] payload;

    public RTPPacket(int payloadType, int sequenceNumber, int timestamp, byte[] data, int dataLength) {
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        ssrc = 1337;

        this.sequenceNumber = sequenceNumber;
        this.timeStamp = timestamp;
        this.payloadType = payloadType;

        header = new byte[HEADER_SIZE];

        header[0] = (byte) (version << 6 | padding << 5 | extension << 4 | cc);
        header[1] = (byte) (marker << 7 | this.payloadType & 0x000000FF);
        header[2] = (byte) (this.sequenceNumber >> 8);
        header[3] = (byte) (this.sequenceNumber & 0xFF);
        header[4] = (byte) (timeStamp >> 24);
        header[5] = (byte) (timeStamp >> 16);
        header[6] = (byte) (timeStamp >> 8);
        header[7] = (byte) (timeStamp & 0xFF);
        header[8] = (byte) (ssrc >> 24);
        header[9] = (byte) (ssrc >> 16);
        header[10] = (byte) (ssrc >> 8);
        header[11] = (byte) (ssrc & 0xFF);

        payloadSize = dataLength;
        payload = new byte[dataLength];

        payload = Arrays.copyOf(data, payloadSize);
    }

    public RTPPacket(byte[] packet, int packetSize) {
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        ssrc = 0;

        if (packetSize >= HEADER_SIZE) {
            header = new byte[HEADER_SIZE];
            for (int i = 0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            payloadSize = packetSize - HEADER_SIZE;
            payload = new byte[payloadSize];
            for (int i = HEADER_SIZE; i < packetSize; i++)
                payload[i - HEADER_SIZE] = packet[i];

            version = (header[0] & 0xFF) >>> 6;
            payloadType = header[1] & 0x7F;
            sequenceNumber = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
            timeStamp = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);
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
        return (timeStamp);
    }

    public int getSequenceNumber() {
        return (sequenceNumber);
    }

    public int getPayloadType() {
        return (payloadType);
    }

    public void printHeader() {
        System.out.print("[RTP-Header] ");
        System.out.println("Version: " + version
                + ", Padding: " + padding
                + ", Extension: " + extension
                + ", CC: " + cc
                + ", Marker: " + marker
                + ", PayloadType: " + payloadType
                + ", SequenceNumber: " + sequenceNumber
                + ", TimeStamp: " + timeStamp);
    }
}