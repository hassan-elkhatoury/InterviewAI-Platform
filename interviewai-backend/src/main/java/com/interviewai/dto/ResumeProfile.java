package com.interviewai.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record ResumeProfile(
    @NotNull @Size(max = 50) List<@NotBlank @Size(max = 100) String> skills,
    @NotBlank @Size(max = 100) String seniority,
    @NotBlank @Size(max = 3000) String experience,
    @NotEmpty @Size(max = 12) List<@NotBlank @Size(max = 200) String> tracks) {}
