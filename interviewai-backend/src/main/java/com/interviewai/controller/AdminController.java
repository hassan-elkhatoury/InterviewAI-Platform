package com.interviewai.controller;

import com.interviewai.dto.*;
import com.interviewai.service.AdminService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
  private final AdminService admin;

  public AdminController(AdminService admin) {
    this.admin = admin;
  }

  @GetMapping("/users")
  public ApiResponse<List<Map<String, Object>>> users() {
    return ApiResponse.ok(admin.users());
  }

  @PostMapping("/users")
  public ApiResponse<Views.User> createUser(@Valid @RequestBody Requests.CreateUser request) {
    return ApiResponse.ok(admin.createUser(request));
  }

  @GetMapping("/users/{id}")
  public ApiResponse<Map<String, Object>> user(@PathVariable int id) {
    return ApiResponse.ok(admin.user(id));
  }

  @PutMapping("/users/{id}")
  public ApiResponse<Void> user(
      Principal p, @PathVariable int id, @Valid @RequestBody Requests.AdminUser request) {
    admin.updateUser(Integer.parseInt(p.getName()), id, request);
    return ApiResponse.ok(null);
  }

  @PutMapping("/users/{id}/password")
  public ApiResponse<Void> password(
      @PathVariable int id, @Valid @RequestBody Requests.AdminPassword request) {
    admin.password(id, request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/users/{id}/reset-xp")
  public ApiResponse<Void> resetXP(@PathVariable int id) {
    admin.reset(id, false);
    return ApiResponse.ok(null);
  }

  @PostMapping("/users/{id}/reset-progress")
  public ApiResponse<Void> resetProgress(@PathVariable int id) {
    admin.reset(id, true);
    return ApiResponse.ok(null);
  }

  @GetMapping("/analytics")
  public ApiResponse<Map<String, Object>> analytics() {
    return ApiResponse.ok(admin.analytics());
  }

  @GetMapping("/courses")
  public ApiResponse<List<Map<String, Object>>> courses() {
    return ApiResponse.ok(admin.courses());
  }

  @PutMapping("/courses/{id}/status")
  public ApiResponse<Void> status(
      @PathVariable int id, @Valid @RequestBody Requests.Status request) {
    admin.courseStatus(id, request.status());
    return ApiResponse.ok(null);
  }

  @DeleteMapping("/courses/{id}")
  public ApiResponse<Void> delete(@PathVariable int id) {
    admin.deleteCourse(id);
    return ApiResponse.ok(null);
  }
}
