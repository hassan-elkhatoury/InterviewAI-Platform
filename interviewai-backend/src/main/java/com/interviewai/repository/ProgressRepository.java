package com.interviewai.repository;

import com.interviewai.entity.Progress;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ProgressRepository extends JpaRepository<Progress, Integer> {
  List<Progress> findByUserId(Integer userId);

  boolean existsByUserIdAndEventKey(Integer userId, String eventKey);
}
