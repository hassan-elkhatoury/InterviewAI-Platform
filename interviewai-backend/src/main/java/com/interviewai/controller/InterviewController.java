package com.interviewai.controller;

import com.interviewai.dto.*;
import com.interviewai.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class InterviewController {
  private final EvaluationService evaluations;
  private final QuestService quests;

  public InterviewController(EvaluationService evaluations, QuestService quests) {
    this.evaluations = evaluations;
    this.quests = quests;
  }

  @PostMapping("/interviews/evaluate")
  public ApiResponse<Views.Evaluation> evaluate(
      Principal p, @Valid @RequestBody Requests.Evaluate request) {
    return ApiResponse.ok(evaluations.evaluate(Integer.parseInt(p.getName()), request));
  }

  @GetMapping("/quests")
  public ApiResponse<List<Views.Quest>> quests(Principal p) {
    return ApiResponse.ok(quests.list(Integer.parseInt(p.getName())));
  }

  @PostMapping("/quests/{id}/claim")
  public ApiResponse<Integer> claim(Principal p, @PathVariable int id) {
    return ApiResponse.ok(quests.claim(Integer.parseInt(p.getName()), id));
  }
}
