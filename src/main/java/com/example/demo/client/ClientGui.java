package com.example.demo.client;
import com.example.demo.Message;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

public class ClientGui extends JFrame implements MessageListener {
    private JPanel connecteduserPanel, messagePanel;
    private MyStompClient myStompClient;
    private String username;
    private JScrollPane scrollPane;

    public ClientGui(String username) throws ExecutionException, InterruptedException {
        super("User " + username);
        this.username = username;
        myStompClient = new MyStompClient(this, username);
        setSize(1218, 685);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int option = JOptionPane.showConfirmDialog(ClientGui.this, "Are you sure you want to close this?", "Exit", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    myStompClient.disconnectUser(username);
                    ClientGui.this.dispose();
                }
            }
        });

        getContentPane().setBackground(Utilities.PrimaryColor);
        addGuiComponents();
    }

    private void addGuiComponents() {
        addConnectedUserComponents();
        addChatComponents();
    }

    private void addConnectedUserComponents() {
        connecteduserPanel = new JPanel();
        connecteduserPanel.setBorder(Utilities.addPadding(10, 10, 10, 10));
        connecteduserPanel.setLayout(new BoxLayout(connecteduserPanel, BoxLayout.Y_AXIS));
        connecteduserPanel.setBackground(Utilities.SecondaryColor);
        connecteduserPanel.setPreferredSize(new Dimension(200, getHeight()));
        connecteduserPanel.setBorder(BorderFactory.createLineBorder(Utilities.PrimaryColor));
        JLabel connectedUserLabel = new JLabel("Connected Users");
        connectedUserLabel.setFont(new Font("Arial", Font.BOLD, 18));
        connectedUserLabel.setForeground(Utilities.Text_Color);
        connecteduserPanel.add(connectedUserLabel);
        add(connecteduserPanel, BorderLayout.WEST);
    }

    private void addChatComponents() {
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBackground(Utilities.Label_Color);

        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(Utilities.PrimaryColor);

        scrollPane = new JScrollPane(messagePanel);
        scrollPane.setBackground(Utilities.TransparentColor);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(17);
        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                revalidate();
                repaint();
            }
        });
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setBorder(Utilities.addPadding(10, 10, 10, 10));
        inputPanel.setLayout(new BorderLayout());
        inputPanel.setBackground(Utilities.TransparentColor);

        JTextField inputField = new JTextField();
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    String input = inputField.getText();
                    if (input.isEmpty()) {
                        return;
                    }
                    inputField.setText("");

                    myStompClient.sendMessage(new Message(username, input));
                }
            }
        });
        inputField.setBorder(Utilities.addPadding(10, 10, 10, 10));
        inputField.setBackground(Utilities.SecondaryColor);
        inputField.setForeground(Utilities.Text_Color);
        inputField.setFont(new Font("Arial", Font.PLAIN, 16));
        inputField.setPreferredSize(new Dimension(inputPanel.getWidth() - 100, 50));
        inputPanel.add(inputField, BorderLayout.CENTER);


        JButton fileButton = new JButton("Fisier");
        fileButton.setFont(new Font("Arial", Font.BOLD, 14));
        fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(ClientGui.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();

                    if (selectedFile.length() > 5 * 1024 * 1024) {
                        JOptionPane.showMessageDialog(ClientGui.this, "Fisierul este prea mare! Te rugam sa alegi un fisier sub 5MB.", "Eroare marime", JOptionPane.WARNING_MESSAGE);
                        return;
                    }


                    byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                    String base64Content = Base64.getEncoder().encodeToString(fileBytes);


                    Message fileMessage = new Message(username, "A atasat un fisier.");
                    fileMessage.setFileName(selectedFile.getName());
                    fileMessage.setFileContent(base64Content);


                    myStompClient.sendMessage(fileMessage);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ClientGui.this, "A aparut o eroare la citirea fisierului.", "Eroare", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Utilities.TransparentColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        buttonPanel.add(fileButton, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        add(chatPanel, BorderLayout.CENTER);
    }

    private JPanel createchatmessagecomponent(Message message) {
        JPanel chatmessage = new JPanel();
        chatmessage.setBackground(Utilities.TransparentColor);
        chatmessage.setBorder(Utilities.addPadding(15, 20, 10, 20));
        chatmessage.setLayout(new BoxLayout(chatmessage, BoxLayout.Y_AXIS));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setBackground(Utilities.TransparentColor);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel usernamelabel = new JLabel(message.getUser());
        usernamelabel.setFont(new Font("Arial", Font.BOLD, 17));
        usernamelabel.setForeground(Utilities.Text_Color);
        headerPanel.add(usernamelabel);

        String timeString = "";
        try {
            if (message.getTimestamp() != null) {
                String rawTime = message.getTimestamp().toString();
                if (rawTime.contains("T")) {
                    timeString = rawTime.split("T")[1].substring(0, 5);
                } else if (rawTime.length() >= 16) {
                    timeString = rawTime.substring(11, 16);
                } else {
                    timeString = rawTime;
                }
            }
        } catch (Exception ignored) {}

        if (timeString.isEmpty()) {
            java.time.LocalTime now = java.time.LocalTime.now();
            timeString = String.format("%02d:%02d", now.getHour(), now.getMinute());
        }

        JLabel timeLabel = new JLabel("  [" + timeString + "]");
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 13));
        timeLabel.setForeground(Color.GRAY);
        headerPanel.add(timeLabel);

        chatmessage.add(headerPanel);

        JLabel messageLabel = new JLabel();
        messageLabel.setText("<html>" +
                "<body style='width:" + (int)(0.60 * getWidth()) + "px; margin-top: 4px;'>" +
                message.getMessage() +
                "</body>" +
                "</html>");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        messageLabel.setForeground(Utilities.Text_Color);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        chatmessage.add(messageLabel);


        if (message.getFileName() != null && !message.getFileName().isEmpty() && message.getFileContent() != null) {
            chatmessage.add(Box.createRigidArea(new Dimension(0, 5))); // Mic spatiu vizual

            JButton downloadButton = new JButton("Descarca: " + message.getFileName());
            downloadButton.setFont(new Font("Arial", Font.BOLD, 14));
            downloadButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            downloadButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            downloadButton.addActionListener(e -> {
                JFileChooser saveChooser = new JFileChooser();
                saveChooser.setSelectedFile(new File(message.getFileName())); // Propune automat numele original
                int saveResult = saveChooser.showSaveDialog(ClientGui.this);
                if (saveResult == JFileChooser.APPROVE_OPTION) {
                    try {
                        File targetFile = saveChooser.getSelectedFile();

                        byte[] decodedBytes = Base64.getDecoder().decode(message.getFileContent());
                        Files.write(targetFile.toPath(), decodedBytes);

                        JOptionPane.showMessageDialog(ClientGui.this, "Fisierul a fost salvat cu succes!", "Succes", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(ClientGui.this, "Nu am putut salva fisierul.", "Eroare", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            chatmessage.add(downloadButton);
        }

        return chatmessage;
    }

    @Override
    public void onmessagereceived(Message message) {
        messagePanel.add(createchatmessagecomponent(message));
        messagePanel.revalidate();
        messagePanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    @Override
    public void activeusersupdate(ArrayList<String> users) {
        if (connecteduserPanel.getComponents().length >= 2) {
            connecteduserPanel.remove(1);
        }
        JPanel userlistpanel = new JPanel();
        userlistpanel.setBackground(Utilities.TransparentColor);
        userlistpanel.setLayout(new BoxLayout(userlistpanel, BoxLayout.Y_AXIS));
        for (String user : users) {
            JLabel usernameLabel = new JLabel(user);
            usernameLabel.setForeground(Utilities.Text_Color);
            usernameLabel.setFont(new Font("Arial", Font.BOLD, 18));
            userlistpanel.add(usernameLabel);
        }
        connecteduserPanel.add(userlistpanel);
        revalidate();
        repaint();
    }
}