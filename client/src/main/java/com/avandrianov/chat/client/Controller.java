package com.avandrianov.chat.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static java.awt.event.KeyEvent.VK_DOWN;
import static javafx.geometry.Pos.BASELINE_CENTER;

public class Controller implements Initializable {
    private DataInputStream in;
    private DataOutputStream out;
    private Stage stage;
    private boolean typeEvent = true;
    private String lastMsg;

    private void setAuthorized(boolean auth, boolean connection) {
        if (!auth && !connection) {
            usersPanel.setManaged(false);
            usersPanel.setVisible(false);
            sendPanel.setVisible(false);
            sendPanel.setManaged(false);
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            sendPanel2.setVisible(false);
            sendPanel2.setManaged(false);
            hostPanel.setVisible(true);
            hostPanel.setManaged(true);
        } else if (!auth) {
            usersPanel.setManaged(false);
            usersPanel.setVisible(false);
            sendPanel.setVisible(false);
            sendPanel.setManaged(false);
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            sendPanel2.setVisible(false);
            sendPanel2.setManaged(false);
            hostPanel.setVisible(false);
            hostPanel.setManaged(false);
        } else {
            usersPanel.setManaged(true);
            usersPanel.setVisible(true);
            sendPanel.setVisible(true);
            sendPanel.setManaged(true);
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            sendPanel2.setVisible(true);
            sendPanel2.setManaged(true);
            hostPanel.setVisible(false);
            hostPanel.setManaged(false);
        }
    }

    @FXML
    TextField textField, loginField, hostField, portField;
    @FXML
    PasswordField passwordField;
    @FXML
    HBox sendPanel, authPanel, sendPanel2, hostPanel;
    @FXML
    VBox usersPanel;
    @FXML
    ListView<String> usersList;
    @FXML
    TextFlow textFlo;

    public void initialize(URL location, ResourceBundle resources) {
        textFlo.setPadding(new Insets(10, 10, 10, 10));
        textFlo.setLineSpacing(10);
        textField.setOnKeyPressed(this::handle);
        usersList.setOnMouseClicked(this::handle);
        setAuthorized(false, false);
        textFlo.getChildren().addAll(getHostsList());
        ObservableList<String> list = FXCollections.observableArrayList();
        usersList.setItems(list);
    }

    private void run() {
        try {
            authorization();
            while (true) {
                String str = in.readUTF();
                String[] tokens = str.split("\\s");
                if (str.equals("Server close!!!")) {
                    end();
                    Platform.runLater(() -> textFlo.getChildren().add(new Text(str)));
                } else if (str.equals("/end")) {
                    end();
                } else if (str.startsWith("/clients")) {
                    Platform.runLater(() -> {
                        usersList.getItems().clear();
                        for (int i = 1; i < tokens.length; i++) {
                            usersList.getItems().add(tokens[i]);
                        }
                    });
                } else if (str.equals("/logout")) {
                    Platform.runLater(() -> {
                        stage.setTitle("Chat");
                        usersList.getItems().clear();
                        textFlo.getChildren().clear();
                    });
                    setAuthorized(false, true);
                    authorization();
                } else if (tokens[0].equals("/changetitle")) {
                    Platform.runLater(() -> stage.setTitle(tokens[1]));
                } else {

                    if (str.contains(":)")) Platform.runLater(() -> {
                        Text text;
                        if (textFlo.getChildren().size() == 0) {
                            text = new Text(str);
                        } else {
                            text = new Text("\n" + str);
                        }
                        System.out.println("Hello");
                        ImageView imageView = null;
                        imageView = new ImageView("http://komotoz.ru/kartinki/images/kartinki_s_serdechkami/kartinki_s_serdechkami_06.jpg");
                        //                                new FileChooser().showOpenDialog(stage).toURI());
                        imageView.setFitHeight(200);
                        imageView.setFitWidth(200);
                        text.setText(text.getText().replace(":)", "     "));

                        textFlo.getChildren().addAll(text, imageView);

                    });
                    else {
                        Platform.runLater(() -> textFlo.getChildren().add(new Text(str)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void authorization() {
        Text text = new Text();
        try {
            while (true) {
                String str;
                str = in.readUTF();
                if (str.startsWith("/authok")) {
                    String[] token = str.split("\\s");
                    Platform.runLater(() -> {
                        stage.setTitle(token[1]);
                        setAuthorized(true, true);
                        textField.requestFocus();
                        textFlo.getChildren().clear();
                    });
                    break;
                } else {
                    text.setText(str);

                    Platform.runLater(() -> textFlo.getChildren().add(text));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void end() {
        Platform.runLater(() -> {
            stage.setTitle("Chat");
            setAuthorized(false, false);
            textFlo.getChildren().clear();
            textFlo.getChildren().add(getHostsList());

        });
    }

    @FXML
    private void connect() {
        Text text = new Text();
        Platform.runLater(() -> {
            int port = 0;
            String ip;
            ip = hostField.getText();
            try {
                port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException e) {
                text.setText("not a correct port\n");

                textFlo.getChildren().add(text);
            }
            Socket socket = null;
            try {
                InetAddress ipAddress = InetAddress.getByName(ip);
//                socket = new Socket(ipAddress, port);
                socket = new Socket("localhost", 8189);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (UnknownHostException e) {
                text.setText("not a valid host name\n");

                textFlo.getChildren().add(text);
            } catch (IOException e) {
                text.setText("no response from the server try again\n");

                textFlo.getChildren().add(text);
            }
            if (Objects.requireNonNull(socket).isConnected()) {
                setAuthorized(false, true);
                loginField.requestFocus();
                hostField.clear();
                portField.clear();
                textFlo.getChildren().clear();
                text.setText("login: login1\npass: pass1\n");

                textFlo.getChildren().add(text);
                Thread thread = new Thread(this::run);
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    public void singIn() {
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
//            out.writeUTF("/auth login1 pass1");
        } catch (IOException e) {
            e.printStackTrace();
        }
        loginField.clear();
        passwordField.clear();
    }

    public void send() {
        try {
            lastMsg = textField.getText();
            out.writeUTF(lastMsg);
            textField.clear();
            textField.requestFocus();
            out.writeUTF("/stoptype");
            typeEvent = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private Text getHostsList() {
        return new Text("port\n-8189\n" +
                "host\n-127.0.0.1\n" +
                "-localhost\n" +
                "-192.168.1.101\n");
    }

    private void handle(KeyEvent event) {
        Platform.runLater(() -> {
            try {
                Robot r = new Robot();
                if (typeEvent && event.getText().length() == 1 && event.getCode() != KeyCode.TAB) {
                    out.writeUTF("/type");
                    typeEvent = false;
                }
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.BACK_SPACE) {
                    out.writeUTF("/stoptype");
                    typeEvent = true;
                }
                if (event.getCode() == KeyCode.UP) {
                    if (textField.getText().equals("")) {
                        textField.appendText(lastMsg);
                        r.keyPress(VK_DOWN);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handle(MouseEvent event) {
        if (event.getClickCount() == 2) {
            textField.clear();
            textField.appendText("/");
            textField.appendText(usersList.getSelectionModel().getSelectedItem());
            textField.appendText(" ");
            textField.requestFocus();
            textField.end();
        }
    }

    public void sendFile() {
        System.out.println(Paths.get("/Users/antonandrivnov/hart.png").toAbsolutePath());
        final FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        System.out.println(file);
        System.out.println(file.toString());
        System.out.println(file.toURI());
    }

}