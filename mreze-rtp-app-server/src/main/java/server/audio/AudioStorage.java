package server.audio;

import java.io.ByteArrayOutputStream;

public class AudioStorage {
    private static AudioStorage instance;
    private final ByteArrayOutputStream buffer;

    private AudioStorage(){
        buffer = new ByteArrayOutputStream();
    }

    public static AudioStorage getInstance() {
        if(instance == null)
            instance = new AudioStorage();
        return instance;
    }

    public void push(byte[] data, int length) {
        buffer.write(data, 0, length);
    }

    public byte[] getBuffer() {
        return buffer.toByteArray();
    }
}
