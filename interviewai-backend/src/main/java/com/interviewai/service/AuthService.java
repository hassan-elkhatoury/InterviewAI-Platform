package com.interviewai.service;

import com.interviewai.dto.*;
import com.interviewai.entity.User;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.UserRepository;
import com.interviewai.security.TokenProvider;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final TokenProvider tokens;
  private final String dummyHash;

  public AuthService(UserRepository users, PasswordEncoder passwords, TokenProvider tokens) {
    this.users = users;
    this.passwords = passwords;
    this.tokens = tokens;
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  @Transactional
  public Views.Auth register(Requests.Register request) {
    if (users.existsByUsernameIgnoreCase(request.username())
        || users.existsByEmailIgnoreCase(request.email()))
      throw ApiException.conflict("Username or email is already registered");
    User user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email().toLowerCase(Locale.ROOT));
    user.setPasswordHash(passwords.encode(request.password()));
    user.setRole("ROLE_USER");
    users.saveAndFlush(user);
    return authenticated(user);
  }

  @Transactional
  public Views.Auth login(Requests.Login request) {
    var user =
        users
            .findByUsernameIgnoreCase(request.username())
            .or(() -> users.findByEmailIgnoreCase(request.username()))
            .orElse(null);
    String hash =
        user == null || user.getPasswordHash() == null ? dummyHash : user.getPasswordHash();
    boolean legacy = hash.matches("(?i)[0-9a-f]{64}");
    boolean matches =
        legacy
            ? matchesLegacy(request.password(), hash)
            : passwords.matches(request.password(), hash);
    if (user == null || !matches || !user.isActive())
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    if (legacy) {
      user.setPasswordHash(passwords.encode(request.password()));
      users.save(user);
    }
    return authenticated(user);
  }

  private boolean matchesLegacy(String rawPassword, String stored) {
    try {
      byte[] digest =
          java.security.MessageDigest.getInstance("SHA-256")
              .digest(rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.security.MessageDigest.isEqual(digest, HexFormat.of().parseHex(stored));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private Views.Auth authenticated(User user) {
    return new Views.Auth(tokens.issue(user), "Bearer", tokens.expiresIn(), view(user));
  }

  public static Views.User view(User u) {
    boolean admin = "ROLE_ADMIN".equals(u.getRole());
    var permissions =
        admin
            ? List.of(
                new Views.Permission("manage_users"),
                new Views.Permission("manage_courses"),
                new Views.Permission("view_analytics"))
            : List.<Views.Permission>of();
    return new Views.User(
        u.getId(),
        u.getUsername(),
        u.getEmail(),
        u.isActive(),
        false,
        u.getRole(),
        List.of(new Views.Role(admin ? 2 : 1, u.getRole(), permissions)),
        u.getCreatedAt());
  }
}
