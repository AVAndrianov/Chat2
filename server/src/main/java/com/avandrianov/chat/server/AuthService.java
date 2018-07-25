package com.avandrianov.chat.server;

public interface AuthService {
    String getNicknamedByLoginAndPassword(String login, String password);
}
