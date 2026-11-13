package com.interviewai.repository;

import com.interviewai.entity.Lesson;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {
  List<Lesson> findByCourseIdOrderByChapterNumberAsc(Integer courseId);
}
