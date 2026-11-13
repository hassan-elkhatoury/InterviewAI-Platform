package com.interviewai.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.interviewai.client.model.*;
import java.util.*;

public class QuestionGateway {
  private final ApiClient api = ApiClient.getInstance();

  public List<Question> getQuestionsByChapterId(int id) {
    return api.get("/lessons/" + id + "/questions", new TypeReference<List<Question>>() {});
  }

  public Chapter getChapterForQuestion(int id) {
    return api.get("/questions/" + id + "/lesson", Chapter.class);
  }

  public Question getQuestionById(int id) {
    return getChapterForQuestion(id).getQuestions().stream()
        .filter(q -> q.getId() == id)
        .findFirst()
        .orElseThrow();
  }

  private boolean needsReview(Question q) {
    return q.getStatus() == Question.QuestionStatus.INCORRECT
        || q.getStatus() == Question.QuestionStatus.IN_PROGRESS;
  }

  public List<Question> getIncorrectQuestionsByChapterId(int id) {
    return getQuestionsByChapterId(id).stream().filter(this::needsReview).toList();
  }

  public List<Question> getIncorrectQuestionsByCourseId(int id) {
    return new CourseProgressGateway()
        .getCourseById(id).getChapters().stream()
            .flatMap(c -> c.getQuestions().stream())
            .filter(this::needsReview)
            .toList();
  }
}
