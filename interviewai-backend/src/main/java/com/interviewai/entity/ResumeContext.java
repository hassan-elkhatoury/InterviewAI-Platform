package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "resume_context")
public class ResumeContext {
  @Id private Integer userId;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String fullText;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String profileJson;

  private LocalDateTime updatedAt;

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer value) {
    userId = value;
  }

  public String getFullText() {
    return fullText;
  }

  public void setFullText(String value) {
    fullText = value;
  }

  public String getProfileJson() {
    return profileJson;
  }

  public void setProfileJson(String value) {
    profileJson = value;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime value) {
    updatedAt = value;
  }
}
