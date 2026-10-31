package com.example.demo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import java.time.LocalDateTime;

@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender")
    private String user;

    private String message;
    private LocalDateTime timestamp;

    // --- Noile campuri pentru fisiere ---
    private String fileName;

    @Lob
    @Column(length = 10000000) // Alocam spatiu pentru fisiere de pana la aprox. 10MB
    private String fileContent;

    public Message() {
        this.timestamp = LocalDateTime.now();
    }

    public Message(String user, String message) {
        this.user = user;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // --- Getteri si Setteri standard ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // --- Getteri si Setteri pentru fisiere ---
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileContent() { return fileContent; }
    public void setFileContent(String fileContent) { this.fileContent = fileContent; }
}