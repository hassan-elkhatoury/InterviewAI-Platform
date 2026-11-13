package com.interviewai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class Requests {
  private Requests() {}

  public record Register(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,100}") String username,
      @NotBlank @Email @Size(max = 255) String email,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record Login(
      @NotBlank @Size(max = 255) String username, @NotBlank @Size(max = 72) String password) {}

  public record Evaluate(
      @NotNull @Positive Integer questionId, @NotBlank @Size(max = 12000) String userAnswer) {}

  public record Profile(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,100}") String username,
      @NotBlank @Email @Size(max = 255) String email) {}

  public record Password(
      @NotBlank @Size(max = 72) String currentPassword,
      @NotBlank @Size(min = 8, max = 72) String newPassword) {}

  public record AdminPassword(@NotBlank @Size(min = 8, max = 72) String newPassword) {}

  public record CreateUser(
      @NotNull @Valid Register account,
      @NotBlank @Pattern(regexp = "ROLE_USER|ROLE_ADMIN") String role) {}

  public record LastUsed(@Positive Integer courseId, @Positive Integer chapterId) {}

  public record Status(@NotBlank @Pattern(regexp = "ACTIVE|COMPLETED|ARCHIVED") String status) {}

  public record AdminUser(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,100}") String username,
      @NotBlank @Email @Size(max = 255) String email,
      @NotNull Boolean active,
      @NotBlank @Pattern(regexp = "ROLE_USER|ROLE_ADMIN") String role) {}

  public record Goal(
      @NotBlank @Size(max = 255) String goalName,
      @NotBlank @Pattern(regexp = "QUESTIONS|XP|CHAPTERS|TIME|COURSES|STREAK") String goalType,
      @Min(1) @Max(10000000) int targetValue) {}

  public record Onboarding(
      @NotBlank @Pattern(regexp = "JOB|VISA|INTERNSHIP|UNIVERSITY") String interviewType,
      @NotBlank @Pattern(regexp = "ENGLISH|FRENCH|ARABIC|SPANISH") String language,
      @NotBlank @Pattern(regexp = "TOMORROW|THIS_WEEK|LATER") String timeline,
      @Size(max = 12000) String context,
      @Size(max = 12000) String description) {}

  public record Generate(
      @NotNull @Valid Onboarding preferences, @Size(max = 40000) String cvText) {}

  public record GeneratedCourse(
      @NotBlank @Size(max = 255) String courseTitle,
      @NotEmpty @Size(max = 20) List<@Valid GeneratedLesson> chapters) {}

  public record GeneratedLesson(
      @NotBlank @Size(max = 255) String name,
      @NotBlank @Size(max = 12000) String description,
      @NotEmpty @Size(max = 30) List<@Valid GeneratedQuestion> questions) {}

  public record GeneratedQuestion(
      @NotBlank @Size(max = 12000) String question,
      @NotBlank @Pattern(regexp = "MULTIPLE_CHOICE|SHORT_ANSWER") String questionType,
      @NotNull @Size(max = 8) List<@NotBlank @Size(max = 255) String> choices,
      @NotBlank @Size(max = 255) String correctAnswer,
      @NotBlank @Size(max = 12000) String explanation) {}
}
