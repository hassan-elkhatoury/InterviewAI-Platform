package com.interviewai.controller;

import com.interviewai.dto.Requests;
import com.interviewai.service.StreamingEvaluationService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/interviews")
public class StreamingController {
  private final StreamingEvaluationService streaming;

  public StreamingController(StreamingEvaluationService streaming) {
    this.streaming = streaming;
  }

  @GetMapping(value = "/evaluate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> stream(
      Principal p, @Valid @ModelAttribute Requests.Evaluate request) {
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .header("X-Accel-Buffering", "no")
        .body(streaming.stream(Integer.parseInt(p.getName()), request));
  }
}
