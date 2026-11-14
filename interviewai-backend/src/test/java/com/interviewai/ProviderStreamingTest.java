package com.interviewai;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.exception.ApiException;
import com.interviewai.service.*;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

class ProviderStreamingTest {
  HttpServer server;
  AIService ai;
  ObjectMapper mapper = new ObjectMapper();
  volatile String response;
  volatile String received;
  volatile String tail = "";
  volatile CountDownLatch finishGate;

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/chat",
        exchange -> {
          received = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
          exchange.sendResponseHeaders(200, 0);
          try (var out = exchange.getResponseBody()) {
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            if (finishGate != null)
              try {
                finishGate.await(5, TimeUnit.SECONDS);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            out.write(tail.getBytes(StandardCharsets.UTF_8));
            out.flush();
          }
        });
    server.start();
    try (var validation = Validation.buildDefaultValidatorFactory()) {
      ai =
          new AIService(
              RestClient.builder(),
              mapper,
              validation.getValidator(),
              "http://127.0.0.1:" + server.getAddress().getPort() + "/chat",
              "key",
              "model",
              Duration.ofSeconds(5));
    }
  }

  @AfterEach
  void close() {
    server.stop(0);
  }

  String delta(String text) throws Exception {
    return "data: "
        + mapper.writeValueAsString(
            Map.of("choices", List.of(Map.of("delta", Map.of("content", text)))))
        + "\n\n";
  }

  @Test
  void parsesFragmentedEscapedCritiqueAndFinalGrade() throws Exception {
    String grade =
        "{\"explanation\":\"You said \\\"Java\\\".\\n"
            + "Caf\\u00e9\",\"score\":85,\"strengths\":\"Good\",\"improvements\":\"Add examples\"}";
    StringBuilder wire = new StringBuilder(": heartbeat\n\n");
    // Split inside escape sequences, Unicode escapes, property names and words.
    for (char c : grade.toCharArray()) wire.append(delta(String.valueOf(c)));
    response = wire + "data: {\"choices\":[]}\n\ndata: [DONE]\n\n";
    List<String> tokens = new ArrayList<>();
    var result = ai.streamGrade("q", "expected", "answer", tokens::add, new StreamControl());
    assertThat(result.score()).isEqualTo(85);
    assertThat(tokens.size()).isGreaterThan(5);
    assertThat(String.join("", tokens)).isEqualTo("You said \"Java\".\nCafé");
    assertThat(mapper.readTree(received).path("stream").asBoolean()).isTrue();
  }

  @Test
  void rejectsTruncatedStreams() throws Exception {
    response = delta("{\"explanation\":\"Partial");
    assertThatThrownBy(() -> ai.streamGrade("q", "a", "a", s -> {}, new StreamControl()))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsProviderErrorsAndOutOfRangeScores() throws Exception {
    response = "data: {\"error\":{\"message\":\"private\"}}\n\n";
    assertThatThrownBy(() -> ai.streamGrade("q", "a", "a", s -> {}, new StreamControl()))
        .isInstanceOf(ApiException.class)
        .hasMessageNotContaining("private");
    response =
        delta("{\"explanation\":\"Text\",\"score\":101,\"strengths\":\"a\",\"improvements\":\"b\"}")
            + "data: [DONE]\n\n";
    assertThatThrownBy(() -> ai.streamGrade("q", "a", "a", s -> {}, new StreamControl()))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void cancellationClosesTheUpstreamBody() throws Exception {
    var control = new StreamControl();
    var closed = new java.util.concurrent.atomic.AtomicBoolean();
    control.body(
        new java.io.ByteArrayInputStream(new byte[0]) {
          public void close() {
            closed.set(true);
          }
        });
    control.close();
    assertThat(closed).isTrue();
    assertThatThrownBy(control::check).isInstanceOf(CancellationException.class);
  }

  @Test
  void critiqueArrivesBeforeProviderFinishes() throws Exception {
    response = delta("{\"explanation\":\"Live critique");
    tail =
        delta("\",\"score\":80,\"strengths\":\"Good\",\"improvements\":\"Examples\"}")
            + "data: [DONE]\n\n";
    finishGate = new CountDownLatch(1);
    var firstToken = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();
    try {
      var result =
          executor.submit(
              () ->
                  ai.streamGrade(
                      "q", "a", "a", text -> firstToken.countDown(), new StreamControl()));
      assertThat(firstToken.await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(result.isDone()).isFalse();
      finishGate.countDown();
      assertThat(result.get(3, TimeUnit.SECONDS).score()).isEqualTo(80);
    } finally {
      finishGate.countDown();
      executor.shutdownNow();
    }
  }
}
