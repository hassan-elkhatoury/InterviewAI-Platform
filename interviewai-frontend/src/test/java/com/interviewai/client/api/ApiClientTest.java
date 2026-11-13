package com.interviewai.client.api;

import static org.junit.jupiter.api.Assertions.*;

import com.interviewai.client.model.*;
import com.interviewai.client.service.InterviewService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;

class ApiClientTest {
  HttpServer server;
  ApiClient client;
  volatile int status = 200;
  volatile String contentType = "application/json";
  volatile String response = "{\"success\":true,\"data\":null}";
  AtomicReference<String> authorization = new AtomicReference<>();
  AtomicReference<String> body = new AtomicReference<>();

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/v1/",
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", contentType);
          exchange.sendResponseHeaders(status, bytes.length);
          try (var output = exchange.getResponseBody()) {
            output.write(bytes);
          }
        });
    server.start();
    client = new ApiClient("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
  }

  @AfterEach
  void close() {
    server.stop(0);
  }

  @Test
  void attachesBearerAndDeserializesAuthenticationRecords() {
    response =
        """
        {"success":true,"data":{"accessToken":"signed","tokenType":"Bearer","expiresIn":3600,
        "user":{"id":1,"username":"alice","email":"alice@example.com","isActive":true,
        "roles":[{"id":2,"name":"ROLE_ADMIN","permissions":[{"name":"manage_users"}]}]}}}
        """;
    client.setToken("existing");
    var auth =
        client.post(
            "/auth/login",
            ApiClient.body("username", "alice", "password", "pass"),
            UserGateway.AuthResult.class);
    assertEquals("Bearer existing", authorization.get());
    assertTrue(body.get().contains("alice"));
    assertEquals("signed", auth.accessToken());
    assertTrue(auth.user().isActive());
    assertTrue(auth.user().hasPermission("manage_users"));
  }

  @Test
  void authorizationFailureClearsTheInMemoryToken() {
    client.setToken("expired");
    status = 401;
    response = "{\"success\":false,\"message\":\"Authentication required\"}";
    ApiException failure =
        assertThrows(ApiException.class, () -> client.get("/users/me", User.class));
    assertEquals(401, failure.getStatus());
    status = 200;
    response = "{\"success\":true,\"data\":null}";
    client.get("/users/me", User.class);
    assertNull(authorization.get());
  }

  @Test
  void courseAndQuestionFieldsSurviveTheJsonBoundary() {
    response =
        """
{"success":true,"data":{"id":1,"userId":2,"courseTitle":"Java","status":"ACTIVE",
"createdAt":"2026-11-13T10:30:00","chapters":[{"id":3,"chapterNumber":1,"name":"Basics",
"status":"IN_PROGRESS","totalQuestions":1,"completedQuestions":0,"questions":[{"id":4,
"question":"Question?","questionType":"SHORT_ANSWER","status":"INCORRECT","choices":[]}]}]}}
""";
    GeneratedCourse course = client.get("/courses/1", GeneratedCourse.class);
    assertEquals(2026, course.getCreatedAt().getYear());
    assertEquals(Chapter.ChapterStatus.IN_PROGRESS, course.getChapters().get(0).getStatus());
    assertEquals(
        Question.QuestionType.SHORT_ANSWER,
        course.getChapters().get(0).getQuestions().get(0).getQuestionType());
  }

  @Test
  void handlesMalformedResponsesAndServerErrors() {
    response = "not json";
    assertThrows(ApiException.class, () -> client.get("/users/me", User.class));
    status = 503;
    response = "{\"success\":false,\"message\":\"AI service is not configured\"}";
    assertEquals(
        "AI service is not configured",
        assertThrows(
                ApiException.class,
                () ->
                    client.post(
                        "/interviews/evaluate",
                        ApiClient.body("questionId", 1, "userAnswer", "answer"),
                        InterviewService.Evaluation.class))
            .getMessage());
  }

  @Test
  void streamDecodesTokensAndRequiresAFinalResult() throws Exception {
    contentType = "text/event-stream";
    response =
        ": heartbeat\n\n"
            + "event: token\n"
            + "data: {\"text\":\"Hello \\n"
            + "\"}\n\n"
            + "event: token\n"
            + "data: {\"text\":\"Café\"}\n\n"
            + "event: result\n"
            + "data: {\"score\":85,\"xpAwarded\":10}\n\n";
    StringBuilder tokens = new StringBuilder();
    client.setToken("stream-token");
    var result =
        client.stream(
                "/interviews/evaluate/stream?questionId=1&userAnswer=a",
                tokens::append,
                InterviewService.Evaluation.class)
            .get(5, java.util.concurrent.TimeUnit.SECONDS);
    assertEquals("Hello \nCafé", tokens.toString());
    assertEquals(85, result.score());
    assertEquals("Bearer stream-token", authorization.get());
    response = "event: token\ndata: {\"text\":\"Partial\"}\n\n";
    assertThrows(
        java.util.concurrent.ExecutionException.class,
        () ->
            client.stream("/stream", s -> {}, InterviewService.Evaluation.class)
                .get(5, java.util.concurrent.TimeUnit.SECONDS));
  }

  @Test
  void uploadsPdfAsMultipartWithoutExposingItsLocalPath() throws Exception {
    java.nio.file.Path pdf = java.nio.file.Files.createTempFile("private-resume", ".pdf");
    try {
      java.nio.file.Files.writeString(pdf, "%PDF-1.7 test");
      response =
          "{\"success\":true,\"data\":{\"skills\":[\"Java\"],\"seniority\":\"Senior\",\"experience\":\"APIs\",\"tracks\":[\"Backend\"]}}";
      var profile = client.uploadResume(pdf).get(5, java.util.concurrent.TimeUnit.SECONDS);
      assertEquals("Java", profile.skills().get(0));
      assertTrue(body.get().contains("name=\"file\""));
      assertTrue(body.get().contains("%PDF-1.7 test"));
      assertFalse(body.get().contains(pdf.toString()));
    } finally {
      java.nio.file.Files.deleteIfExists(pdf);
    }
  }

  @Test
  void logoutCancelsAnActiveStreamWithoutWaitingForTheServer() throws Exception {
    var release = new java.util.concurrent.CountDownLatch(1);
    var tokenReceived = new java.util.concurrent.CountDownLatch(1);
    server.createContext(
        "/api/v1/pending",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
          exchange.sendResponseHeaders(200, 0);
          try (var output = exchange.getResponseBody()) {
            output.write(
                "event:token\ndata:{\"text\":\"Live\"}\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
            try {
              release.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        });
    try {
      client.setToken("signed");
      var result =
          client.stream(
              "/pending", text -> tokenReceived.countDown(), InterviewService.Evaluation.class);
      assertTrue(tokenReceived.await(3, java.util.concurrent.TimeUnit.SECONDS));
      assertFalse(result.isDone());
      client.clearToken();
      assertTrue(result.isCancelled());
    } finally {
      release.countDown();
    }
  }
}
