package com.interviewai.client.api;

import static com.interviewai.client.api.ApiClient.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.interviewai.client.model.UserManagementDTO;
import java.util.*;

public class UserManagementGateway {
  private final ApiClient api = ApiClient.getInstance();

  public List<UserManagementDTO> getAllUsersWithDetails() {
    return api.get("/admin/users", new TypeReference<List<UserManagementDTO>>() {});
  }

  public UserManagementDTO getUserById(int id) {
    return api.get("/admin/users/" + id, UserManagementDTO.class);
  }

  public List<UserManagementDTO> searchUsers(
      String search, String type, String language, String status, String level) {
    String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    return getAllUsersWithDetails().stream()
        .filter(
            u ->
                u.getUsername().toLowerCase(Locale.ROOT).contains(term)
                    || String.valueOf(u.getEmail()).toLowerCase(Locale.ROOT).contains(term))
        .filter(
            u ->
                type == null
                    || type.isBlank()
                    || type.equals("All Types")
                    || type.equals(u.getInterviewType()))
        .filter(
            u ->
                language == null
                    || language.isBlank()
                    || language.equals("All Languages")
                    || language.equals(u.getLanguage()))
        .filter(
            u ->
                status == null
                    || status.isBlank()
                    || status.equals("All Status")
                    || u.isActive() == status.equals("Active"))
        .filter(
            u ->
                level == null
                    || level.isBlank()
                    || level.equals("All Levels")
                    || (level.startsWith("Beginner") && u.getTotalXP() <= 100)
                    || (level.startsWith("Intermediate")
                        && u.getTotalXP() > 100
                        && u.getTotalXP() <= 500)
                    || (level.startsWith("Advanced") && u.getTotalXP() > 500))
        .toList();
  }

  private boolean save(UserManagementDTO u) {
    api.put(
        "/admin/users/" + u.getId(),
        body(
            "username",
            u.getUsername(),
            "email",
            u.getEmail(),
            "active",
            u.isActive(),
            "role",
            u.getRole()),
        Void.class);
    return true;
  }

  public boolean suspendUser(int id) {
    var u = getUserById(id);
    u.setActive(false);
    return save(u);
  }

  public boolean activateUser(int id) {
    var u = getUserById(id);
    u.setActive(true);
    return save(u);
  }

  public boolean resetUserXP(int id) {
    api.post("/admin/users/" + id + "/reset-xp", null, Void.class);
    return true;
  }

  public boolean resetUserProgress(int id) {
    api.post("/admin/users/" + id + "/reset-progress", null, Void.class);
    return true;
  }

  public boolean updateUserEmail(int id, String email) {
    var u = getUserById(id);
    u.setEmail(email);
    return save(u);
  }

  public boolean updateUsername(int id, String name) {
    var u = getUserById(id);
    u.setUsername(name);
    return save(u);
  }

  public boolean updateUserRole(int id, String role) {
    var u = getUserById(id);
    u.setRole(role);
    return save(u);
  }

  public List<String> getDistinctInterviewTypes() {
    return getAllUsersWithDetails().stream()
        .map(UserManagementDTO::getInterviewType)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .toList();
  }

  public List<String> getDistinctLanguages() {
    return getAllUsersWithDetails().stream()
        .map(UserManagementDTO::getLanguage)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .toList();
  }

  public List<String> getUserBadges(int id) {
    return getUserById(id).getBadges();
  }

  public int getTotalUserCount() {
    return getAllUsersWithDetails().size();
  }
}
