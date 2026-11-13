package com.interviewai.controller;

import com.interviewai.dto.*;
import com.interviewai.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {
  private final ProfileService profiles;
  private final ProgressService progress;
  private final AchievementService achievements;
  private final OpenRouterAIService ai;
  private final CurriculumService curriculum;
  private final ResumeService resumes;

  public UserController(
      ProfileService profiles,
      ProgressService progress,
      AchievementService achievements,
      OpenRouterAIService ai,
      CurriculumService curriculum,
      ResumeService resumes) {
    this.profiles = profiles;
    this.progress = progress;
    this.achievements = achievements;
    this.ai = ai;
    this.curriculum = curriculum;
    this.resumes = resumes;
  }

  private int uid(Principal p) {
    return Integer.parseInt(p.getName());
  }

  @GetMapping("/users/me")
  public ApiResponse<Views.User> profile(Principal p) {
    return ApiResponse.ok(profiles.profile(uid(p)));
  }

  @PutMapping("/users/me")
  public ApiResponse<Views.User> update(Principal p, @Valid @RequestBody Requests.Profile request) {
    return ApiResponse.ok(profiles.update(uid(p), request));
  }

  @PutMapping("/users/me/password")
  public ApiResponse<Void> password(Principal p, @Valid @RequestBody Requests.Password request) {
    profiles.changePassword(uid(p), request);
    return ApiResponse.ok(null);
  }

  @GetMapping("/users/me/last-used")
  public ApiResponse<Integer[]> lastUsed(Principal p) {
    return ApiResponse.ok(profiles.lastUsed(uid(p)));
  }

  @PutMapping("/users/me/last-used")
  public ApiResponse<Void> lastUsed(Principal p, @Valid @RequestBody Requests.LastUsed request) {
    profiles.lastUsed(uid(p), request);
    return ApiResponse.ok(null);
  }

  @GetMapping("/users/me/progress")
  public ApiResponse<Map<String, Object>> progress(Principal p) {
    return ApiResponse.ok(progress.summary(uid(p)));
  }

  @GetMapping("/users/me/achievements")
  public ApiResponse<List<Views.Achievement>> achievements(Principal p) {
    return ApiResponse.ok(achievements.list(uid(p)));
  }

  @GetMapping("/users/me/onboarding")
  public ApiResponse<Requests.Onboarding> onboarding(Principal p) {
    return ApiResponse.ok(profiles.onboarding(uid(p)));
  }

  @PutMapping("/users/me/onboarding")
  public ApiResponse<Void> onboarding(
      Principal p, @Valid @RequestBody Requests.Onboarding request) {
    profiles.onboarding(uid(p), request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/courses/generate")
  public ApiResponse<List<Views.Course>> generate(
      Principal p, @Valid @RequestBody Requests.Generate request) {
    int uid = uid(p);
    var personalized = resumes.personalize(uid, request);
    var technical = ai.generate(personalized, true);
    var softskills = ai.generate(personalized, false);
    var result = curriculum.savePreparation(uid, technical, softskills);
    profiles.onboarding(uid, request.preferences());
    return ApiResponse.ok(result);
  }

  @GetMapping("/users/me/goals")
  public ApiResponse<List<Map<String, Object>>> goals(Principal p) {
    return ApiResponse.ok(profiles.goals(uid(p)));
  }

  @PostMapping("/users/me/goals")
  public ApiResponse<Integer> goal(Principal p, @Valid @RequestBody Requests.Goal request) {
    return ApiResponse.ok(profiles.saveGoal(uid(p), null, request));
  }

  @PutMapping("/users/me/goals/{id}")
  public ApiResponse<Integer> goal(
      Principal p, @PathVariable int id, @Valid @RequestBody Requests.Goal request) {
    return ApiResponse.ok(profiles.saveGoal(uid(p), id, request));
  }

  @DeleteMapping("/users/me/goals/{id}")
  public ApiResponse<Void> deleteGoal(Principal p, @PathVariable int id) {
    profiles.deleteGoal(uid(p), id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/leaderboard")
  public ApiResponse<List<Map<String, Object>>> leaderboard() {
    return ApiResponse.ok(progress.leaderboard());
  }
}
