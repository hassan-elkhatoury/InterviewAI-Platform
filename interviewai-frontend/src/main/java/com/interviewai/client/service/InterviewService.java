package com.interviewai.client.service;

import com.interviewai.client.api.*;

public class InterviewService {
  public java.util.concurrent.CompletableFuture<Evaluation> stream(
      int questionId, String answer, java.util.function.Consumer<String> tokens) {
    String encoded = java.net.URLEncoder.encode(answer, java.nio.charset.StandardCharsets.UTF_8);
    return ApiClient.getInstance().stream(
        "/interviews/evaluate/stream?questionId=" + questionId + "&userAnswer=" + encoded,
        tokens,
        Evaluation.class);
  }

  public record Evaluation(
      int feedbackId,
      int score,
      String strengths,
      String improvements,
      String explanation,
      String correctAnswer,
      String status,
      int xpAwarded) {}

  public Evaluation evaluate(int questionId, String answer) {
    return ApiClient.getInstance()
        .post(
            "/interviews/evaluate",
            ApiClient.body("questionId", questionId, "userAnswer", answer),
            Evaluation.class);
  }
}
