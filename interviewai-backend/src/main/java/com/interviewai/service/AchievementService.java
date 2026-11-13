package com.interviewai.service;

import com.interviewai.dto.Views;
import com.interviewai.entity.Achievement;
import com.interviewai.repository.AchievementRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementService {
  private final AchievementRepository achievements;
  private final ProgressService progress;

  public AchievementService(AchievementRepository achievements, ProgressService progress) {
    this.achievements = achievements;
    this.progress = progress;
  }

  @Transactional
  public void check(int userId) {
    if (progress.totalXP(userId) > 0) unlock(userId, "FIRST_STEPS", "First Steps");
    if (progress.totalXP(userId) >= 1000) unlock(userId, "XP_1000", "1000 XP");
    if (progress.streak(userId) >= 7) unlock(userId, "CONSISTENCY_HERO", "Consistency Hero");
  }

  private void unlock(int userId, String code, String title) {
    if (achievements.existsByUserIdAndCode(userId, code)) return;
    Achievement a = new Achievement();
    a.setUserId(userId);
    a.setCode(code);
    a.setTitle(title);
    achievements.save(a);
  }

  @Transactional(readOnly = true)
  public List<Views.Achievement> list(int userId) {
    return achievements.findByUserId(userId).stream()
        .map(a -> new Views.Achievement(a.getCode(), a.getTitle(), a.getEarnedAt()))
        .toList();
  }
}
