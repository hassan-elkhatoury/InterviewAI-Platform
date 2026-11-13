package com.interviewai.service;

import com.interviewai.dto.*;
import com.interviewai.entity.*;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {
  private final UserRepository users;
  private final OnboardingRepository onboarding;
  private final UserGoalRepository goals;
  private final PasswordEncoder passwords;
  private final CurriculumService curriculum;
  private final ProgressService progress;

  public ProfileService(
      UserRepository users,
      OnboardingRepository onboarding,
      UserGoalRepository goals,
      PasswordEncoder passwords,
      CurriculumService curriculum,
      ProgressService progress) {
    this.users = users;
    this.onboarding = onboarding;
    this.goals = goals;
    this.passwords = passwords;
    this.curriculum = curriculum;
    this.progress = progress;
  }

  public User user(int uid) {
    return users.findById(uid).orElseThrow(ApiException::notFound);
  }

  public Views.User profile(int uid) {
    return AuthService.view(user(uid));
  }

  public Views.User update(int uid, Requests.Profile request) {
    User user = user(uid);
    unique(uid, request.username(), request.email());
    user.setUsername(request.username());
    user.setEmail(request.email().toLowerCase(Locale.ROOT));
    return AuthService.view(users.save(user));
  }

  public void unique(int uid, String username, String email) {
    if (users.findByUsernameIgnoreCase(username).filter(u -> u.getId() != uid).isPresent()
        || users.findByEmailIgnoreCase(email).filter(u -> u.getId() != uid).isPresent())
      throw ApiException.conflict("Username or email is already registered");
  }

  public void changePassword(int uid, Requests.Password request) {
    User user = users.lockById(uid).orElseThrow(ApiException::notFound);
    if (!passwords.matches(request.currentPassword(), user.getPasswordHash()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
    user.setPasswordHash(passwords.encode(request.newPassword()));
    user.setTokenVersion(user.getTokenVersion() + 1);
    users.save(user);
  }

  public Integer[] lastUsed(int uid) {
    User u = user(uid);
    return new Integer[] {u.getLastCourseId(), u.getLastChapterId()};
  }

  public void lastUsed(int uid, Requests.LastUsed request) {
    if (request.courseId() != null) curriculum.ownedCourse(uid, request.courseId());
    if (request.chapterId() != null) {
      var lesson = curriculum.ownedLesson(uid, request.chapterId());
      if (!Objects.equals(lesson.getCourseId(), request.courseId()))
        throw new ApiException(
            HttpStatus.BAD_REQUEST, "Lesson does not belong to the selected course");
    }
    User user = user(uid);
    user.setLastCourseId(request.courseId());
    user.setLastChapterId(request.chapterId());
    users.save(user);
  }

  public Requests.Onboarding onboarding(int uid) {
    return onboarding
        .findById(uid)
        .map(
            o ->
                new Requests.Onboarding(
                    o.getInterviewType(),
                    o.getLanguage(),
                    o.getTimeline(),
                    o.getContext(),
                    o.getDescription()))
        .orElse(null);
  }

  public void onboarding(int uid, Requests.Onboarding request) {
    Onboarding o = onboarding.findById(uid).orElseGet(Onboarding::new);
    o.setUserId(uid);
    o.setInterviewType(request.interviewType());
    o.setLanguage(request.language());
    o.setTimeline(request.timeline());
    o.setContext(request.context());
    o.setDescription(request.description());
    o.setUpdatedAt(LocalDateTime.now(Clock.systemUTC()));
    onboarding.save(o);
  }

  private int metric(int uid, String type) {
    Map<String, Object> stats = progress.summary(uid);
    return ((Number)
            stats.get(
                switch (type) {
                  case "QUESTIONS" -> "questionsAnswered";
                  case "XP" -> "totalXP";
                  case "CHAPTERS" -> "lessonsCompleted";
                  case "TIME" -> "estimatedMinutes";
                  case "COURSES" -> "coursesEnrolled";
                  case "STREAK" -> "streak";
                  default -> throw new IllegalArgumentException("Unknown metric");
                }))
        .intValue();
  }

  public List<Map<String, Object>> goals(int uid) {
    return goals.findByUserIdAndActiveTrueOrderByCreatedAtAsc(uid).stream()
        .map(
            g -> {
              int value = metric(uid, g.getGoalType());
              if (!"STREAK".equals(g.getGoalType())) value = Math.max(0, value - g.getStartValue());
              return Map.<String, Object>of(
                  "id",
                  g.getId(),
                  "userId",
                  uid,
                  "goalName",
                  g.getGoalName(),
                  "goalType",
                  g.getGoalType(),
                  "targetValue",
                  g.getTargetValue(),
                  "currentValue",
                  value,
                  "startValue",
                  g.getStartValue(),
                  "isActive",
                  g.isActive(),
                  "createdAt",
                  g.getCreatedAt(),
                  "updatedAt",
                  g.getUpdatedAt());
            })
        .toList();
  }

  public int saveGoal(int uid, Integer id, Requests.Goal request) {
    UserGoal goal =
        id == null
            ? new UserGoal()
            : goals
                .findById(id)
                .filter(g -> g.getUserId() == uid && g.isActive())
                .orElseThrow(ApiException::notFound);
    if (id == null || !request.goalType().equals(goal.getGoalType()))
      goal.setStartValue("STREAK".equals(request.goalType()) ? 0 : metric(uid, request.goalType()));
    goal.setUserId(uid);
    goal.setGoalName(request.goalName());
    goal.setGoalType(request.goalType());
    goal.setTargetValue(request.targetValue());
    goal.setUpdatedAt(LocalDateTime.now(Clock.systemUTC()));
    return goals.save(goal).getId();
  }

  public void deleteGoal(int uid, int id) {
    UserGoal g =
        goals
            .findById(id)
            .filter(goal -> goal.getUserId() == uid)
            .orElseThrow(ApiException::notFound);
    g.setActive(false);
    goals.save(g);
  }
}
