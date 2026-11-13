package com.interviewai.service;

import com.interviewai.entity.*;
import com.interviewai.repository.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgressService {
  private final ProgressRepository progress;
  private final FeedbackRepository feedback;
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final QuestionRepository questions;
  private final UserRepository users;

  public ProgressService(
      ProgressRepository progress,
      FeedbackRepository feedback,
      CourseRepository courses,
      LessonRepository lessons,
      QuestionRepository questions,
      UserRepository users) {
    this.progress = progress;
    this.feedback = feedback;
    this.courses = courses;
    this.lessons = lessons;
    this.questions = questions;
    this.users = users;
  }

  public static LocalDate today() {
    return LocalDate.now(Clock.systemUTC());
  }

  public List<Lesson> userLessons(int userId) {
    return courses.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .flatMap(c -> lessons.findByCourseIdOrderByChapterNumberAsc(c.getId()).stream())
        .toList();
  }

  public List<Question> userQuestions(int userId) {
    return userLessons(userId).stream()
        .flatMap(l -> questions.findByChapterIdOrderByIdAsc(l.getId()).stream())
        .toList();
  }

  public int answeredSince(int userId, LocalDate start, boolean correct) {
    return (int)
        feedback.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(
                f -> f.getQuestionId() != null && !f.getCreatedAt().toLocalDate().isBefore(start))
            .filter(f -> !correct || (f.getScore() != null && f.getScore() >= 70))
            .map(Feedback::getQuestionId)
            .distinct()
            .count();
  }

  public int completedThisMonth(int userId) {
    LocalDate month = today().withDayOfMonth(1);
    return (int)
        userLessons(userId).stream()
            .filter(
                l ->
                    "COMPLETED".equals(l.getStatus())
                        && l.getCompletedAt() != null
                        && !l.getCompletedAt().toLocalDate().isBefore(month))
            .count();
  }

  public int totalXP(int userId) {
    return progress.findByUserId(userId).stream().mapToInt(Progress::getXp).sum();
  }

  public int streak(int userId) {
    Set<LocalDate> days = new HashSet<>();
    progress.findByUserId(userId).stream()
        .filter(p -> p.getXp() > 0)
        .forEach(p -> days.add(p.getLastUpdated().toLocalDate()));
    LocalDate day = days.contains(today()) ? today() : today().minusDays(1);
    int count = 0;
    while (days.contains(day)) {
      count++;
      day = day.minusDays(1);
    }
    return count;
  }

  public Map<String, Object> summary(int userId) {
    var events = progress.findByUserId(userId);
    var chapters = userLessons(userId);
    var items = userQuestions(userId);
    var enrolled = courses.findByUserIdOrderByCreatedAtDesc(userId);
    int answered = (int) items.stream().filter(q -> !"NOT_STARTED".equals(q.getStatus())).count();
    int correct = (int) items.stream().filter(q -> "COMPLETED".equals(q.getStatus())).count();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("totalXP", events.stream().mapToInt(Progress::getXp).sum());
    result.put("streak", streak(userId));
    result.put("questionsToday", answeredSince(userId, today(), false));
    result.put("correctToday", answeredSince(userId, today(), true));
    result.put("correctThisMonth", answeredSince(userId, today().withDayOfMonth(1), true));
    result.put(
        "lessonsCompleted",
        (int) chapters.stream().filter(l -> "COMPLETED".equals(l.getStatus())).count());
    result.put("chaptersThisMonth", completedThisMonth(userId));
    result.put("questionsAnswered", answered);
    result.put("questionsAvailable", items.size());
    result.put("estimatedMinutes", answered * 2);
    result.put("coursesEnrolled", enrolled.size());
    result.put("accuracy", answered == 0 ? 0.0 : 100.0 * correct / answered);
    Map<String, Integer> xpDays = new LinkedHashMap<>();
    Map<String, Boolean> activeDays = new LinkedHashMap<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate date = today().minusDays(i);
      String name = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
      int xp =
          events.stream()
              .filter(e -> e.getLastUpdated().toLocalDate().equals(date))
              .mapToInt(Progress::getXp)
              .sum();
      xpDays.put(name, xp);
      activeDays.put(name, xp > 0);
    }
    result.put("xpByDay", xpDays);
    result.put("activeByDay", activeDays);
    result.put(
        "courses",
        enrolled.stream()
            .map(
                c -> {
                  var ls = lessons.findByCourseIdOrderByChapterNumberAsc(c.getId());
                  return Map.<String, Object>of(
                      "course_id",
                      c.getId(),
                      "course_title",
                      c.getCourseTitle(),
                      "user_xp",
                      events.stream()
                          .filter(e -> e.getCourseId().equals(c.getId()))
                          .mapToInt(Progress::getXp)
                          .sum(),
                      "progress_percentage",
                      ls.isEmpty()
                          ? 0
                          : (int)
                              (100
                                  * ls.stream()
                                      .filter(l -> "COMPLETED".equals(l.getStatus()))
                                      .count()
                                  / ls.size()));
                })
            .toList());
    return result;
  }

  public List<Map<String, Object>> leaderboard() {
    var sorted =
        users.findAll().stream()
            .filter(User::isActive)
            .sorted(Comparator.comparingInt((User u) -> totalXP(u.getId())).reversed())
            .limit(20)
            .toList();
    List<Map<String, Object>> result = new ArrayList<>();
    int rank = 0;
    for (User user : sorted)
      result.add(
          Map.of(
              "rank",
              ++rank,
              "user_id",
              user.getId(),
              "username",
              user.getUsername(),
              "total_xp",
              totalXP(user.getId())));
    return result;
  }
}
