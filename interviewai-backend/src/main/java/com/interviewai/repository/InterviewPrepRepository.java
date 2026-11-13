package com.interviewai.repository;

import com.interviewai.entity.InterviewPrep;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface InterviewPrepRepository extends JpaRepository<InterviewPrep, Integer> {
  List<InterviewPrep> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
