package app.client;

import java.awt.*;
import java.util.ArrayDeque;

public class FrameSynchronizer {

    private final ArrayDeque<Image> queue;
    private int curSeqNb;
    private Image lastImage;

    public FrameSynchronizer(int bsize) {
        curSeqNb = 1;
        queue = new ArrayDeque<>(bsize);
    }

    public void addFrame(Image image, int seqNum) {
        if (seqNum < curSeqNb) {
            queue.add(lastImage);
        } else if (seqNum > curSeqNb) {
            for (int i = curSeqNb; i < seqNum; i++) {
                queue.add(lastImage);
            }
            queue.add(image);
        } else {
            queue.add(image);
        }
    }

    public Image nextFrame() {
        curSeqNb++;
        lastImage = queue.peekLast();
        return queue.remove();
    }
}