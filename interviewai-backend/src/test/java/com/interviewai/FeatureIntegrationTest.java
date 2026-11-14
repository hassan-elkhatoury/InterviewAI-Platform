package com.interviewai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.interviewai.dto.*;
import com.interviewai.entity.*;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.*;
import com.interviewai.service.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
class FeatureIntegrationTest {
  @org.springframework.boot.test.web.server.LocalServerPort int port;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UserRepository users;
  @Autowired CourseRepository courses;
  @Autowired LessonRepository lessons;
  @Autowired QuestionRepository questions;
  @Autowired FeedbackRepository feedback;
  @Autowired ProgressRepository progress;
  @Autowired ResumeContextRepository resumes;
  @MockitoBean AIService ai;
  String token;
  int uid;
  Question question;
  final ResumeProfile profile =
      new ResumeProfile(
          List.of("Java", "Spring Boot"),
          "Senior",
          "5 years building Java APIs",
          List.of("Java backend", "API design"));

  @BeforeEach
  void setup() throws Exception {
    token = register();
    Course c = new Course();
    c.setUserId(uid);
    c.setCourseTitle("Interview");
    courses.saveAndFlush(c);
    Lesson l = new Lesson();
    l.setCourseId(c.getId());
    l.setChapterNumber(1);
    l.setName("Java");
    lessons.saveAndFlush(l);
    question = new Question();
    question.setChapterId(l.getId());
    question.setQuestion("Explain dependency injection");
    question.setQuestionType("SHORT_ANSWER");
    question.setCorrectAnswer("Dependencies are provided externally");
    questions.saveAndFlush(question);
  }

