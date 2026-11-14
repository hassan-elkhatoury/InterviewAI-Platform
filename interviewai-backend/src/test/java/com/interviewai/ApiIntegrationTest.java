package com.interviewai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.interviewai.dto.Requests;
import com.interviewai.entity.*;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.*;
import com.interviewai.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(
    printOnlyOnFailure = false,
    print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired CourseRepository courses;
  @Autowired LessonRepository lessons;
  @Autowired QuestionRepository questions;
  @Autowired FeedbackRepository feedback;
  @Autowired ProgressRepository progress;
  @Autowired QuestRepository quests;
  @Autowired PasswordEncoder passwords;
  @Autowired EvaluationService evaluations;
  @MockitoBean AIService ai;
  String username;
  String token;
  User owner;
  Course course;
  Lesson lesson;
  Question question;

  @BeforeEach
  void setUp() throws Exception {
    username = "user" + UUID.randomUUID().toString().replace("-", "");
    JsonNode auth = register(username);
    token = auth.path("data").path("accessToken").asText();
    owner = users.findByUsernameIgnoreCase(username).orElseThrow();
    course = new Course();
    course.setUserId(owner.getId());
    course.setCourseTitle("Java interviews");
    courses.saveAndFlush(course);
    lesson = new Lesson();
    lesson.setCourseId(course.getId());
    lesson.setChapterNumber(1);
    lesson.setName("Fundamentals");
    lesson.setDescription("Java basics");
    lessons.saveAndFlush(lesson);
    question = question("MULTIPLE_CHOICE");
  }

  Question question(String type) {
    Question q = new Question();
    q.setChapterId(lesson.getId());
    q.setQuestion("Which keyword declares a class?");
    q.setQuestionType(type);
    q.setCorrectAnswer("class");
    q.setExplanation("The class keyword declares a Java class.");
    return questions.saveAndFlush(q);
  }

  JsonNode register(String name) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "username",
                                name,
                                "email",
                                name + "@example.com",
                                "password",
                                "password123"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
    return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  ResultActions evaluate(int id, String answer) throws Exception {
    return mvc.perform(
        authenticated(post("/api/v1/interviews/evaluate"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(new Requests.Evaluate(id, answer))));
  }

  @Test
  void registrationHashesPasswordsAndLoginSupportsEmail() throws Exception {
    assertThat(owner.getRole()).isEqualTo("ROLE_USER");
    assertThat(owner.getPasswordHash()).startsWith("$2");
    assertThat(passwords.matches("password123", owner.getPasswordHash())).isTrue();
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of("username", username + "@example.com", "password", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(Map.of("username", username, "password", "incorrect"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false));
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "username",
                            username.toUpperCase(Locale.ROOT),
                            "email",
                            "duplicate@example.com",
                            "password",
                            "password123"))))
        .andExpect(status().isConflict());
  }

  @Test
  void validationAndErrorsUseTheEnvelope() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"x\",\"email\":\"bad\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"newuser\",\"email\":\"new@example.com\",\"password\":\"password123\",\"role\":\"ROLE_ADMIN\"}"))
        .andExpect(status().isBadRequest());
    evaluate(question.getId(), " ").andExpect(status().isBadRequest());
    evaluate(-1, "answer").andExpect(status().isBadRequest());
    mvc.perform(
            authenticated(post("/api/v1/interviews/evaluate"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
    mvc.perform(authenticated(get("/api/v1/courses/not-an-id"))).andExpect(status().isBadRequest());
  }

  @Test
  void bearerAuthenticationAndRoleChecksAreEnforced() throws Exception {
    mvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false));
    mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer tampered"))
        .andExpect(status().isUnauthorized());
    mvc.perform(authenticated(get("/api/v1/users/me")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value(username));
    mvc.perform(authenticated(get("/api/v1/admin/users")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false));
    owner.setRole("ROLE_ADMIN");
    users.saveAndFlush(owner);
    mvc.perform(authenticated(get("/api/v1/admin/users")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));
    mvc.perform(authenticated(get("/api/v1/admin/analytics")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalUsers").isNumber());
    owner.setActive(false);
    users.saveAndFlush(owner);
    mvc.perform(authenticated(get("/api/v1/users/me"))).andExpect(status().isUnauthorized());
  }

  @Test
  void coursesAndEvaluationAreScopedToTheCurrentUser() throws Exception {
    mvc.perform(authenticated(get("/api/v1/courses")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].courseTitle").value("Java interviews"));
    mvc.perform(authenticated(get("/api/v1/courses/" + course.getId() + "/lessons")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].questions[0].correctAnswer").isEmpty());
    token =
        register("other" + UUID.randomUUID().toString().replace("-", ""))
            .path("data")
            .path("accessToken")
            .asText();
    mvc.perform(authenticated(get("/api/v1/courses/" + course.getId() + "/lessons")))
        .andExpect(status().isNotFound());
    mvc.perform(authenticated(get("/api/v1/lessons/" + lesson.getId())))
        .andExpect(status().isNotFound());
    evaluate(question.getId(), "class").andExpect(status().isNotFound());
    mvc.perform(authenticated(get("/api/v1/courses"))).andExpect(jsonPath("$.data").isEmpty());
    verifyNoInteractions(ai);
  }

  @Test
  void evaluationPersistsFeedbackAndAwardsXpOnce() throws Exception {
    evaluate(question.getId(), "class")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.score").value(100))
        .andExpect(jsonPath("$.data.xpAwarded").value(10));
    evaluate(question.getId(), "class")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.xpAwarded").value(0));
    assertThat(progress.findByUserId(owner.getId())).hasSize(1);
    assertThat(feedback.findByUserIdOrderByCreatedAtDesc(owner.getId())).hasSize(2);
    assertThat(lessons.findById(lesson.getId()).orElseThrow().getStatus()).isEqualTo("COMPLETED");
    assertThat(courses.findById(course.getId()).orElseThrow().getStatus()).isEqualTo("COMPLETED");
    mvc.perform(authenticated(get("/api/v1/users/me/achievements")))
        .andExpect(jsonPath("$.data[0].code").value("FIRST_STEPS"));
    verifyNoInteractions(ai);
  }

  @Test
  void aiFailureDoesNotInventFeedbackOrXp() throws Exception {
    Question shortAnswer = question("SHORT_ANSWER");
    when(ai.validateAnswer(anyString(), anyString(), anyString()))
        .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "AI service unavailable"));
    evaluate(shortAnswer.getId(), "My answer").andExpect(status().isBadGateway());
    assertThat(progress.findByUserId(owner.getId())).isEmpty();
    assertThat(feedback.findByUserIdOrderByCreatedAtDesc(owner.getId())).isEmpty();
    assertThat(questions.findById(shortAnswer.getId()).orElseThrow().getStatus())
        .isEqualTo("NOT_STARTED");
    doReturn(new AIService.Grade(85, "Good reasoning", "Add an example", "Explanation"))
        .when(ai)
        .validateAnswer(anyString(), anyString(), anyString());
    evaluate(shortAnswer.getId(), "My answer")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.score").value(85));
  }

  @Test
  void questClaimsRequireEligibilityAndCannotBeRepeated() throws Exception {
    Quest daily = quests.findByCode("DAILY").orElseThrow();
    mvc.perform(authenticated(get("/api/v1/quests")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2));
    mvc.perform(authenticated(post("/api/v1/quests/" + daily.getId() + "/claim")))
        .andExpect(status().isConflict());
    for (int i = 0; i < 20; i++) {
      Question q = question("MULTIPLE_CHOICE");
      Feedback f = new Feedback();
      f.setUserId(owner.getId());
      f.setCourseId(course.getId());
      f.setQuestionId(q.getId());
      f.setScore(100);
      feedback.save(f);
    }
    feedback.flush();
    mvc.perform(authenticated(post("/api/v1/quests/" + daily.getId() + "/claim")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(900));
    mvc.perform(authenticated(post("/api/v1/quests/" + daily.getId() + "/claim")))
        .andExpect(status().isConflict());
    assertThat(progress.findByUserId(owner.getId()).stream().mapToInt(Progress::getXp).sum())
        .isEqualTo(900);
  }

  @Test
  void profileChangesCannotEscalateRolesAndPasswordChangeRevokesTokens() throws Exception {
    mvc.perform(
            authenticated(put("/api/v1/users/me"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "username",
                            username,
                            "email",
                            "new@example.com",
                            "role",
                            "ROLE_ADMIN"))))
        .andExpect(status().isBadRequest());
    mvc.perform(
            authenticated(put("/api/v1/users/me/password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"currentPassword\":\"password123\",\"newPassword\":\"replacement123\"}"))
        .andExpect(status().isOk());
    mvc.perform(authenticated(get("/api/v1/users/me"))).andExpect(status().isUnauthorized());
    assertThat(
            passwords.matches(
                "replacement123", users.findById(owner.getId()).orElseThrow().getPasswordHash()))
        .isTrue();
  }

  @Test
  void goalsAndOnboardingRoundTripWithoutClientOwnedUserIds() throws Exception {
    mvc.perform(
            authenticated(put("/api/v1/users/me/onboarding"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"interviewType\":\"JOB\",\"language\":\"ENGLISH\",\"timeline\":\"THIS_WEEK\",\"context\":\"Java\",\"description\":\"Practice\"}"))
        .andExpect(status().isOk());
    mvc.perform(authenticated(get("/api/v1/users/me/onboarding")))
        .andExpect(jsonPath("$.data.context").value("Java"));
    String result =
        mvc.perform(
                authenticated(post("/api/v1/users/me/goals"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"goalName\":\"Earn XP\",\"goalType\":\"XP\",\"targetValue\":100}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = json.readTree(result).path("data").asInt();
    evaluate(question.getId(), "class").andExpect(status().isOk());
    mvc.perform(authenticated(get("/api/v1/users/me/goals")))
        .andExpect(jsonPath("$.data[0].currentValue").value(10));
    token =
        register("goalowner" + UUID.randomUUID().toString().replace("-", ""))
            .path("data")
            .path("accessToken")
            .asText();
    mvc.perform(authenticated(delete("/api/v1/users/me/goals/" + id)))
        .andExpect(status().isNotFound());
  }

  @Test
  void legacyPasswordsAreUpgradedAfterASuccessfulLogin() throws Exception {
    owner.setPasswordHash(
        HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest("old-password".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    users.saveAndFlush(owner);
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of("username", username, "password", "old-password"))))
        .andExpect(status().isOk());
    assertThat(users.findById(owner.getId()).orElseThrow().getPasswordHash()).startsWith("$2");
  }

  @Test
  void courseGenerationPersistsBothCoursesAndPreparationTogether() throws Exception {
    var item =
        new Requests.GeneratedQuestion(
            "Keyword?",
            "MULTIPLE_CHOICE",
            List.of("class", "function"),
            "class",
            "Java uses class.");
    var chapter = new Requests.GeneratedLesson("Basics", "Practice", List.of(item));
    var generated = new Requests.GeneratedCourse("Prepared course", List.of(chapter));
    when(ai.generate(any(), anyBoolean())).thenReturn(generated);
    mvc.perform(
            authenticated(post("/api/v1/courses/generate"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"preferences\":{\"interviewType\":\"JOB\",\"language\":\"ENGLISH\",\"timeline\":\"LATER\",\"context\":\"Java\"},\"cvText\":\"Experience\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2));
    mvc.perform(authenticated(get("/api/v1/preparations")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1));
    assertThat(courses.findByUserIdOrderByCreatedAtDesc(owner.getId())).hasSize(3);
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentEvaluationsCannotDuplicateRewards() throws Exception {
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var request = new Requests.Evaluate(question.getId(), "class");
      var first = executor.submit(() -> evaluations.evaluate(owner.getId(), request));
      var second = executor.submit(() -> evaluations.evaluate(owner.getId(), request));
      int rewards =
          first.get(20, java.util.concurrent.TimeUnit.SECONDS).xpAwarded()
              + second.get(20, java.util.concurrent.TimeUnit.SECONDS).xpAwarded();
      assertThat(rewards).isEqualTo(10);
      assertThat(progress.findByUserId(owner.getId())).hasSize(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void expiredTokensAreRejected() throws Exception {
    var key =
        new javax.crypto.spec.SecretKeySpec(
            "test-only-signing-key-at-least-32-bytes-long"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "HmacSHA256");
    var encoder =
        new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
    var claims =
        org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
            .issuer("interviewai")
            .subject(owner.getId().toString())
            .issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(Instant.now().minusSeconds(3600))
            .claim("version", 0)
            .build();
    token =
        encoder
            .encode(
                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                    org.springframework.security.oauth2.jwt.JwsHeader.with(
                            org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                        .build(),
                    claims))
            .getTokenValue();
    mvc.perform(authenticated(get("/api/v1/users/me"))).andExpect(status().isUnauthorized());
  }
}
