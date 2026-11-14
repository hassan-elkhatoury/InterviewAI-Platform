package com.interviewai.service;

import com.interviewai.dto.*;
import com.interviewai.entity.*;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EvaluationService {
  private final CurriculumService curriculum;
  private final AIService ai;
  private final UserRepository users;
  private final QuestionRepository questions;
  private final LessonRepository lessons;
  private final CourseRepository courses;
  private final FeedbackRepository feedback;
  private final XPService xp;
  private final AchievementService achievements;
  private final TransactionTemplate transactions;

  public EvaluationService(
      CurriculumService curriculum,
      AIService ai,
      UserRepository users,
      QuestionRepository questions,
      LessonRepository lessons,
      CourseRepository courses,
      FeedbackRepository feedback,
      XPService xp,
      AchievementService achievements,
      TransactionTemplate transactions) {
    this.curriculum = curriculum;
    this.ai = ai;
    this.users = users;
    this.questions = questions;
    this.lessons = lessons;
    this.courses = courses;
    this.feedback = feedback;
    this.xp = xp;
    this.achievements = achievements;
    this.transactions = transactions;
  }

  public Views.Evaluation evaluate(int userId, Requests.Evaluate request) {
    Question question = curriculum.ownedQuestion(userId, request.questionId());
    AIService.Grade grade;
    if ("MULTIPLE_CHOICE".equals(question.getQuestionType())) {
      boolean correct =
          question.getCorrectAnswer().trim().equalsIgnoreCase(request.userAnswer().trim());
      grade =
          new AIService.Grade(
              correct ? 100 : 0,
              correct ? "You selected the correct answer." : "You attempted the question.",
              correct ? "Keep practicing." : "Review the explanation and try again.",
              question.getExplanation() == null ? "" : question.getExplanation());
    } else
      grade =
          ai.validateAnswer(
              question.getQuestion(), question.getCorrectAnswer(), request.userAnswer());
    // Do not hold a database transaction or lock while waiting for the AI provider.
    return transactions.execute(tx -> persist(userId, request, grade));
  }

  public Views.Evaluation saveStreamed(
      int userId, Requests.Evaluate request, AIService.Grade grade) {
    return transactions.execute(tx -> persist(userId, request, grade));
  }

  private Views.Evaluation persist(
      int userId, Requests.Evaluate request, AIService.Grade grade) {
    User user = users.lockById(userId).filter(User::isActive).orElseThrow(ApiException::notFound);
    Question question = curriculum.ownedQuestion(user.getId(), request.questionId());
    Lesson lesson = curriculum.ownedLesson(userId, question.getChapterId());
    boolean alreadyCompleted = "COMPLETED".equals(question.getStatus());
    boolean passed = grade.score() >= 70;
    question.setStatus(passed || alreadyCompleted ? "COMPLETED" : "INCORRECT");
    question.setUpdatedAt(LocalDateTime.now(Clock.systemUTC()));
    questions.saveAndFlush(question);
    Feedback entry = new Feedback();
    entry.setUserId(userId);
    entry.setCourseId(lesson.getCourseId());
    entry.setQuestionId(question.getId());
    entry.setUserAnswer(request.userAnswer());
    entry.setScore(grade.score());
    entry.setStrengths(grade.strengths());
    entry.setImprovements(grade.improvements());
    entry.setExplanation(grade.explanation());
    entry.setMessage(grade.explanation());
    feedback.save(entry);
    int reward =
        passed && !alreadyCompleted
            ? xp.award(
                userId,
                lesson.getCourseId(),
                "question:" + question.getId(),
                XPService.CORRECT_ANSWER_XP)
            : 0;
    boolean complete =
        questions.findByChapterIdOrderByIdAsc(lesson.getId()).stream()
            .allMatch(q -> "COMPLETED".equals(q.getStatus()));
    lesson.setStatus(complete ? "COMPLETED" : "IN_PROGRESS");
    if (complete && lesson.getCompletedAt() == null)
      lesson.setCompletedAt(LocalDateTime.now(Clock.systemUTC()));
    lessons.saveAndFlush(lesson);
    if (lessons.findByCourseIdOrderByChapterNumberAsc(lesson.getCourseId()).stream()
        .allMatch(l -> "COMPLETED".equals(l.getStatus()))) {
      Course course = curriculum.ownedCourse(userId, lesson.getCourseId());
      course.setStatus("COMPLETED");
      courses.save(course);
    }
    achievements.check(userId);
    return new Views.Evaluation(
        entry.getId(),
        grade.score(),
        grade.strengths(),
        grade.improvements(),
        grade.explanation(),
        question.getCorrectAnswer(),
        question.getStatus(),
        reward);
  }
}
