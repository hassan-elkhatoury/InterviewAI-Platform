package com.interviewai.repository;

import com.interviewai.entity.Choice;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ChoiceRepository extends JpaRepository<Choice, Integer> {
  List<Choice> findByQuestionIdOrderByIdAsc(Integer questionId);
}
