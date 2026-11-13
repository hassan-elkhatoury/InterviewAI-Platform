package com.interviewai.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.interviewai.client.model.*;
import java.util.*;

public class CourseProgressGateway {
  private final ApiClient api = ApiClient.getInstance();

  public Optional<GeneratedCourse> findLatestActiveCourseWithChapters(int uid) {
    List<GeneratedCourse> courses = new CourseGateway().getCourses();
    return courses.stream()
        .filter(c -> "ACTIVE".equals(c.getStatus()))
        .findFirst()
        .or(() -> courses.stream().findFirst());
  }

  public List<Question> getQuestionsForChapter(int id) {
    return api.get("/lessons/" + id + "/questions", new TypeReference<List<Question>>() {});
  }

  public Optional<Chapter> findChapterById(int id) {
    return Optional.of(api.get("/lessons/" + id, Chapter.class));
  }

  public boolean areAllQuestionsCompleted(int id) {
    Chapter c = findChapterById(id).orElseThrow();
    return c.getTotalQuestions() > 0 && c.getCompletedQuestions() == c.getTotalQuestions();
  }

  public Map<String, Integer> refreshChapterCounts(int id) {
    Chapter c = findChapterById(id).orElseThrow();
    return Map.of("total", c.getTotalQuestions(), "completed", c.getCompletedQuestions());
  }

  public GeneratedCourse getCourseById(int id) {
    return api.get("/courses/" + id, GeneratedCourse.class);
  }
}
