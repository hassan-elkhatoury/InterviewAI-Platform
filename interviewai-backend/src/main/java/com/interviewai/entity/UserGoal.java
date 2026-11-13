package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_goals")
public class UserGoal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "goal_name")
  private String goalName;

  @Column(name = "goal_type")
  private String goalType;

  @Column(name = "target_value")
  private int targetValue;

  @Column(name = "start_value")
  private int startValue;

  @Column(name = "current_value")
  private int currentValue;

  @Column(name = "is_active")
  private boolean active = true;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now(java.time.Clock.systemUTC());

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now(java.time.Clock.systemUTC());

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

  public String getGoalName() {
    return goalName;
  }

  public void setGoalName(String goalName) {
    this.goalName = goalName;
  }

  public String getGoalType() {
    return goalType;
  }

  public void setGoalType(String goalType) {
    this.goalType = goalType;
  }

  public int getTargetValue() {
    return targetValue;
  }

  public void setTargetValue(int targetValue) {
    this.targetValue = targetValue;
  }

  public int getStartValue() {
    return startValue;
  }

  public void setStartValue(int startValue) {
    this.startValue = startValue;
  }

  public int getCurrentValue() {
    return currentValue;
  }

  public void setCurrentValue(int currentValue) {
    this.currentValue = currentValue;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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
