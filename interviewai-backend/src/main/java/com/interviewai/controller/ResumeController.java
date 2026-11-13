package com.interviewai.controller;

import com.interviewai.dto.*;
import com.interviewai.service.ResumeService;
import java.security.Principal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {
  private final ResumeService resumes;

  public ResumeController(ResumeService resumes) {
    this.resumes = resumes;
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<ResumeProfile> upload(Principal p, @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(resumes.upload(Integer.parseInt(p.getName()), file));
  }

  @GetMapping("/me")
  public ApiResponse<ResumeProfile> profile(Principal p) {
    return ApiResponse.ok(resumes.profile(Integer.parseInt(p.getName())));
  }
}
