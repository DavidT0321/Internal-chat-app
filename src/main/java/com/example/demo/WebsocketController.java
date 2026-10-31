package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class WebsocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager webSocketSessionManager;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Autowired
    public WebsocketController(SimpMessagingTemplate messagingTemplate,
                               WebSocketSessionManager webSocketSessionManager,
                               MessageRepository messageRepository,
                               UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketSessionManager = webSocketSessionManager;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @MessageMapping("/message")
    public void handleMessage(Message message){
        Message savedMessage = messageRepository.save(message);
        System.out.println("Saved and received message from user: " + savedMessage.getUser() + ": " + savedMessage.getMessage());
        messagingTemplate.convertAndSend("/topic/messages", savedMessage);
    }

    @MessageMapping("/connect")
    public void connectUser(String username){
        webSocketSessionManager.addUsername(username);
        webSocketSessionManager.broadcastActiveUsernames();
        System.out.println("Connected user: " + username);
    }

    @MessageMapping("/disconnect")
    public void disconnectUser(String username){
        webSocketSessionManager.removeUsername(username);
        webSocketSessionManager.broadcastActiveUsernames();
        System.out.println("Disconnected user: " + username);
    }

    @MessageMapping("/request-users")
    public void requestusers(String username){
        webSocketSessionManager.broadcastActiveUsernames();
        List<Message> history = messageRepository.findTop50ByOrderByTimestampAsc();
        for (Message oldMessage : history) {
            messagingTemplate.convertAndSend("/topic/messages", oldMessage);
        }
        System.out.println("Sent chat history to user: " + username);
    }

    @MessageMapping("/register")
    public void registerUser(@Payload User user) {
        System.out.println("-> A ajuns cererea de inregistrare de la: " + user.getUsername());

        String safeNetworkName = user.getUsername().replace(" ", "_");
        String destination = "/topic/auth/" + safeNetworkName;

        if (userRepository.existsById(user.getUsername())) {
            messagingTemplate.convertAndSend(destination, "\"REGISTER_FAIL_EXISTS\"");
            return;
        }

        userRepository.save(user);
        messagingTemplate.convertAndSend(destination, "\"REGISTER_SUCCESS\"");
        System.out.println("-> Cont salvat cu succes: " + user.getUsername());
    }

    @MessageMapping("/login")
    public void loginUser(@Payload User user) {
        System.out.println("-> A ajuns cererea de logare de la: " + user.getUsername());

        String safeNetworkName = user.getUsername().replace(" ", "_");
        String destination = "/topic/auth/" + safeNetworkName;

        java.util.Optional<User> dbUser = userRepository.findById(user.getUsername());

        if (dbUser.isPresent() && dbUser.get().getPassword().equals(user.getPassword())) {
            messagingTemplate.convertAndSend(destination, "\"LOGIN_SUCCESS\"");
            System.out.println("-> Parola corecta trimisa spre canalul: " + destination);
        } else {
            messagingTemplate.convertAndSend(destination, "\"LOGIN_FAIL\"");
            System.out.println("-> Parola gresita / Cont inexistent pentru: " + user.getUsername());
        }
    }
}