package com.interviewai.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.interviewai.client.model.*;
import java.util.*;

public class CourseGateway {
  private final ApiClient api = ApiClient.getInstance();

  public List<GeneratedCourse> getCourses() {
    return api.get("/courses", new TypeReference<List<GeneratedCourse>>() {});
  }

  public Boolean checkUserCourse(int uid) {
    return !getCourses().isEmpty();
  }

  public Chapter getChapterById(int id) {
    return api.get("/lessons/" + id, Chapter.class);
  }
}
