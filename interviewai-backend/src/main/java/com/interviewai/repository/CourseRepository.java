package com.interviewai.repository;

import com.interviewai.entity.Course;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CourseRepository extends JpaRepository<Course, Integer> {
  List<Course> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
