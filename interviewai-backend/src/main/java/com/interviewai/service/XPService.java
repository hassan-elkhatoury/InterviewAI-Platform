package com.interviewai.service;

import com.interviewai.entity.Progress;
import com.interviewai.repository.ProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XPService {
  public static final int CORRECT_ANSWER_XP = 10;
  private final ProgressRepository progress;

  public XPService(ProgressRepository progress) {
    this.progress = progress;
  }

  // Callers hold the user's pessimistic lock. Unique event keys also protect against duplicate
  // rewards.
  @Transactional
  public int award(int userId, int courseId, String eventKey, int amount) {
    if (progress.existsByUserIdAndEventKey(userId, eventKey)) return 0;
    Progress event = new Progress();
    event.setUserId(userId);
    event.setCourseId(courseId);
    event.setEventKey(eventKey);
    event.setXp(amount);
    progress.save(event);
    return amount;
  }
}
