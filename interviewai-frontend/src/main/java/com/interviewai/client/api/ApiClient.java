package com.interviewai.client.api;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** HTTP transport for the desktop client. Tokens remain in memory and are cleared on logout. */
public final class ApiClient {
  private static final ApiClient INSTANCE =
      new ApiClient(
          System.getenv().getOrDefault("INTERVIEWAI_API_URL", "http://localhost:8080/api/v1"));
  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final String baseUrl;
  private volatile String token;
  private static final ExecutorService NETWORK =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "api-network");
            t.setDaemon(true);
            return t;
          });
  private final Set<CompletableFuture<?>> activeStreams = ConcurrentHashMap.newKeySet();
  private final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
          .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
          .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
          .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

  public ApiClient(String baseUrl) {
    URI uri = URI.create(baseUrl);
    if (!List.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null)
      throw new IllegalArgumentException("INTERVIEWAI_API_URL must be an HTTP(S) base URL");
    this.baseUrl = baseUrl.replaceAll("/+$", "");
  }

  public static ApiClient getInstance() {
    return INSTANCE;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void clearToken() {
    token = null;
    activeStreams.forEach(f -> f.cancel(true));
  }

  public CompletableFuture<ResumeProfile> uploadResume(Path file) {
    String accessToken = token;
    return CompletableFuture.supplyAsync(
            () -> {
              try {
                if (Files.size(file) > 5L * 1024 * 1024)
                  throw new ApiException(413, "PDF must not exceed 5 MB");
                byte[] bytes;
                try (InputStream input = Files.newInputStream(file)) {
                  bytes = input.readNBytes(5 * 1024 * 1024 + 1);
                }
                if (bytes.length > 5 * 1024 * 1024)
                  throw new ApiException(413, "PDF must not exceed 5 MB");
                String boundary = "InterviewAI" + UUID.randomUUID().toString().replace("-", "");
                byte[] prefix =
                    ("--"
                            + boundary
                            + "\r\n"
                            + "Content-Disposition: form-data; name=\"file\";"
                            + " filename=\"resume.pdf\"\r\n"
                            + "Content-Type: application/pdf\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8);
                byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
                var builder =
                    HttpRequest.newBuilder(URI.create(baseUrl + "/resumes/upload"))
                        .timeout(Duration.ofMinutes(2))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Accept", "application/json");
                if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
                return builder
                    .POST(
                        HttpRequest.BodyPublishers.concat(
                            HttpRequest.BodyPublishers.ofByteArray(prefix),
                            HttpRequest.BodyPublishers.ofByteArray(bytes),
                            HttpRequest.BodyPublishers.ofByteArray(suffix)))
                    .build();
              } catch (IOException e) {
                throw new ApiException("Cannot read the selected PDF", e);
              }
            },
            NETWORK)
        .thenCompose(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
        .thenApply(
            response -> {
              try {
                if (response.statusCode() == 401) clearToken();
                JsonNode envelope = mapper.readTree(response.body());
                if (response.statusCode() < 200
                    || response.statusCode() >= 300
                    || !envelope.path("success").asBoolean())
                  throw new ApiException(
                      response.statusCode(),
                      envelope.path("message").asText("Resume upload failed"));
                return convert(envelope.get("data"), ResumeProfile.class);
              } catch (IOException e) {
                throw new ApiException("Invalid upload response", e);
              }
            });
  }

  public <T> CompletableFuture<T> stream(
      String path, Consumer<String> tokens, Class<T> resultType) {
    if (!path.startsWith("/") || path.startsWith("//"))
      throw new IllegalArgumentException("Invalid API path");
    var request =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(65))
            .header("Accept", "text/event-stream");
    if (token != null) request.header("Authorization", "Bearer " + token);
    CompletableFuture<T> result = new CompletableFuture<>();
    AtomicReference<InputStream> body = new AtomicReference<>();
    var responseFuture =
        client.sendAsync(request.GET().build(), HttpResponse.BodyHandlers.ofInputStream());
    activeStreams.add(result);
    result
        .orTimeout(65, TimeUnit.SECONDS)
        .whenComplete(
            (value, error) -> {
              activeStreams.remove(result);
              responseFuture.cancel(true);
              InputStream input = body.get();
              if (input != null)
                try {
                  input.close();
                } catch (IOException ignored) {
                }
            });
    responseFuture.whenCompleteAsync(
        (response, failure) -> {
          if (failure != null) {
            result.completeExceptionally(failure);
            return;
          }
          try (InputStream input = response.body()) {
            body.set(input);
            if (result.isDone()) return;
            if (response.statusCode() != 200) {
              JsonNode error =
                  mapper.readTree(new String(input.readNBytes(65536), StandardCharsets.UTF_8));
              result.completeExceptionally(
                  new ApiException(
                      response.statusCode(), error.path("message").asText("Evaluation failed")));
              if (response.statusCode() == 401) clearToken();
              return;
            }
            if (!response
                .headers()
                .firstValue("Content-Type")
                .orElse("")
                .startsWith("text/event-stream"))
              throw new ApiException(502, "Server did not return an evaluation stream");
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String event = "message", line;
            StringBuilder data = new StringBuilder();
            while (!result.isDone() && (line = reader.readLine()) != null) {
              if (line.length() > 65536) throw new ApiException(502, "Stream event too large");
              if (line.isEmpty()) {
                if (data.length() > 0) {
                  JsonNode payload = mapper.readTree(data.toString());
                  if (event.equals("token")) tokens.accept(payload.path("text").asText());
                  else if (event.equals("result")) {
                    result.complete(convert(payload, resultType));
                    return;
                  } else if (event.equals("error"))
                    throw new ApiException(
                        502, payload.path("message").asText("Evaluation failed"));
                }
                event = "message";
                data.setLength(0);
              } else if (line.startsWith("event:")) event = line.substring(6).trim();
              else if (line.startsWith("data:")) {
                if (data.length() > 0) data.append('\n');
                String value = line.substring(5);
                data.append(value.startsWith(" ") ? value.substring(1) : value);
                if (data.length() > 65536) throw new ApiException(502, "Stream event too large");
              }
            }
            if (!result.isDone())
              throw new ApiException(
                  502, "Connection closed before evaluation completed. Please retry.");
          } catch (Exception e) {
            result.completeExceptionally(e);
          }
        },
        NETWORK);
    return result;
  }

  public <T> T get(String path, Class<T> type) {
    return convert(request("GET", path, null), type);
  }

  public <T> T get(String path, TypeReference<T> type) {
    return mapper.convertValue(request("GET", path, null), type);
  }

  public <T> T post(String path, Object body, Class<T> type) {
    return convert(request("POST", path, body), type);
  }

  public <T> T put(String path, Object body, Class<T> type) {
    return convert(request("PUT", path, body), type);
  }

  public void delete(String path) {
    request("DELETE", path, null);
  }

  public <T> T convert(Object data, Class<T> type) {
    return type == Void.class ? null : mapper.convertValue(data, type);
  }

  public <T> T convert(Object data, TypeReference<T> type) {
    return mapper.convertValue(data, type);
  }

  private JsonNode request(String method, String path, Object body) {
    if (!path.startsWith("/") || path.startsWith("//") || path.contains(".."))
      throw new IllegalArgumentException("Invalid API path");
    try {
      var builder =
          HttpRequest.newBuilder(URI.create(baseUrl + path))
              .timeout(Duration.ofMinutes(4))
              .header("Accept", "application/json");
      String accessToken = token;
      if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
      if (body != null) builder.header("Content-Type", "application/json");
      builder.method(
          method,
          body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
      var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 401) clearToken();
      JsonNode envelope;
      try {
        envelope = mapper.readTree(response.body());
      } catch (IOException e) {
        throw new ApiException(response.statusCode(), "The server returned an invalid response");
      }
      if (response.statusCode() < 200
          || response.statusCode() >= 300
          || envelope == null
          || !envelope.path("success").asBoolean())
        throw new ApiException(
            response.statusCode(),
            envelope == null
                ? "API request failed"
                : envelope.path("message").asText("API request failed"));
      return envelope.get("data");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException("Request interrupted", e);
    } catch (IOException e) {
      throw new ApiException(
          "Cannot reach the InterviewAI server. Check that the backend is running.", e);
    }
  }

  public static Map<String, Object> body(Object... pairs) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) values.put((String) pairs[i], pairs[i + 1]);
    return values;
  }
}
