package com.example.javachatapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatClient extends Application {
    private PrintWriter out;
    private VBox chatHistory = new VBox(12);
    private ListView<String> userListView = new ListView<>();
    private ScrollPane scrollPane = new ScrollPane();
    private TextField inputField = new TextField();
    private Label typingLabel = new Label(" ");
    private Button sendButton = new Button("SEND");
    private String userName;

    private final String DARK_BG = "#17212b";
    private final String MY_BUBBLE = "#2b5278";
    private final String THEIR_BUBBLE = "#182533";
    private final String EMPIRE_BLUE = "#0088cc";

    @Override
    public void start(Stage primaryStage) {
        userName = new TextInputDialog("User").showAndWait().orElse("Anonymous");

        // 1. Chat Area Styling
        chatHistory.setPadding(new Insets(15));
        chatHistory.setStyle("-fx-background-color: " + DARK_BG + ";");
        scrollPane.setContent(chatHistory);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + DARK_BG + "; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        chatHistory.heightProperty().addListener((obs, oldVal, newVal) -> scrollPane.setVvalue(1.0));

        // 2. User List Styling (The Green Dot Logic)
        userListView.setPrefWidth(150);
        userListView.setStyle("-fx-control-inner-background: #0e1621; -fx-background-color: #0e1621; -fx-border-color: transparent;");

        // Customizing the list to show a green dot + Name
        userListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #0e1621;");
                } else {
                    Circle statusDot = new Circle(5, Color.web("#4ade80")); // Green Dot
                    Label nameLabel = new Label(item);
                    nameLabel.setTextFill(Color.WHITE);
                    nameLabel.setStyle("-fx-font-weight: bold;");

                    HBox container = new HBox(8, statusDot, nameLabel);
                    container.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(container);
                    setStyle("-fx-background-color: #0e1621;");
                }
            }
        });

        // 3. Header for Online Users
        Label onlineHeader = new Label("ONLINE USERS");
        onlineHeader.setStyle("-fx-text-fill: #808080; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 5;");
        VBox userSideBar = new VBox(5, onlineHeader, userListView);
        userSideBar.setStyle("-fx-background-color: #0e1621; -fx-padding: 10;");

        // 4. Input Bar Styling
        inputField.setPromptText("Write a message...");
        inputField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        sendButton.setStyle("-fx-background-color: " + EMPIRE_BLUE + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 10 20;");

        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        // 5. Layout Setup
        HBox bottomBar = new HBox(10, inputField, sendButton);
        bottomBar.setPadding(new Insets(10));

        VBox chatContainer = new VBox(5, scrollPane, typingLabel, bottomBar);
        HBox.setHgrow(chatContainer, Priority.ALWAYS);

        HBox mainLayout = new HBox(0, chatContainer, userSideBar);
        mainLayout.setStyle("-fx-background-color: " + DARK_BG + ";");

        primaryStage.setScene(new Scene(mainLayout, 750, 600));
        primaryStage.setTitle("Empire Chat: " + userName);
        primaryStage.show();

        connectToServer();
    }

    private void sendMessage() {
        String text = inputField.getText();
        if (text != null && !text.trim().isEmpty() && out != null) {
            out.println(userName + ": " + text);
            inputField.clear();
        }
    }

    private void addMessageToUI(String msg) {
        boolean isMe = msg.startsWith(userName + ":");
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        Label content = new Label(msg);
        content.setWrapText(true);
        content.setMaxWidth(350);
        content.setTextFill(Color.WHITE);

        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 9px;");

        VBox bubble = new VBox(4, content, timeLbl);
        bubble.setPadding(new Insets(10));
        bubble.setStyle("-fx-background-radius: 15; -fx-background-color: " + (isMe ? MY_BUBBLE : THEIR_BUBBLE));
        bubble.setAlignment(isMe ? Pos.BOTTOM_RIGHT : Pos.BOTTOM_LEFT);

        HBox wrapper = new HBox(bubble);
        wrapper.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(0, 0, 5, 0));

        Platform.runLater(() -> chatHistory.getChildren().add(wrapper));
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 1234);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("USER_JOINED:" + userName);

            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String finalLine = line;
                        if (finalLine.startsWith("UPDATE_USERS:")) {
                            updateUserList(finalLine);
                        } else if (finalLine.startsWith("SIGNAL_TYPING:")) {
                            handleTyping(finalLine);
                        } else {
                            addMessageToUI(finalLine);
                        }
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> addMessageToUI("SYSTEM: Lost connection."));
                }
            }).start();
        } catch (IOException e) {
            Platform.runLater(() -> addMessageToUI("SYSTEM: Server offline."));
        }
    }

    private void updateUserList(String data) {
        String[] users = data.replace("UPDATE_USERS:", "").split(",");
        Platform.runLater(() -> {
            userListView.getItems().clear();
            userListView.getItems().addAll(users);
        });
    }

    private void handleTyping(String signal) {
        String user = signal.split(":")[1];
        if (!user.equals(userName)) {
            Platform.runLater(() -> {
                typingLabel.setText(user + " is typing...");
                typingLabel.setStyle("-fx-text-fill: #808080; -fx-padding: 0 0 0 20;");
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (Exception e) {}
                    Platform.runLater(() -> typingLabel.setText(" "));
                }).start();
            });
        }
    }

    public static void main(String[] args) { launch(args); }
}