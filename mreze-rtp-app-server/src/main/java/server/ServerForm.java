package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ServerForm extends JFrame {
    private final JButton startStreamButton = new JButton("STREAM");
    private final JButton stopStreamButton = new JButton("STOP");
    private final JButton playStreamButton = new JButton("PLAY");

    public ServerForm(){
        super("RTSP app.server.Server");

        this.pack();
        this.setVisible(true);
        this.setSize(new Dimension(400, 200));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 3));
        buttonPanel.add(startStreamButton);
        buttonPanel.add(stopStreamButton);
        buttonPanel.add(playStreamButton);
        buttonPanel.setBounds(0, 280, 380, 50);

        this.getContentPane().add(buttonPanel, BorderLayout.CENTER);
    }

    public void initListenerStartStreamButton(ActionListener listener) {
        this.startStreamButton.addActionListener(listener);
    }

    public void initListenerStopStreamButton(ActionListener listener) {
        this.stopStreamButton.addActionListener(listener);
    }

    public void initListenerPlayStreamButton(ActionListener listener) {
        this.playStreamButton.addActionListener(listener);
    }
}
