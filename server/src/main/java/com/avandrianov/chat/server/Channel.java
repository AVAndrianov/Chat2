package com.avandrianov.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.Date;

public class Channel extends Socket {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nameClient;
    private String str;
    private Server server;
    private String login;
    private String pass;
    private boolean liveClient = true;

    Channel(Server server, Socket socket) {
        this.socket = socket;
        this.server = server;
        Thread thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        authorization();
        try {
            while (liveClient) {
                System.out.println("hello");
                this.str = in.readUTF();
                String[] token = str.split("\\s");
                if (token[0].equals("/serverclose")) {
                    server.serverClose();
                    System.out.println("Channel");
                } else if (token[0].equals("/end")) {
                    out.writeUTF("/end");
                    break;
                } else if (token[0].equals("/stoptype")) {
                    this.nameClient = getNameClientFromBd();
                    server.updateUserList();
                } else if (token[0].equals("/type")) {
                    this.nameClient += "-type";
                    server.updateUserList();
                } else if (token[0].equals("/log")) {
                    ResultSet rs;
                    rs = server.bdas.getLog();
                    while (rs.next()) {
                        String msg = String.format("%s: %s %s для %s\n",
                                rs.getString(4),
                                rs.getString(1),
                                rs.getString(2),
                                rs.getString(3));
                        out.writeUTF(msg);
                    }
                } else if (token[0].equals("/help")) {
                    out.writeUTF(server.getHelp());
                } else if (token[0].equals("/logout")) {
                    out.writeUTF("/logout");
                    server.unSubscribe(this);
                    authorization();
                } else if (token[0].equals("/name")) {
                    server.bdas.changeName(token[1], this.login, this.pass);
                    this.nameClient = token[1];
                    server.msgYourself(String.format("You change name on: %s\n", this.nameClient), this);
                    server.updateUserList();
                    sendMsg("/changetitle " + this.nameClient);
                } else if (token[0].equals("/quit")) {
                    server.unSubscribe(this);
                    this.close();
                } else if (token[0].startsWith("/")) {
                    StringBuilder msg = new StringBuilder();
                    for (String t : token) {
                        if (!t.equals(token[0])) {
                            msg.append(t).append(" ");
                        }
                    }
                    String name = token[0].substring(1);
                    String d = new Date().toString();
                    this.nameClient = getNameClientFromBd();
                    server.bdas.addLog(msg.toString(), d, this.nameClient, name);
                    if (!this.nameClient.equals(name)) {
                        if (server.msgPersonal("private message from - " + this.nameClient + ": "
                                + msg + "\n", name))
                            server.msgYourself("private message to - " + name + ": "
                                    + msg + "\n", this);
                        else
                            server.msgYourself("client with nickname " + name +
                                    " not found\n", this);
                    }
                } else {
                    this.nameClient = getNameClientFromBd();
                    server.msgBroadcast(this.nameClient + ": " + str);
                    String d = new Date().toString();
                    server.bdas.addLog(str, d, this.nameClient, "ALL");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                System.out.println("inClose");
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (Exception e) {
                System.out.println("outClose");
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (Exception e) {
                System.out.println("socketClose");
                e.printStackTrace();
            }
            server.unSubscribe(this);
        }
    }

    private void authorization() {
        try {
            out.writeUTF(server.getHelp());
            while (true) {
                this.str = in.readUTF();
                String[] token = this.str.split("\\s");
                if (token[0].equals("/auth")) {
                    this.login = token[1];
                    this.pass = token[2];
                    if ((str = server.bdas.getNicknamedByLoginAndPassword(token[1], token[2])) != null) {
                        this.nameClient = str;
                        out.writeUTF("/authok " + this.nameClient);
                        server.subscribe(this);
                        break;
                    } else {
                        out.writeUTF("wrong login or password\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            liveClient = false;
        }
    }

    public void sendMsg(String msg) {
        try {
            this.out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNameClient() {
        return this.nameClient;
    }

    public String getNameClientFromBd() {
        return this.nameClient = server.bdas.getNicknamedByLoginAndPassword(this.login, this.pass);
    }
}