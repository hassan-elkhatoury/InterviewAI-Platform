package com.interviewai.client.service;
import com.interviewai.client.api.*;
import com.interviewai.client.model.*;
import java.util.*;
public class AdminService {
    private final UserGateway users=new UserGateway();
    public boolean createAdmin(String username, String email, String password, String role) {
        ApiClient.getInstance().post("/admin/users", ApiClient.body("account", ApiClient.body("username", username,
            "email", email, "password", password), "role", role), User.class);
        return true;
    }
    public User login(String username,String password) { User u=users.login(username,password); return u.hasRole("ROLE_ADMIN")?u:null; }
    public List<User> getAllAdmins() { return users.getAllAdmins(); }
    public List<User> getAllUsers() { return users.getAllUsers(); }
    public List<Role> getAllRoles() { return new RoleGateway().getAllRoles(); }
    public boolean updateAdminRole(int id,String role) { return new UserManagementGateway().updateUserRole(id,role); }
    public boolean toggleAdminAccess(int id,boolean active) { var gateway=new UserManagementGateway(); return active?gateway.activateUser(id):gateway.suspendUser(id); }
    public boolean resetPassword(int id,String password) { return users.updatePassword(id,password); }
}
