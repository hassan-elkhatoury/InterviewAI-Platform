package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "username")
  private String username;

  @Column(name = "email")
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "role")
  private String role = "ROLE_USER";

  @Column(name = "is_active")
  private boolean active = true;

  @Column(name = "two_factor_enabled")
  private boolean twoFactorEnabled;

  @Column(name = "token_version")
  private int tokenVersion;

  @Column(name = "last_course_id")
  private Integer lastCourseId;

  @Column(name = "last_chapter_id")
  private Integer lastChapterId;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now(java.time.Clock.systemUTC());

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isTwoFactorEnabled() {
    return twoFactorEnabled;
  }

  public void setTwoFactorEnabled(boolean twoFactorEnabled) {
    this.twoFactorEnabled = twoFactorEnabled;
  }

  public int getTokenVersion() {
    return tokenVersion;
  }

  public void setTokenVersion(int tokenVersion) {
    this.tokenVersion = tokenVersion;
  }

  public Integer getLastCourseId() {
    return lastCourseId;
  }

  public void setLastCourseId(Integer lastCourseId) {
    this.lastCourseId = lastCourseId;
  }

  public Integer getLastChapterId() {
    return lastChapterId;
  }

  public void setLastChapterId(Integer lastChapterId) {
    this.lastChapterId = lastChapterId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
