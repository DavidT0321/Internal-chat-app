package com.example.demo.client;

import com.example.demo.User;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LoginGui extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private StompSession stompSession;
    private StompSession.Subscription currentAuthSubscription;
    private String currentSubscribedUser = "";

    public LoginGui() {
        super("Chat Autentificare");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Utilities.PrimaryColor);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Utilizator:");
        userLabel.setForeground(Utilities.Text_Color);
        gbc.gridx = 0; gbc.gridy = 0;
        add(userLabel, gbc);

        usernameField = new JTextField(15);
        gbc.gridx = 1; gbc.gridy = 0;
        add(usernameField, gbc);

        JLabel passLabel = new JLabel("Parola:");
        passLabel.setForeground(Utilities.Text_Color);
        gbc.gridx = 0; gbc.gridy = 1;
        add(passLabel, gbc);

        passwordField = new JPasswordField(15);
        gbc.gridx = 1; gbc.gridy = 1;
        add(passwordField, gbc);

        JButton loginButton = new JButton("Logare");
        gbc.gridx = 0; gbc.gridy = 2;
        add(loginButton, gbc);

        JButton registerButton = new JButton("Inregistrare");
        gbc.gridx = 1; gbc.gridy = 2;
        add(registerButton, gbc);

        connectWebSocket();

        loginButton.addActionListener(e -> handleAuth("/app/login"));
        registerButton.addActionListener(e -> handleAuth("/app/register"));
    }

    private void connectWebSocket() {
        try {
            List<Transport> transports = new ArrayList<>();
            transports.add(new WebSocketTransport(new StandardWebSocketClient()));
            SockJsClient sockJsClient = new SockJsClient(transports);
            WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);


            List<MessageConverter> converters = new ArrayList<>();
            converters.add(new StringMessageConverter());
            converters.add(new JacksonJsonMessageConverter());
            stompClient.setMessageConverter(new CompositeMessageConverter(converters));

            stompSession = stompClient.connectAsync("ws://localhost:8080/ws", new StompSessionHandlerAdapter() {
                @Override
                public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                    System.err.println("!!! EROARE INTERNA STOMP !!!");
                    exception.printStackTrace();
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    System.err.println("!!! EROARE DE RETEA !!!");
                    exception.printStackTrace();
                }
            }).get();
            System.out.println("Conexiune WebSocket stabilita cu succes in frontend!");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Serverul backend nu este pornit!", "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAuth(String endpoint) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Toate campurile sunt obligatorii!", "Atentie", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (stompSession == null || !stompSession.isConnected()) {
            JOptionPane.showMessageDialog(this, "Nu exista conexiune cu serverul!", "Eroare", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String safeNetworkName = username.replace(" ", "_");

        if (!safeNetworkName.equals(currentSubscribedUser)) {
            if (currentAuthSubscription != null) {
                currentAuthSubscription.unsubscribe();
            }

            currentSubscribedUser = safeNetworkName;
            currentAuthSubscription = stompSession.subscribe("/topic/auth/" + safeNetworkName, new StompFrameHandler() {
                @Override
                public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    String response = (String) payload;
                    System.out.println("Clientul a primit raspunsul: " + response);

                    SwingUtilities.invokeLater(() -> {
                        if (response != null && response.contains("LOGIN_SUCCESS")) {
                            try {
                                ClientGui clientGui = new ClientGui(username);
                                clientGui.setVisible(true);

                                JOptionPane.showMessageDialog(LoginGui.this, "Te-ai logat cu succes!");
                                LoginGui.this.dispose();

                            } catch (Throwable ex) {
                                System.err.println("!!! A CRAPAT LA DESCHIDEREA CHATULUI !!!");
                                ex.printStackTrace();
                            }
                        } else if (response != null && response.contains("LOGIN_FAIL")) {
                            JOptionPane.showMessageDialog(LoginGui.this, "Credentiale incorecte!", "Eroare", JOptionPane.ERROR_MESSAGE);
                        } else if (response != null && response.contains("REGISTER_SUCCESS")) {
                            JOptionPane.showMessageDialog(LoginGui.this, "Cont creat cu succes! Acum te poti loga.");
                        } else if (response != null && response.contains("REGISTER_FAIL_EXISTS")) {
                            JOptionPane.showMessageDialog(LoginGui.this, "Acest utilizator exista deja!", "Eroare", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
            });

            new Thread(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                stompSession.send(endpoint, new User(username, password));
            }).start();

        } else {
            stompSession.send(endpoint, new User(username, password));
        }
    }
}