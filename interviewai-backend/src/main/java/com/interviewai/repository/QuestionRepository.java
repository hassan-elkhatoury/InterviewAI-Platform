package com.interviewai.repository;

import com.interviewai.entity.Question;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
  List<Question> findByChapterIdOrderByIdAsc(Integer chapterId);
}
