package com.interviewai.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class Views {
  private Views() {}

  public record Permission(String name) {}

  public record Role(int id, String name, List<Permission> permissions) {}

  public record User(
      int id,
      String username,
      String email,
      boolean isActive,
      boolean twoFactorEnabled,
      String role,
      List<Role> roles,
      LocalDateTime createdAt) {}

  public record Auth(String accessToken, String tokenType, long expiresIn, User user) {}

  public record Question(
      int id,
      String question,
      String questionType,
      List<String> choices,
      String correctAnswer,
      String explanation,
      String status) {}

  public record Lesson(
      int id,
      int courseId,
      int chapterNumber,
      String name,
      String description,
      String status,
      List<Question> questions,
      int totalQuestions,
      int completedQuestions) {}

  public record Course(
      int id,
      int userId,
      String courseTitle,
      String status,
      LocalDateTime createdAt,
      List<Lesson> chapters,
      double completionPercentage) {}

  public record Evaluation(
      int feedbackId,
      int score,
      String strengths,
      String improvements,
      String explanation,
      String correctAnswer,
      String status,
      int xpAwarded) {}

  public record Quest(
      int id,
      String code,
      String title,
      int targetValue,
      int xpReward,
      String period,
      int currentValue,
      boolean claimed) {}

  public record Achievement(String code, String title, LocalDateTime earnedAt) {}
}
