package client;

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


        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.add(buttonPanel);
        buttonPanel.setBounds(0, 280, 380, 50);

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
}