  String register() throws Exception {
    String name = "feature" + UUID.randomUUID().toString().replace("-", "");
    var response =
        mvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "username",
                                name,
                                "email",
                                name + "@example.com",
                                "password",
                                "test-password123"))))
            .andExpect(status().isCreated())
            .andReturn();
    var auth = mapper.readTree(response.getResponse().getContentAsString()).path("data");
    uid = auth.path("user").path("id").asInt();
    return auth.path("accessToken").asText();
  }

  String bearer() {
    return "Bearer " + token;
  }

  byte[] pdf(String text) throws Exception {
    try (var doc = new PDDocument();
        var output = new ByteArrayOutputStream()) {
      var page = new PDPage();
      doc.addPage(page);
      try (var content = new PDPageContentStream(doc, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(30, 700);
        content.showText(text);
        content.endText();
      }
      doc.save(output);
      return output.toByteArray();
    }
  }

  ResultActions upload(byte[] bytes) throws Exception {
    return mvc.perform(
        multipart("/api/v1/resumes/upload")
            .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", bytes))
            .header("Authorization", bearer()));
  }

  @Test
  void uploadsPdfAndRetrievesOnlyTheCurrentUsersProfile() throws Exception {
    when(ai.extractResume(anyString())).thenReturn(profile);
    upload(pdf("Senior Java developer with Spring Boot, 5 years experience"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.skills[0]").value("Java"));
    assertThat(resumes.findById(uid).orElseThrow().getFullText()).contains("Spring Boot");
    mvc.perform(get("/api/v1/resumes/me").header("Authorization", bearer()))
        .andExpect(jsonPath("$.data.seniority").value("Senior"));
    token = register();
    mvc.perform(get("/api/v1/resumes/me").header("Authorization", bearer()))
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void invalidOversizedEmptyAndScannedPdfsFailBeforeAi() throws Exception {
    upload("not a pdf".getBytes()).andExpect(status().isBadRequest());
    upload(new byte[0]).andExpect(status().isBadRequest());
    upload(new byte[5 * 1024 * 1024 + 1]).andExpect(status().isPayloadTooLarge());
    upload(pdf("")).andExpect(status().isBadRequest());
    mvc.perform(multipart("/api/v1/resumes/upload").header("Authorization", bearer()))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(ai);
    assertThat(resumes.findById(uid)).isEmpty();
  }

  @Test
  void failedExtractionPreservesThePreviousResume() throws Exception {
    when(ai.extractResume(anyString())).thenReturn(profile);
    upload(pdf("Java developer")).andExpect(status().isOk());
    doThrow(new ApiException(HttpStatus.BAD_GATEWAY, "AI unavailable"))
        .when(ai)
        .extractResume(anyString());
    upload(pdf("Replacement resume")).andExpect(status().isBadGateway());
    assertThat(resumes.findById(uid).orElseThrow().getFullText()).contains("Java developer");
  }

  @Test
  void storedResumeEvidenceIsInjectedIntoBothGeneratedTracks() throws Exception {
    when(ai.extractResume(anyString())).thenReturn(profile);
    upload(pdf("Java developer evidence")).andExpect(status().isOk());
    var q =
        new Requests.GeneratedQuestion(
            "Java?", "SHORT_ANSWER", List.of(), "A language", "Explanation");
    when(ai.generate(any(), anyBoolean()))
        .thenReturn(
            new Requests.GeneratedCourse(
                "Java plan",
                List.of(new Requests.GeneratedLesson("Basics", "Practice", List.of(q)))));
    mvc.perform(
            post("/api/v1/courses/generate")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"preferences\":{\"interviewType\":\"JOB\",\"language\":\"ENGLISH\",\"timeline\":\"LATER\"}}"))
        .andExpect(status().isOk());
    var captured = org.mockito.ArgumentCaptor.forClass(Requests.Generate.class);
    verify(ai, times(2)).generate(captured.capture(), anyBoolean());
    assertThat(captured.getAllValues())
        .allSatisfy(
            r ->
                assertThat(r.cvText())
                    .contains("Java developer evidence", "Spring Boot", "Senior"));
  }

  @Test
  void streamingEmitsTokensThenPersistsExactlyOneEvaluation() throws Exception {
    doAnswer(
            invocation -> {
              Consumer<String> tokens = invocation.getArgument(3);
              tokens.accept("You explained ");
              tokens.accept("the concept.");
              return new AIService.Grade(
                  85, "Good", "Add detail", "You explained the concept.");
            })
        .when(ai)
        .streamGrade(anyString(), anyString(), anyString(), any(), any());
    MvcResult pending =
        mvc.perform(
                get("/api/v1/interviews/evaluate/stream")
                    .param("questionId", question.getId().toString())
                    .param("userAnswer", "My answer")
                    .header("Authorization", bearer()))
            .andExpect(request().asyncStarted())
            .andReturn();
    pending.getAsyncResult(10000);
    String body =
        mvc.perform(asyncDispatch(pending))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).contains("event:token", "You explained ", "event:result", "\"score\":85");
    assertThat(body.indexOf("event:token")).isLessThan(body.indexOf("event:result"));
    assertThat(feedback.findByUserIdOrderByCreatedAtDesc(uid)).hasSize(1);
    assertThat(progress.findByUserId(uid)).hasSize(1);
  }

  @Test
  void streamingFailureEmitsErrorWithoutSavingFeedback() throws Exception {
    doThrow(new ApiException(HttpStatus.BAD_GATEWAY, "AI stream failed"))
        .when(ai)
        .streamGrade(anyString(), anyString(), anyString(), any(), any());
    var pending =
        mvc.perform(
                get("/api/v1/interviews/evaluate/stream")
                    .param("questionId", question.getId().toString())
                    .param("userAnswer", "answer")
                    .header("Authorization", bearer()))
            .andExpect(request().asyncStarted())
            .andReturn();
    pending.getAsyncResult(10000);
    String body =
        mvc.perform(asyncDispatch(pending)).andReturn().getResponse().getContentAsString();
    assertThat(body).contains("event:error", "AI stream failed").doesNotContain("event:result");
    assertThat(feedback.findByUserIdOrderByCreatedAtDesc(uid)).isEmpty();
    assertThat(progress.findByUserId(uid)).isEmpty();
  }

  @Test
  void streamingChecksAuthenticationOwnershipAndInputBeforeOpening() throws Exception {
    mvc.perform(
            get("/api/v1/interviews/evaluate/stream")
                .param("questionId", "1")
                .param("userAnswer", "answer"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/interviews/evaluate/stream")
                .header("Authorization", bearer())
                .param("questionId", "-1")
                .param("userAnswer", " "))
        .andExpect(status().isBadRequest());
    token = register();
    mvc.perform(
            get("/api/v1/interviews/evaluate/stream")
                .header("Authorization", bearer())
                .param("questionId", question.getId().toString())
                .param("userAnswer", "answer"))
        .andExpect(status().isNotFound());
    verifyNoInteractions(ai);
  }

  @Test
  void realHttpStreamDeliversCritiqueBeforeEvaluationCompletes() throws Exception {
    var finish = new java.util.concurrent.CountDownLatch(1);
    doAnswer(
            invocation -> {
              Consumer<String> tokens = invocation.getArgument(3);
              tokens.accept("Live critique");
              if (!finish.await(10, java.util.concurrent.TimeUnit.SECONDS))
                throw new IllegalStateException("Client did not receive token");
              return new AIService.Grade(80, "Good", "Add detail", "Live critique");
            })
        .when(ai)
        .streamGrade(anyString(), anyString(), anyString(), any(), any());
    var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    try {
      var request =
          java.net.http.HttpRequest.newBuilder(
                  java.net.URI.create(
                      "http://127.0.0.1:"
                          + port
                          + "/api/v1/interviews/evaluate/stream?questionId="
                          + question.getId()
                          + "&userAnswer=answer"))
              .header("Authorization", bearer())
              .GET()
              .build();
      var response =
          java.net.http.HttpClient.newHttpClient()
              .sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream())
              .get(5, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(response.statusCode()).isEqualTo(200);
      try (var reader =
          new BufferedReader(
              new InputStreamReader(response.body(), java.nio.charset.StandardCharsets.UTF_8))) {
        String first =
            executor
                .submit(
                    () -> {
                      String line;
                      while ((line = reader.readLine()) != null)
                        if (line.startsWith("data:")) return line;
                      return "";
                    })
                .get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(first).contains("Live critique");
        assertThat(feedback.findByUserIdOrderByCreatedAtDesc(uid)).isEmpty();
        finish.countDown();
        String rest =
            executor
                .submit(() -> reader.lines().collect(java.util.stream.Collectors.joining("\n")))
                .get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(rest).contains("event:result", "\"score\":80");
      }
      assertThat(feedback.findByUserIdOrderByCreatedAtDesc(uid)).hasSize(1);
    } finally {
      finish.countDown();
      executor.shutdownNow();
    }
  }
}
