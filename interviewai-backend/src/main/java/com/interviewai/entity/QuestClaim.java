package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quest_claims")
public class QuestClaim {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "quest_id")
  private Integer questId;

  @Column(name = "period_start")
  private LocalDate periodStart;

  @Column(name = "claimed_at")
  private LocalDateTime claimedAt = LocalDateTime.now(java.time.Clock.systemUTC());

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

  public Integer getQuestId() {
    return questId;
  }

  public void setQuestId(Integer questId) {
    this.questId = questId;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public void setPeriodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
  }

  public LocalDateTime getClaimedAt() {
    return claimedAt;
  }

  public void setClaimedAt(LocalDateTime claimedAt) {
    this.claimedAt = claimedAt;
  }
}
