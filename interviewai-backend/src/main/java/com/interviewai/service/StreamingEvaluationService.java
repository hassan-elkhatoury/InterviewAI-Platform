package com.interviewai.service;

import com.interviewai.dto.*;
import com.interviewai.exception.ApiException;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class StreamingEvaluationService {
  private final CurriculumService curriculum;
  private final OpenRouterAIService ai;
  private final EvaluationService evaluations;
  private final ExecutorService workers =
      new ThreadPoolExecutor(8, 8, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32));
  private final ScheduledExecutorService timers = Executors.newScheduledThreadPool(2);

  public StreamingEvaluationService(
      CurriculumService curriculum, OpenRouterAIService ai, EvaluationService evaluations) {
    this.curriculum = curriculum;
    this.ai = ai;
    this.evaluations = evaluations;
  }

  public SseEmitter stream(int userId, Requests.Evaluate request) {
    var question = curriculum.ownedQuestion(userId, request.questionId());
    var emitter = new SseEmitter(60_000L);
    var control = new StreamControl();
    var closed = new AtomicBoolean();
    var task = new AtomicReference<Future<?>>();
    Runnable cancel =
        () -> {
          closed.set(true);
          control.close();
          var work = task.get();
          if (work != null) work.cancel(true);
        };
    emitter.onCompletion(cancel);
    emitter.onTimeout(cancel);
    emitter.onError(error -> cancel.run());
    var deadline =
        timers.schedule(
            () -> {
              if (closed.compareAndSet(false, true)) {
                try {
                  emitter.send(
                      SseEmitter.event()
                          .name("error")
                          .data(Map.of("message", "Evaluation timed out. Please retry.")));
                } catch (IOException | IllegalStateException ignored) {
                }
                cancel.run();
                emitter.completeWithError(
                    new ApiException(HttpStatus.GATEWAY_TIMEOUT, "Evaluation timed out"));
              }
            },
            60,
            TimeUnit.SECONDS);
    var heartbeat =
        timers.scheduleAtFixedRate(
            () -> {
              if (!closed.get())
                try {
                  emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (IOException | IllegalStateException e) {
                  cancel.run();
                }
            },
            10,
            10,
            TimeUnit.SECONDS);
    emitter.onCompletion(
        () -> {
          deadline.cancel(false);
          heartbeat.cancel(false);
        });
    try {
      task.set(
          workers.submit(
              () -> {
                try {
                  java.util.function.Consumer<String> send =
                      text -> {
                        if (closed.get()) throw new CancellationException();
                        try {
                          emitter.send(SseEmitter.event().name("token").data(Map.of("text", text)));
                        } catch (IOException e) {
                          cancel.run();
                          throw new java.io.UncheckedIOException(e);
                        }
                      };
                  Views.Evaluation result;
                  if ("MULTIPLE_CHOICE".equals(question.getQuestionType())) {
                    result = evaluations.evaluate(userId, request);
                    send.accept(result.explanation());
                  } else {
                    var grade =
                        ai.streamGrade(
                            question.getQuestion(),
                            question.getCorrectAnswer(),
                            request.userAnswer(),
                            send,
                            control);
                    if (closed.get()) return;
                    result = evaluations.saveStreamed(userId, request, grade);
                  }
                  if (!closed.get()) emitter.send(SseEmitter.event().name("result").data(result));
                  emitter.complete();
                } catch (CancellationException | UncheckedIOException ignored) {
                  emitter.complete();
                } catch (Exception e) {
                  if (!closed.get()) {
                    String message =
                        e instanceof ApiException
                            ? e.getMessage()
                            : "Evaluation failed. Please retry.";
                    try {
                      emitter.send(
                          SseEmitter.event().name("error").data(Map.of("message", message)));
                    } catch (IOException | IllegalStateException ignored) {
                    }
                    // The error event is already committed; exception advice will not write a
                    // second response.
                    emitter.completeWithError(e);
                  }
                } finally {
                  closed.set(true);
                  control.close();
                  deadline.cancel(false);
                  heartbeat.cancel(false);
                }
              }));
      if (closed.get()) task.get().cancel(true);
    } catch (RejectedExecutionException e) {
      cancel.run();
      deadline.cancel(false);
      heartbeat.cancel(false);
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE, "Too many evaluations. Please retry shortly.");
    }
    return emitter;
  }

  @PreDestroy
  void shutdown() {
    workers.shutdownNow();
    timers.shutdownNow();
  }
}
