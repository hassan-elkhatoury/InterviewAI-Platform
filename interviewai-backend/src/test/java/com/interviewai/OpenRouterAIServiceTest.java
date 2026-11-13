package com.interviewai;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.interviewai.exception.ApiException;
import com.interviewai.service.OpenRouterAIService;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

class OpenRouterAIServiceTest {
  HttpServer server;
  OpenRouterAIService ai;
  ValidatorFactory validation;
  ObjectMapper mapper = new ObjectMapper();
  volatile int responseStatus = 200;
  volatile String response;
  AtomicReference<String> request = new AtomicReference<>();
  AtomicReference<String> authorization = new AtomicReference<>();

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/chat",
        exchange -> {
          request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(responseStatus, bytes.length);
          try (var output = exchange.getResponseBody()) {
            output.write(bytes);
          }
        });
    server.start();
    validation = Validation.buildDefaultValidatorFactory();
    ai = service("test-key");
  }

  OpenRouterAIService service(String key) {
    return new OpenRouterAIService(
        RestClient.builder(),
        mapper,
        validation.getValidator(),
        "http://127.0.0.1:" + server.getAddress().getPort() + "/chat",
        key,
        "test-model",
        Duration.ofSeconds(2));
  }

  void content(String value) throws Exception {
    response =
        mapper.writeValueAsString(
            Map.of("choices", java.util.List.of(Map.of("message", Map.of("content", value)))));
  }

  @AfterEach
  void close() {
    server.stop(0);
    validation.close();
  }

  @Test
  void validJsonIsParsedAndCredentialsStayInTheAuthorizationHeader() throws Exception {
    content(
        "```json\n"
            + "{\"score\":85,\"strengths\":\"You explained the idea\",\"improvements\":\"Add an"
            + " example\",\"explanation\":\"Details\"}\n"
            + "```");
    assertThat(ai.validateAnswer("Question", "Expected", "My answer").score()).isEqualTo(85);
    JsonNode sent = mapper.readTree(request.get());
    assertThat(sent.path("model").asText()).isEqualTo("test-model");
    assertThat(sent.path("messages").size()).isEqualTo(2);
    assertThat(authorization.get()).isEqualTo("Bearer test-key");
    assertThat(request.get()).doesNotContain("test-key");
  }

  @Test
  void malformedProviderJsonDoesNotProduceAPassingFallback() throws Exception {
    content("not json");
    assertFailure(HttpStatus.BAD_GATEWAY);
  }

  @Test
  void outOfRangeScoresAreRejected() throws Exception {
    content(
        "{\"score\":101,\"strengths\":\"Good\",\"improvements\":\"More\",\"explanation\":\"Why\"}");
    assertFailure(HttpStatus.BAD_GATEWAY);
  }

  @Test
  void missingFeedbackIsRejected() throws Exception {
    content("{\"score\":85}");
    assertFailure(HttpStatus.BAD_GATEWAY);
  }

  @Test
  void providerErrorsDoNotLeakTheirResponseBodies() {
    responseStatus = 429;
    response = "{\"error\":\"private-provider-detail\"}";
    assertThatThrownBy(() -> ai.validateAnswer("q", "expected", "answer"))
        .isInstanceOfSatisfying(
            ApiException.class,
            e -> {
              assertThat(e.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
              assertThat(e.getMessage()).doesNotContain("private-provider-detail");
            });
  }

  @Test
  void unconfiguredProviderReturnsAnExplicitFailure() {
    ai = service("");
    assertFailure(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(request.get()).isNull();
  }

  void assertFailure(HttpStatus expected) {
    assertThatThrownBy(() -> ai.validateAnswer("q", "expected", "answer"))
        .isInstanceOfSatisfying(
            ApiException.class, e -> assertThat(e.status()).isEqualTo(expected));
  }
}
