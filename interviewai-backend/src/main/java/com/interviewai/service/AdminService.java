package com.interviewai.service;

import com.interviewai.dto.*;
import com.interviewai.entity.*;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {
  private final UserRepository users;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final QuestionRepository questions;
  private final OnboardingRepository onboarding;
  private final FeedbackRepository feedback;
  private final ProgressRepository events;
  private final QuestClaimRepository claims;
  private final AchievementRepository badges;
  private final ProgressService progress;
  private final AchievementService achievements;
  private final ProfileService profiles;
  private final PasswordEncoder passwords;

  public AdminService(
      UserRepository users,
      CourseRepository courses,
      LessonRepository lessons,
      QuestionRepository questions,
      OnboardingRepository onboarding,
      FeedbackRepository feedback,
      ProgressRepository events,
      QuestClaimRepository claims,
      AchievementRepository badges,
      ProgressService progress,
      AchievementService achievements,
      ProfileService profiles,
      PasswordEncoder passwords) {
    this.users = users;
    this.courses = courses;
    this.lessons = lessons;
    this.questions = questions;
    this.onboarding = onboarding;
    this.feedback = feedback;
    this.events = events;
    this.claims = claims;
    this.badges = badges;
    this.progress = progress;
    this.achievements = achievements;
    this.profiles = profiles;
    this.passwords = passwords;
  }

  public List<Map<String, Object>> users() {
    return users.findAll().stream().map(this::userView).toList();
  }

  public Views.User createUser(Requests.CreateUser request) {
    profiles.unique(-1, request.account().username(), request.account().email());
    User user = new User();
    user.setUsername(request.account().username());
    user.setEmail(request.account().email().toLowerCase(Locale.ROOT));
    user.setPasswordHash(passwords.encode(request.account().password()));
    user.setRole(request.role());
    return AuthService.view(users.saveAndFlush(user));
  }

  public Map<String, Object> user(int id) {
    return userView(profiles.user(id));
  }

  private Map<String, Object> userView(User u) {
    Map<String, Object> m = new LinkedHashMap<>();
    var stats = progress.summary(u.getId());
    var earned = achievements.list(u.getId());
    m.put("id", u.getId());
    m.put("username", u.getUsername());
    m.put("email", u.getEmail());
    m.put("role", u.getRole());
    m.put("roles", AuthService.view(u).roles());
    m.put("isActive", u.isActive());
    m.put("twoFactorEnabled", false);
    m.put("createdAt", u.getCreatedAt());
    m.put(
        "lastActive",
        events.findByUserId(u.getId()).stream()
            .map(Progress::getLastUpdated)
            .max(Comparator.naturalOrder())
            .orElse(null));
    onboarding
        .findById(u.getId())
        .ifPresent(
            o -> {
              m.put("interviewType", o.getInterviewType());
              m.put("language", o.getLanguage());
              m.put("timeline", o.getTimeline());
              m.put("context", o.getContext());
            });
    m.put("totalXP", stats.get("totalXP"));
    m.put("completedLessons", stats.get("lessonsCompleted"));
    m.put("enrolledCourses", stats.get("coursesEnrolled"));
    m.put("currentStreak", stats.get("streak"));
    m.put("quizAvgScore", stats.get("accuracy"));
    m.put("badgeCount", earned.size());
    m.put("badges", earned.stream().map(Views.Achievement::title).toList());
    return m;
  }

  public void updateUser(int actor, int id, Requests.AdminUser request) {
    User user = users.lockById(id).orElseThrow(ApiException::notFound);
    if (actor == id && (!request.active() || !"ROLE_ADMIN".equals(request.role())))
      throw ApiException.conflict("You cannot remove your own administrator access");
    profiles.unique(id, request.username(), request.email());
    if (user.isActive() != request.active() || !user.getRole().equals(request.role()))
      user.setTokenVersion(user.getTokenVersion() + 1);
    user.setUsername(request.username());
    user.setEmail(request.email().toLowerCase(Locale.ROOT));
    user.setRole(request.role());
    user.setActive(request.active());
    users.save(user);
  }

  public void password(int id, Requests.AdminPassword request) {
    User user = users.lockById(id).orElseThrow(ApiException::notFound);
    user.setPasswordHash(passwords.encode(request.newPassword()));
    user.setTokenVersion(user.getTokenVersion() + 1);
    users.save(user);
  }

  public void reset(int id, boolean all) {
    users.lockById(id).orElseThrow(ApiException::notFound);
    events.deleteAll(events.findByUserId(id));
    badges.deleteAll(badges.findByUserId(id));
    claims.deleteAll(claims.findAll().stream().filter(c -> c.getUserId() == id).toList());
    if (all) {
      feedback.deleteAll(feedback.findByUserIdOrderByCreatedAtDesc(id));
      for (Question q : progress.userQuestions(id)) {
        q.setStatus("NOT_STARTED");
        questions.save(q);
      }
      for (Lesson l : progress.userLessons(id)) {
        l.setStatus("NOT_STARTED");
        l.setCompletedAt(null);
        lessons.save(l);
      }
      for (Course c : courses.findByUserIdOrderByCreatedAtDesc(id)) {
        c.setStatus("ACTIVE");
        courses.save(c);
      }
    }
  }

  public List<Map<String, Object>> courses() {
    return courses.findAll().stream()
        .map(
            c -> {
              var ls = lessons.findByCourseIdOrderByChapterNumberAsc(c.getId());
              var owner = users.findById(c.getUserId());
              int done = (int) ls.stream().filter(l -> "COMPLETED".equals(l.getStatus())).count();
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("id", c.getId());
              m.put("userId", c.getUserId());
              m.put("username", owner.map(User::getUsername).orElse(""));
              m.put("userEmail", owner.map(User::getEmail).orElse(""));
              m.put("courseTitle", c.getCourseTitle());
              m.put("status", c.getStatus());
              m.put("createdAt", c.getCreatedAt());
              m.put("chaptersCount", ls.size());
              m.put("completedChapters", done);
              m.put(
                  "questionsCount",
                  ls.stream()
                      .mapToInt(l -> questions.findByChapterIdOrderByIdAsc(l.getId()).size())
                      .sum());
              m.put("completionPercentage", ls.isEmpty() ? 0.0 : 100.0 * done / ls.size());
              return m;
            })
        .toList();
  }

  public void courseStatus(int id, String status) {
    Course c = courses.findById(id).orElseThrow(ApiException::notFound);
    c.setStatus(status);
    courses.save(c);
  }

  public void deleteCourse(int id) {
    Course course = courses.findById(id).orElseThrow(ApiException::notFound);
    feedback.deleteAll(
        feedback.findByUserIdOrderByCreatedAtDesc(course.getUserId()).stream()
            .filter(f -> Objects.equals(f.getCourseId(), id))
            .toList());
    courses.delete(course);
  }

  private Map<String, Integer> distribution(
      List<Onboarding> data, Function<Onboarding, String> key) {
    return data.stream()
        .map(key)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Function.identity(), s -> 1, Integer::sum, TreeMap::new));
  }

  public Map<String, Object> analytics() {
    var us = users.findAll();
    var cs = courses.findAll();
    var ls = lessons.findAll();
    var fs = feedback.findAll();
    var ps = events.findAll();
    var os = onboarding.findAll();
    Map<String, Object> m = new LinkedHashMap<>();
    LocalDate today = ProgressService.today();
    m.put("totalUsers", us.size());
    m.put("activeUsers", (int) us.stream().filter(User::isActive).count());
    m.put(
        "dailySignups",
        (int) us.stream().filter(u -> !u.getCreatedAt().toLocalDate().isBefore(today)).count());
    m.put(
        "weeklySignups",
        (int)
            us.stream()
                .filter(u -> !u.getCreatedAt().toLocalDate().isBefore(today.minusDays(6)))
                .count());
    m.put(
        "monthlySignups",
        (int)
            us.stream()
                .filter(u -> !u.getCreatedAt().toLocalDate().isBefore(today.withDayOfMonth(1)))
                .count());
    m.put("totalCourses", cs.size());
    m.put(
        "interviewsCompleted",
        (int) cs.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count());
    m.put(
        "completedChapters",
        (int) ls.stream().filter(l -> "COMPLETED".equals(l.getStatus())).count());
    m.put(
        "inProgressChapters",
        (int) ls.stream().filter(l -> "IN_PROGRESS".equals(l.getStatus())).count());
    m.put(
        "averageScore",
        fs.stream()
            .filter(f -> f.getScore() != null)
            .mapToInt(Feedback::getScore)
            .average()
            .orElse(0));
    var types = distribution(os, Onboarding::getInterviewType);
    var languages = distribution(os, Onboarding::getLanguage);
    m.put("interviewTypeDistribution", types);
    m.put("languageDistribution", languages);
    m.put(
        "mostCommonInterviewType",
        types.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A"));
    m.put(
        "mostSelectedLanguage",
        languages.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A"));
    List<Map<String, Object>> signups = new ArrayList<>(), usage = new ArrayList<>();
    for (int i = 29; i >= 0; i--) {
      LocalDate day = today.minusDays(i);
      signups.add(
          Map.of(
              "date",
              day,
              "count",
              (int) us.stream().filter(u -> u.getCreatedAt().toLocalDate().equals(day)).count()));
      usage.add(
          Map.of(
              "date",
              day,
              "count",
              (int)
                  ps.stream()
                      .filter(p -> p.getLastUpdated().toLocalDate().equals(day))
                      .map(Progress::getUserId)
                      .distinct()
                      .count()));
    }
    m.put("signupTrend", signups);
    m.put("usageTrend", usage);
    m.put("totalFeedback", fs.size());
    m.put(
        "recentFeedback",
        fs.stream()
            .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
            .limit(5)
            .map(
                f -> {
                  Map<String, Object> item = new LinkedHashMap<>();
                  item.put("id", f.getId());
                  item.put("message", f.getMessage());
                  item.put("created_at", f.getCreatedAt());
                  item.put(
                      "username",
                      users.findById(f.getUserId()).map(User::getUsername).orElse("Unknown"));
                  return item;
                })
            .toList());
    m.put(
        "totalXPToday",
        ps.stream()
            .filter(p -> p.getLastUpdated().toLocalDate().equals(today))
            .mapToInt(Progress::getXp)
            .sum());
    m.put(
        "totalXPWeek",
        ps.stream()
            .filter(p -> !p.getLastUpdated().toLocalDate().isBefore(today.minusDays(6)))
            .mapToInt(Progress::getXp)
            .sum());
    m.put(
        "dailyActiveUsers",
        (int)
            ps.stream()
                .filter(p -> p.getLastUpdated().toLocalDate().equals(today))
                .map(Progress::getUserId)
                .distinct()
                .count());
    m.put(
        "weeklyActiveUsers",
        (int)
            ps.stream()
                .filter(p -> !p.getLastUpdated().toLocalDate().isBefore(today.minusDays(6)))
                .map(Progress::getUserId)
                .distinct()
                .count());
    Runtime runtime = Runtime.getRuntime();
    long used = runtime.totalMemory() - runtime.freeMemory();
    m.put(
        "systemHealth",
        Map.of(
            "dbConnectionStatus",
            "Healthy",
            "memoryUsage",
            used / 1048576 + "MB / " + runtime.maxMemory() / 1048576 + "MB",
            "memoryPercent",
            (int) (100 * used / runtime.maxMemory())));
    return m;
  }
}
