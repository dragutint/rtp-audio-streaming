package app.server.comm;

import lombok.Data;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

@Data
public class RtpController {
    private DatagramSocket rtpSocket;
    private InetAddress clientIPAddr;
    private int rtpDestPort;
    private int audioType;

    public RtpController(InetAddress clientIPAddr) throws SocketException {
        this.clientIPAddr = clientIPAddr;
        this.audioType = 26;
        rtpSocket = new DatagramSocket();
//        rtpSocket.setSoTimeout(50);
    }

    public void closeSocket() {
        rtpSocket.close();
    }
}
