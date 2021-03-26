package app.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientForm extends JFrame {
    private final JButton setupButton = new JButton("Setup");
    private final JButton playButton = new JButton("Play");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton tearButton = new JButton("Close");
    private final JButton describeButton = new JButton("Session");
    private final JLabel statLabel1 = new JLabel();
    private final JLabel statLabel2 = new JLabel();
    private final JLabel statLabel3 = new JLabel();
    private final JLabel iconLabel = new JLabel();

    public ClientForm() {
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
        buttonPanel.add(describeButton);

        statLabel1.setText("Total Bytes Received: 0");
        statLabel2.setText("Packets Lost: 0");
        statLabel3.setText("Data Rate (bytes/sec): 0");

        iconLabel.setIcon(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        mainPanel.add(statLabel1);
        mainPanel.add(statLabel2);
        mainPanel.add(statLabel3);
        iconLabel.setBounds(0, 0, 380, 280);
        buttonPanel.setBounds(0, 280, 380, 50);
        statLabel1.setBounds(0, 330, 380, 20);
        statLabel2.setBounds(0, 350, 380, 20);
        statLabel3.setBounds(0, 370, 380, 20);


        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.setSize(new Dimension(380, 420));
        this.setVisible(true);
    }

    public void initListenerSetupButton(ActionListener listener) {
        this.setupButton.addActionListener(listener);
    }

    public void initListenerPlayButton(ActionListener listener) {
        this.playButton.addActionListener(listener);
    }

    public void initListenerPauseButton(ActionListener listener) {
        this.pauseButton.addActionListener(listener);
    }

    public void initListenerTeardownButton(ActionListener listener) {
        this.tearButton.addActionListener(listener);
    }

    public void initListenerDescribeButton(ActionListener listener) {
        this.describeButton.addActionListener(listener);
    }

    public void setIcon(Image image) {
        ImageIcon icon = new ImageIcon(image);
        this.iconLabel.setIcon(icon);
    }


    public void setTotalBytesText(String text) {
        statLabel1.setText(text);
    }

    public void setPacketLossRateText(String text) {
        statLabel2.setText(text);
    }

    public void setDataRateText(String text) {
        statLabel3.setText(text);
    }
}
