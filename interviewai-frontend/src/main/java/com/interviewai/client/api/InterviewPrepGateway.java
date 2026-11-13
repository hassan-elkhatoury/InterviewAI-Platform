package com.interviewai.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

public class InterviewPrepGateway {
  public List<InterviewPrepRecord> getAllInterviewPreps(int uid) {
    return ApiClient.getInstance()
        .get("/preparations", new TypeReference<List<InterviewPrepRecord>>() {});
  }

  public int getTechnicalCourseId(int uid) {
    return getAllInterviewPreps(uid).stream()
        .filter(p -> p.technicalCourseId != 0)
        .findFirst()
        .map(p -> p.technicalCourseId)
        .orElse(-1);
  }

  public int getSoftskillsCourseId(int uid) {
    return getAllInterviewPreps(uid).stream()
        .filter(p -> p.softskillsCourseId != 0)
        .findFirst()
        .map(p -> p.softskillsCourseId)
        .orElse(-1);
  }

  public boolean hasInterviewPrep(int uid) {
    return !getAllInterviewPreps(uid).isEmpty();
  }

  public static class InterviewPrepRecord {
    public int id;
    public int technicalCourseId;
    public int softskillsCourseId;
    public String technicalTitle;
    public String softskillsTitle;
    public java.util.Date createdAt;
  }
}
