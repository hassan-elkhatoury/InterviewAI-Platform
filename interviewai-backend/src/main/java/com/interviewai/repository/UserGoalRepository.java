package com.interviewai.repository;

import com.interviewai.entity.UserGoal;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserGoalRepository extends JpaRepository<UserGoal, Integer> {
  List<UserGoal> findByUserIdAndActiveTrueOrderByCreatedAtAsc(Integer userId);
}
