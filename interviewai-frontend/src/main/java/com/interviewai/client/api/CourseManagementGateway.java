package com.interviewai.client.api;

import static com.interviewai.client.api.ApiClient.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.interviewai.client.model.CourseManagementDTO;
import java.util.*;

public class CourseManagementGateway {
  private final ApiClient api = ApiClient.getInstance();

  public List<CourseManagementDTO> getAllCourses() {
    return api.get("/admin/courses", new TypeReference<List<CourseManagementDTO>>() {});
  }

  public List<CourseManagementDTO> searchCourses(String term) {
    String s = term.toLowerCase(Locale.ROOT);
    return getAllCourses().stream()
        .filter(
            c ->
                (c.getCourseTitle() + " " + c.getUsername() + " " + c.getUserEmail())
                    .toLowerCase(Locale.ROOT)
                    .contains(s))
        .toList();
  }

  public List<CourseManagementDTO> getCoursesByStatus(String status) {
    return getAllCourses().stream().filter(c -> status.equals(c.getStatus())).toList();
  }

  public List<CourseManagementDTO> getCoursesByUser(int uid) {
    return getAllCourses().stream().filter(c -> c.getUserId() == uid).toList();
  }

  public boolean updateCourseStatus(int id, String status) {
    api.put("/admin/courses/" + id + "/status", body("status", status), Void.class);
    return true;
  }

  public boolean deleteCourse(int id) {
    api.delete("/admin/courses/" + id);
    return true;
  }

  public int[] getCourseStatistics() {
    var cs = getAllCourses();
    return new int[] {
      cs.size(),
      (int) cs.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count(),
      (int) cs.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count(),
      cs.stream().mapToInt(CourseManagementDTO::getChaptersCount).sum(),
      cs.stream().mapToInt(CourseManagementDTO::getQuestionsCount).sum(),
      (int)
          Math.round(
              cs.stream()
                  .mapToDouble(CourseManagementDTO::getCompletionPercentage)
                  .average()
                  .orElse(0))
    };
  }

  public List<String> getDistinctStatuses() {
    return List.of("ACTIVE", "COMPLETED", "ARCHIVED");
  }
}
