package com.interviewai.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

public class ProgressGateway {
  private final ApiClient api = ApiClient.getInstance();

  private Map<String, Object> summary() {
    return api.get("/users/me/progress", new TypeReference<Map<String, Object>>() {});
  }

  private int metric(String name) {
    return ((Number) summary().get(name)).intValue();
  }

  public int getTotalXPForUser(int uid) {
    return metric("totalXP");
  }

  public int calculateUserStreak(int uid) {
    return metric("streak");
  }

  public int getQuestionsAnsweredToday(int uid) {
    return metric("questionsToday");
  }

  public int getQuestionsAnsweredCorrectlyToday(int uid) {
    return metric("correctToday");
  }

  public int getQuestionsAnsweredCorrectlyThisMonth(int uid) {
    return metric("correctThisMonth");
  }

  public int getLessonsCompletedByUser(int uid) {
    return metric("lessonsCompleted");
  }

  public int getChaptersCompletedThisMonth(int uid) {
    return metric("chaptersThisMonth");
  }

  public int getTotalQuestionsAnswered(int uid) {
    return metric("questionsAnswered");
  }

  public int getTotalQuestionsAvailable(int uid) {
    return metric("questionsAvailable");
  }

  public int getEstimatedTimeSpent(int uid) {
    return metric("estimatedMinutes");
  }

  public int getTotalCoursesEnrolled(int uid) {
    return metric("coursesEnrolled");
  }

  public double calculateAccuracy(int uid) {
    return ((Number) summary().get("accuracy")).doubleValue();
  }

  public Map<String, Integer> getLast7DaysXp(int uid) {
    return api.convert(summary().get("xpByDay"), new TypeReference<Map<String, Integer>>() {});
  }

  public Map<String, Boolean> getLast7DaysProgress(int uid) {
    return api.convert(summary().get("activeByDay"), new TypeReference<Map<String, Boolean>>() {});
  }

  public List<Map<String, Object>> getUserCourses(int uid) {
    return api.convert(summary().get("courses"), new TypeReference<List<Map<String, Object>>>() {});
  }

  public List<Map<String, Object>> getTopLearners(int limit) {
    return new UserGateway().getTopLearners(limit);
  }

  public record Quest(
      int id,
      String code,
      String title,
      int targetValue,
      int xpReward,
      String period,
      int currentValue,
      boolean claimed) {}

  public List<Quest> getQuests() {
    return api.get("/quests", new TypeReference<List<Quest>>() {});
  }

  private Quest quest(String code) {
    return getQuests().stream().filter(q -> q.code().equals(code)).findFirst().orElseThrow();
  }

  public boolean hasClaimedDailyQuest(int uid) {
    return quest("DAILY").claimed();
  }

  public boolean hasClaimedMonthlyQuest(int uid) {
    return quest("MONTHLY").claimed();
  }

  public void claimDailyQuest(int uid, int ignoredClientReward) {
    api.post("/quests/" + quest("DAILY").id() + "/claim", null, Integer.class);
  }

  public void claimMonthlyQuest(int uid, int ignoredClientReward) {
    api.post("/quests/" + quest("MONTHLY").id() + "/claim", null, Integer.class);
  }

  public List<Map<String, Object>> getDailyQuests(int uid) {
    return getQuests().stream()
        .map(
            q ->
                Map.<String, Object>of(
                    "quest_name",
                    q.title(),
                    "required_count",
                    q.targetValue(),
                    "current_count",
                    q.currentValue(),
                    "xp_reward",
                    q.xpReward()))
        .toList();
  }
}
