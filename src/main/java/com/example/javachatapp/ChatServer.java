package com.example.javachatapp;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<PrintWriter, String> userNames = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Empire Server started on Port 1234...");
        ServerSocket serverSocket = new ServerSocket(1234);

        while (true) {
            new ClientHandler(serverSocket.accept()).start();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) { this.socket = socket; }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                synchronized (clientWriters) { clientWriters.add(out); }

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("USER_JOINED:")) {
                        String name = message.split(":")[1];
                        userNames.put(out, name);
                        broadcastUserList();
                    } else {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("User disconnected.");
            } finally {
                if (out != null) {
                    synchronized (clientWriters) { clientWriters.remove(out); }
                    userNames.remove(out);
                    broadcastUserList();
                }
            }
        }

        private void broadcastUserList() {
            String list = "UPDATE_USERS:" + String.join(",", userNames.values());
            for (PrintWriter writer : clientWriters) {
                writer.println(list);
            }
        }
    }
}