package com.interviewai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quests")
public class Quest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "code")
  private String code;

  @Column(name = "title")
  private String title;

  @Column(name = "target_value")
  private int targetValue;

  @Column(name = "xp_reward")
  private int xpReward;

  @Column(name = "period")
  private String period;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

  public int getTargetValue() {
    return targetValue;
  }

  public void setTargetValue(int targetValue) {
    this.targetValue = targetValue;
  }

  public int getXpReward() {
    return xpReward;
  }

  public void setXpReward(int xpReward) {
    this.xpReward = xpReward;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }
}
