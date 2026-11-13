package com.interviewai.service;

import com.interviewai.dto.Views;
import com.interviewai.entity.*;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestService {
  private final QuestRepository quests;
  private final QuestClaimRepository claims;
  private final UserRepository users;
  private final CourseRepository courses;
  private final ProgressService progress;
  private final XPService xp;
  private final AchievementService achievements;

  public QuestService(
      QuestRepository quests,
      QuestClaimRepository claims,
      UserRepository users,
      CourseRepository courses,
      ProgressService progress,
      XPService xp,
      AchievementService achievements) {
    this.quests = quests;
    this.claims = claims;
    this.users = users;
    this.courses = courses;
    this.progress = progress;
    this.xp = xp;
    this.achievements = achievements;
  }

  private LocalDate start(Quest q) {
    return "DAILY".equals(q.getPeriod())
        ? ProgressService.today()
        : ProgressService.today().withDayOfMonth(1);
  }

  private int current(int uid, Quest q) {
    return "DAILY".equals(q.getPeriod())
        ? progress.answeredSince(uid, start(q), false)
        : progress.completedThisMonth(uid);
  }

  @Transactional(readOnly = true)
  public List<Views.Quest> list(int uid) {
    return quests.findAll().stream()
        .map(
            q ->
                new Views.Quest(
                    q.getId(),
                    q.getCode(),
                    q.getTitle(),
                    q.getTargetValue(),
                    q.getXpReward(),
                    q.getPeriod(),
                    current(uid, q),
                    claims.existsByUserIdAndQuestIdAndPeriodStart(uid, q.getId(), start(q))))
        .toList();
  }

  @Transactional
  public int claim(int uid, int id) {
    users.lockById(uid).orElseThrow(ApiException::notFound);
    Quest quest = quests.findById(id).orElseThrow(ApiException::notFound);
    if (claims.existsByUserIdAndQuestIdAndPeriodStart(uid, id, start(quest)))
      throw ApiException.conflict("Quest already claimed for this period");
    if (current(uid, quest) < quest.getTargetValue())
      throw ApiException.conflict("Quest target has not been reached");
    Course course =
        courses.findByUserIdOrderByCreatedAtDesc(uid).stream()
            .findFirst()
            .orElseThrow(ApiException::notFound);
    QuestClaim claim = new QuestClaim();
    claim.setUserId(uid);
    claim.setQuestId(id);
    claim.setPeriodStart(start(quest));
    claims.save(claim);
    int reward =
        xp.award(uid, course.getId(), "quest:" + id + ":" + start(quest), quest.getXpReward());
    achievements.check(uid);
    return reward;
  }
}
