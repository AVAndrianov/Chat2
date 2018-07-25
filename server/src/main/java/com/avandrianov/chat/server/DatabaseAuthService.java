package com.avandrianov.chat.server;

import java.sql.*;

public class DatabaseAuthService implements AuthService {
    private Connection connection;
    private Statement stmt;

    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:users.db");
        stmt = connection.createStatement();
    }

    public void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNicknamedByLoginAndPassword(String login, String password) {
        try {
            ResultSet rs = stmt.executeQuery(String.format("SELECT nickname FROM singe_in WHERE pass = '%s' AND login = '%s';", password, login));
            if (!rs.next()) {
                return null;
            }
            return rs.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void changeName(String newName, String login, String pass) {
        try {
            stmt.executeUpdate(String.format("UPDATE singe_in SET nickname = '%s' WHERE login = '%s' AND pass = '%s'", newName, login, pass));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addLog(String text, String data, String fromName, String toName) {
        try {
            stmt.executeUpdate(String.format("INSERT INTO chat_log (log_text, data_log, to_log, from_log) VALUES ('%s','%s','%s','%s')", text, data, toName, fromName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ResultSet getLog() {
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery(String.format("SELECT log_text, data_log, to_log, from_log FROM chat_log"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("DataBase");
        }
        return rs;
    }
}
