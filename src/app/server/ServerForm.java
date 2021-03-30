package app.server;

import javax.swing.*;
import java.awt.*;

public class ServerForm extends JFrame {
    JLabel label;

    public ServerForm(){
        super("RTSP app.server.Server");

        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);

        this.pack();
        this.setVisible(true);
        this.setSize(new Dimension(400, 200));
    }

    public void setText(String s) {
        label.setText(s);
    }
}
