package com.interviewai.repository;

import com.interviewai.entity.Quest;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface QuestRepository extends JpaRepository<Quest, Integer> {
  Optional<Quest> findByCode(String code);
}
