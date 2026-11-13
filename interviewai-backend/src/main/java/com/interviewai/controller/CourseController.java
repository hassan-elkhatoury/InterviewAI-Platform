package com.interviewai.controller;

import com.interviewai.dto.*;
import com.interviewai.service.CurriculumService;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CourseController {
  private final CurriculumService courses;

  public CourseController(CurriculumService courses) {
    this.courses = courses;
  }

  private int uid(Principal p) {
    return Integer.parseInt(p.getName());
  }

  @GetMapping("/courses")
  public ApiResponse<List<Views.Course>> all(Principal p) {
    return ApiResponse.ok(courses.courses(uid(p)));
  }

  @GetMapping("/courses/{id}")
  public ApiResponse<Views.Course> course(Principal p, @PathVariable int id) {
    return ApiResponse.ok(courses.course(uid(p), id));
  }

  @GetMapping("/courses/{id}/lessons")
  public ApiResponse<List<Views.Lesson>> lessons(Principal p, @PathVariable int id) {
    return ApiResponse.ok(courses.lessons(uid(p), id));
  }

  @GetMapping("/lessons/{id}")
  public ApiResponse<Views.Lesson> lesson(Principal p, @PathVariable int id) {
    return ApiResponse.ok(courses.lesson(uid(p), id));
  }

  @GetMapping("/lessons/{id}/questions")
  public ApiResponse<List<Views.Question>> questions(Principal p, @PathVariable int id) {
    return ApiResponse.ok(courses.questions(uid(p), id));
  }

  @GetMapping("/questions/review")
  public ApiResponse<List<Views.Question>> review(Principal p) {
    return ApiResponse.ok(courses.review(uid(p)));
  }

  @GetMapping("/questions/{id}/lesson")
  public ApiResponse<Views.Lesson> questionLesson(Principal p, @PathVariable int id) {
    return ApiResponse.ok(courses.lesson(uid(p), courses.ownedQuestion(uid(p), id).getChapterId()));
  }

  @GetMapping("/preparations")
  public ApiResponse<List<Map<String, Object>>> preparations(Principal p) {
    return ApiResponse.ok(courses.preparations(uid(p)));
  }
}
