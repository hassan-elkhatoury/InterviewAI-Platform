package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievements")
public class Achievement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "code")
  private String code;

  @Column(name = "title")
  private String title;

  @Column(name = "earned_at")
  private LocalDateTime earnedAt = LocalDateTime.now(java.time.Clock.systemUTC());

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDateTime getEarnedAt() {
    return earnedAt;
  }

  public void setEarnedAt(LocalDateTime earnedAt) {
    this.earnedAt = earnedAt;
  }
}
