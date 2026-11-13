package com.interviewai.client.api;

import java.util.List;

public record ResumeProfile(
    List<String> skills, String seniority, String experience, List<String> tracks) {}
