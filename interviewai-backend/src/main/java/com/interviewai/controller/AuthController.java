package com.interviewai.controller;

import com.interviewai.dto.*;
import com.interviewai.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Views.Auth> register(@Valid @RequestBody Requests.Register request) {
    return ApiResponse.ok(auth.register(request));
  }

  @PostMapping("/login")
  public ApiResponse<Views.Auth> login(@Valid @RequestBody Requests.Login request) {
    return ApiResponse.ok(auth.login(request));
  }
}
