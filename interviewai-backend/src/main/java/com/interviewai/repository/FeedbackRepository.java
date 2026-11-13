package com.interviewai.repository;

import com.interviewai.entity.Feedback;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
  List<Feedback> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
