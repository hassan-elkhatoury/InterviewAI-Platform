package com.interviewai.client.api;

import static com.interviewai.client.api.ApiClient.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.interviewai.client.model.User;
import com.interviewai.client.util.SessionContext;
import java.util.*;

public class UserGateway {
  private final ApiClient api = ApiClient.getInstance();

  public record AuthResult(String accessToken, String tokenType, long expiresIn, User user) {}

  public User login(String identifier, String password) {
    api.clearToken();
    AuthResult result =
        api.post(
            "/auth/login", body("username", identifier, "password", password), AuthResult.class);
    api.setToken(result.accessToken());
    return result.user();
  }

  public boolean validateCredentials(String identifier, String password) {
    try {
      User user = login(identifier, password);
      SessionContext.setCurrentUser(user);
      return true;
    } catch (ApiException e) {
      if (e.getStatus() == 401) return false;
      throw e;
    }
  }

  public boolean createUser(String username, String email, String password, String role) {
    api.post(
        "/auth/register",
        body("username", username, "email", email, "password", password),
        AuthResult.class);
    return true;
  }

  public User getByUsername(String username) {
    return api.get("/users/me", User.class);
  }

  public User getById(int id) {
    User current = SessionContext.getCurrentUser();
    return api.get(
        current != null && current.getId() == id ? "/users/me" : "/admin/users/" + id, User.class);
  }

  public Integer[] getLastUsedIds(int id) {
    return api.get("/users/me/last-used", Integer[].class);
  }

  public boolean updateLastUsed(int uid, Integer course, Integer chapter) {
    api.put("/users/me/last-used", body("courseId", course, "chapterId", chapter), Void.class);
    return true;
  }

  public List<Map<String, Object>> getTopLearners(int limit) {
    return api.get("/leaderboard", new TypeReference<List<Map<String, Object>>>() {}).stream()
        .limit(limit)
        .toList();
  }

  public boolean updateEmail(int uid, String email) {
    User u = getById(uid);
    u.setEmail(email);
    updateUser(u);
    return true;
  }

  public void updateUser(User user) {
    User current = SessionContext.getCurrentUser();
    if (current != null && current.getId() == user.getId())
      api.put(
          "/users/me", body("username", user.getUsername(), "email", user.getEmail()), User.class);
    else
      api.put(
          "/admin/users/" + user.getId(),
          body(
              "username",
              user.getUsername(),
              "email",
              user.getEmail(),
              "active",
              user.isActive(),
              "role",
              user.getRole()),
          Void.class);
  }

  public boolean changePassword(String current, String replacement) {
    String username = SessionContext.getCurrentUser().getUsername();
    api.put(
        "/users/me/password",
        body("currentPassword", current, "newPassword", replacement),
        Void.class);
    api.clearToken();
    SessionContext.setCurrentUser(login(username, replacement));
    return true;
  }

  public boolean updatePassword(int uid, String password) {
    api.put("/admin/users/" + uid + "/password", body("newPassword", password), Void.class);
    return true;
  }

  public List<User> getAllUsers() {
    return api.get("/admin/users", new TypeReference<List<User>>() {});
  }

  public List<User> getAllAdmins() {
    return getAllUsers().stream().filter(u -> u.hasRole("ROLE_ADMIN")).toList();
  }
}
