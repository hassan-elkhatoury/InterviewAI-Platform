package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_data")
public class Onboarding {
  @Id
  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "interview_type")
  private String interviewType;

  @Column(name = "language")
  private String language;

  @Column(name = "timeline")
  private String timeline;

  @Column(name = "context", columnDefinition = "TEXT")
  private String context;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "cv_path")
  private String cvPath;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now(java.time.Clock.systemUTC());

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now(java.time.Clock.systemUTC());

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getInterviewType() {
    return interviewType;
  }

  public void setInterviewType(String interviewType) {
    this.interviewType = interviewType;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getTimeline() {
    return timeline;
  }

  public void setTimeline(String timeline) {
    this.timeline = timeline;
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCvPath() {
    return cvPath;
  }

  public void setCvPath(String cvPath) {
    this.cvPath = cvPath;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
