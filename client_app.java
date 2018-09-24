/**
 * Java chat client that connects to a server.
 * It uses multithreading to process user input to
 * server and to receive server output.
 * Written by Ivan Mihaylov
 */
package javaFX_client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;



public class client_app extends Application implements EventHandler < ActionEvent > {

 public static void main(String[] argv) {
  launch(argv);
 }

 //////////////////////
 // JavaFX objects.
 //////////////////////

 Stage Prime;

 // Connect screen.
 HBox IPHBox;
 TextField IPAddress;
 TextField PortField;
 Button ConnectButton;
 Scene ConnectScreen;

 private int connectToServer(String address, int PORT) {
  server = null;
  try {
   server = new Socket("localhost", PORT);
  } catch (IOException e) {
   System.out.println("Failed to establish connection.");
   return 1;
  }
  System.out.println("Established connection.");

  try {
   serverOutput = new DataOutputStream(server.getOutputStream());
   serverInput = new DataInputStream(server.getInputStream());
  } catch (IOException e) {
   System.out.println("Failed to establish I/O connection with server.");
   return 2;
  }
  System.out.println("Established I/O connection.");
  return 0;
 }

 // Messaging screen.
 VBox msgBundle;
 HBox Sender;
 TextArea Messages;
 TextField userInput;
 Scene MsgScreen;
 Button sendButton;

 // Make and return the "enter server IP" scene.
 private Scene setConnectScreen() {
  ConnectButton = new Button();
  ConnectButton.setText("Connect");
  ConnectButton.setOnAction(this);

  IPAddress = new TextField("localhost");
  IPAddress.setPrefWidth(110);

  PortField = new TextField("1984");
  PortField.setPrefWidth(50);

  IPHBox = new HBox(IPAddress, PortField, ConnectButton);
  IPHBox.setAlignment(Pos.CENTER);

  StackPane layout = new StackPane();
  layout.getChildren().add(IPHBox);

  return new Scene(layout, 300, 250);
 }

 // Make and return the chat box scene.
 private Scene setMsgScene() {
  sendButton = new Button("Send");
  userInput = new TextField();
  Messages = new TextArea();

  Messages.setPrefHeight(500);
  Messages.setPrefWidth(400);
  Messages.setEditable(false);
  Messages.setMouseTransparent(true);
  Messages.setFocusTraversable(false);
  Messages.setWrapText(true);
  //Messages.setDisable(true);

  userInput.setOnKeyPressed(
   (event) -> {
    if (event.getCode() == KeyCode.ENTER)
     sendButton.fire();
   });

  userInput.setPrefWidth(212);
  userInput.requestFocus();

  sendButton.setFocusTraversable(false);
  sendButton.setOnAction(this);
  Sender = new HBox(userInput, sendButton);
  Sender.setAlignment(Pos.CENTER);

  msgBundle = new VBox(Messages, Sender);
  msgBundle.setAlignment(Pos.CENTER);
  StackPane layout = new StackPane();
  layout.getChildren().add(msgBundle);

  return new Scene(layout, 300, 500);
 }

 // Login screen.
 HBox hb_username;
 Button b_login;
 TextField tf_user;

 private Scene setLoginScene() {
  b_login = new Button("Log in");
  b_login.setOnAction(this);
  tf_user = new TextField();
  tf_user.setOnKeyPressed(
   (event) -> {
    if (event.getCode() == KeyCode.ENTER)
     b_login.fire();
   });
  hb_username = new HBox(tf_user, b_login);

  hb_username.setAlignment(Pos.CENTER);

  StackPane layout = new StackPane();
  layout.getChildren().add(hb_username);
  return new Scene(layout, 300, 250);
 }

 @Override
 public void start(Stage primaryStage) {
  Prime = primaryStage;
  Prime.setTitle("Vanko Chat");
  Prime.setScene(setConnectScreen());
  Prime.show();
 }


 //////////////////////
 // Server Objects.
 //////////////////////
 Socket server;
 DataOutputStream serverOutput = null;
 DataInputStream serverInput = null;
 boolean isLogged;
 String username;

 @Override
 public void handle(ActionEvent event) {
  if (event.getSource() == ConnectButton) {
   String Address = IPAddress.getText();
   int PORT = Integer.parseInt(PortField.getText());

   if (connectToServer(Address, PORT) != 0) {
    System.out.println("Server not found");
   } else {
    Prime.setScene(setLoginScene());
    Prime.show();
    Runnable receiver = () -> {
     @SuppressWarnings("unused")
     String message = null;
     System.out.println("Starting receiver thread");
     while (true) {
      try {
       message = serverInput.readUTF();
       Messages.appendText(message);
       Messages.appendText("\n");
      } catch (IOException e) {
       System.out.println("Connection with server receiver interrupted.");
       System.exit(-1);
      }
     }
    };
    Thread receiver_thread = new Thread(receiver);

    receiver_thread.start();
   }
  }

  if (event.getSource() == sendButton) {
   userInput.requestFocus();
   String message = userInput.getText();
   userInput.clear();
   Messages.appendText(username + ": " + message);
   Messages.appendText("\n");
   try {
    serverOutput.writeUTF(message);
   } catch (IOException e) {
    System.out.println("Unable to send message.");
    System.exit(-1);
   }
  }

  if (event.getSource() == b_login) {
   username = "";
   username = tf_user.getText();
   if (username != "") {
    try {
     serverOutput.writeUTF(username);
     Prime.setScene(setMsgScene());
     Prime.show();
    } catch (IOException e) {
     System.out.println("Unable to send message.");
     System.exit(-1);
    }
   }
  }
 }


 @Override
 public void stop() {
  try {
   serverOutput.close();
   serverInput.close();
   server.close();
  } catch (IOException e) {
   System.out.println("Failed to close serverSocket");
   System.exit(-1);
  }
 }
}
