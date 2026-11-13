package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_prep")
public class InterviewPrep {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "technical_course_id")
  private Integer technicalCourseId;

  @Column(name = "softskills_course_id")
  private Integer softskillsCourseId;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now(java.time.Clock.systemUTC());

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

  public Integer getTechnicalCourseId() {
    return technicalCourseId;
  }

  public void setTechnicalCourseId(Integer technicalCourseId) {
    this.technicalCourseId = technicalCourseId;
  }

  public Integer getSoftskillsCourseId() {
    return softskillsCourseId;
  }

  public void setSoftskillsCourseId(Integer softskillsCourseId) {
    this.softskillsCourseId = softskillsCourseId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
