package com.interviewai.repository;

import com.interviewai.entity.QuestClaim;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface QuestClaimRepository extends JpaRepository<QuestClaim, Integer> {
  boolean existsByUserIdAndQuestIdAndPeriodStart(
      Integer userId, Integer questId, java.time.LocalDate periodStart);
}
