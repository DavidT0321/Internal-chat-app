package com.example.demo.client;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                LoginGui loginGui = new LoginGui();
                loginGui.setVisible(true);
            }
        });
    }
}