package com.avandrianov.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;

public class Server {
    private Vector<Channel> channels;
    private ServerSocket serverSocket;
    protected DatabaseAuthService bdas;

    Server() {
        try {
            serverSocket = new ServerSocket(8189);
            channels = new Vector<>();
            bdas = new DatabaseAuthService();
            try {
                bdas.connect();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
            while (true) {
                Socket socket = serverSocket.accept();
                new Channel(this, socket);
                updateUserList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
                bdas.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribe(Channel channel) {
        channels.add(channel);
        updateUserList();
        msgAllButMe(channel.getNameClient() + " joined the chat!\n", channel.getNameClient());
    }

    public void unSubscribe(Channel channel) {
        channels.remove(channel);
        updateUserList();
        msgAllButMe(channel.getNameClient() + " left the chat!\n", channel.getNameClient());
    }

    public void msgBroadcast(String msg) {
        for (Channel chanel : channels) {
            chanel.sendMsg(msg);
        }
    }

    public void msgYourself(String msg, Channel channel) {
        channel.sendMsg(msg);
    }

    public boolean msgPersonal(String msg, String name) {
        for (Channel c : channels) {
            if (c.getNameClient().equals(name)) {
                c.sendMsg(msg);
                return true;
            }
        }
        return false;
    }

    private void msgAllButMe(String msg, String name) {
        for (Channel c : channels)
            if (!c.getNameClient().equals(name))
                c.sendMsg(msg);
            else
                c.sendMsg("Welcome " + name + "\n");
    }

    public void updateUserList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clients ");
        for (Channel chanel : channels) {
            sb.append(chanel.getNameClient()).append(" ");
        }
        sb.setLength(sb.length() - 1);
        System.out.println(sb.toString());
        String out = sb.toString();
        for (Channel chanel : channels) {
            System.out.println(chanel.getNameClient());
            chanel.sendMsg(out);
        }
    }

    public void serverClose() {
        try {
            msgBroadcast("Server close!!!");
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String getHelp() {
        return "/help\n" +
                "/log - print chat log.\n" +
                "/end - leave the server.\n" +
                "/name - change name.\n" +
                "/logout - leave chat.\n" +
                "/serverclose - stops the server\n";
    }
}