package com.interviewai.client.service;
import com.interviewai.client.api.UserGateway;
import com.interviewai.client.model.User;
public class AuthService {
    private final UserGateway users=new UserGateway();
    public boolean authenticate(String username,String password) { return users.validateCredentials(username,password); }
    public User getUser(String username) { return users.getByUsername(username); }
}
