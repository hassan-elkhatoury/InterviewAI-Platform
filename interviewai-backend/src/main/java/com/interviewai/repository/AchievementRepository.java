package com.interviewai.repository;

import com.interviewai.entity.Achievement;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AchievementRepository extends JpaRepository<Achievement, Integer> {
  List<Achievement> findByUserId(Integer userId);

  boolean existsByUserIdAndCode(Integer userId, String code);
}
