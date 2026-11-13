package com.interviewai.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "choices")
public class Choice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "question_id")
  private Integer questionId;

  @Column(name = "choice_text")
  private String choiceText;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getQuestionId() {
    return questionId;
  }

  public void setQuestionId(Integer questionId) {
    this.questionId = questionId;
  }

  public String getChoiceText() {
    return choiceText;
  }

  public void setChoiceText(String choiceText) {
    this.choiceText = choiceText;
  }
}
