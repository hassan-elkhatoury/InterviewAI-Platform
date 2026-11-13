package com.interviewai.service;

import com.interviewai.dto.*;
import com.interviewai.entity.*;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurriculumService {
  private final CourseRepository courses;
  private final LessonRepository lessons;
  private final QuestionRepository questions;
  private final ChoiceRepository choices;
  private final InterviewPrepRepository preps;

  public CurriculumService(
      CourseRepository courses,
      LessonRepository lessons,
      QuestionRepository questions,
      ChoiceRepository choices,
      InterviewPrepRepository preps) {
    this.courses = courses;
    this.lessons = lessons;
    this.questions = questions;
    this.choices = choices;
    this.preps = preps;
  }

  public Course ownedCourse(int userId, int id) {
    return courses
        .findById(id)
        .filter(c -> c.getUserId() == userId)
        .orElseThrow(ApiException::notFound);
  }

  public Lesson ownedLesson(int userId, int id) {
    Lesson lesson = lessons.findById(id).orElseThrow(ApiException::notFound);
    ownedCourse(userId, lesson.getCourseId());
    return lesson;
  }

  public Question ownedQuestion(int userId, int id) {
    Question question = questions.findById(id).orElseThrow(ApiException::notFound);
    ownedLesson(userId, question.getChapterId());
    return question;
  }

  public List<Views.Course> courses(int userId) {
    return courses.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::view).toList();
  }

  public Views.Course course(int userId, int id) {
    return view(ownedCourse(userId, id));
  }

  public List<Views.Lesson> lessons(int userId, int courseId) {
    ownedCourse(userId, courseId);
    return lessons.findByCourseIdOrderByChapterNumberAsc(courseId).stream()
        .map(this::view)
        .toList();
  }

  public Views.Lesson lesson(int userId, int lessonId) {
    return view(ownedLesson(userId, lessonId));
  }

  public List<Views.Question> questions(int userId, int lessonId) {
    ownedLesson(userId, lessonId);
    return questions.findByChapterIdOrderByIdAsc(lessonId).stream().map(this::view).toList();
  }

  public List<Views.Question> review(int userId) {
    return courses(userId).stream()
        .flatMap(c -> c.chapters().stream())
        .flatMap(l -> l.questions().stream())
        .filter(q -> "INCORRECT".equals(q.status()) || "IN_PROGRESS".equals(q.status()))
        .toList();
  }

  public Views.Course view(Course course) {
    var chapters =
        lessons.findByCourseIdOrderByChapterNumberAsc(course.getId()).stream()
            .map(this::view)
            .toList();
    double percent =
        chapters.isEmpty()
            ? 0
            : 100.0
                * chapters.stream().filter(c -> "COMPLETED".equals(c.status())).count()
                / chapters.size();
    return new Views.Course(
        course.getId(),
        course.getUserId(),
        course.getCourseTitle(),
        course.getStatus(),
        course.getCreatedAt(),
        chapters,
        percent);
  }

  public Views.Lesson view(Lesson lesson) {
    var items =
        questions.findByChapterIdOrderByIdAsc(lesson.getId()).stream().map(this::view).toList();
    return new Views.Lesson(
        lesson.getId(),
        lesson.getCourseId(),
        lesson.getChapterNumber(),
        lesson.getName(),
        lesson.getDescription(),
        lesson.getStatus(),
        items,
        items.size(),
        (int) items.stream().filter(q -> "COMPLETED".equals(q.status())).count());
  }

  private Views.Question view(Question q) {
    boolean answered = !"NOT_STARTED".equals(q.getStatus());
    return new Views.Question(
        q.getId(),
        q.getQuestion(),
        q.getQuestionType(),
        choices.findByQuestionIdOrderByIdAsc(q.getId()).stream()
            .map(Choice::getChoiceText)
            .toList(),
        answered ? q.getCorrectAnswer() : null,
        answered ? q.getExplanation() : null,
        q.getStatus());
  }

  public List<Map<String, Object>> preparations(int userId) {
    return preps.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(
            p -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("id", p.getId());
              m.put("technicalCourseId", p.getTechnicalCourseId());
              m.put("softskillsCourseId", p.getSoftskillsCourseId());
              m.put("createdAt", p.getCreatedAt());
              m.put(
                  "technicalTitle",
                  p.getTechnicalCourseId() == null
                      ? "Deleted course"
                      : courses
                          .findById(p.getTechnicalCourseId())
                          .map(Course::getCourseTitle)
                          .orElse(""));
              m.put(
                  "softskillsTitle",
                  p.getSoftskillsCourseId() == null
                      ? "Deleted course"
                      : courses
                          .findById(p.getSoftskillsCourseId())
                          .map(Course::getCourseTitle)
                          .orElse(""));
              return m;
            })
        .toList();
  }

  @Transactional
  public List<Views.Course> savePreparation(
      int userId, Requests.GeneratedCourse technical, Requests.GeneratedCourse softskills) {
    Course first = saveCourse(userId, technical), second = saveCourse(userId, softskills);
    InterviewPrep prep = new InterviewPrep();
    prep.setUserId(userId);
    prep.setTechnicalCourseId(first.getId());
    prep.setSoftskillsCourseId(second.getId());
    preps.save(prep);
    return List.of(view(first), view(second));
  }

  private Course saveCourse(int userId, Requests.GeneratedCourse data) {
    Course course = new Course();
    course.setUserId(userId);
    course.setCourseTitle(data.courseTitle());
    courses.save(course);
    int number = 0;
    for (var chapter : data.chapters()) {
      Lesson lesson = new Lesson();
      lesson.setCourseId(course.getId());
      lesson.setChapterNumber(++number);
      lesson.setName(chapter.name());
      lesson.setDescription(chapter.description());
      lessons.save(lesson);
      for (var item : chapter.questions()) {
        if ("MULTIPLE_CHOICE".equals(item.questionType())
            && (item.choices().size() < 2 || !item.choices().contains(item.correctAnswer())))
          throw new ApiException(
              org.springframework.http.HttpStatus.BAD_GATEWAY,
              "AI returned an invalid multiple-choice question");
        Question question = new Question();
        question.setChapterId(lesson.getId());
        question.setQuestion(item.question());
        question.setQuestionType(item.questionType());
        question.setCorrectAnswer(item.correctAnswer());
        question.setExplanation(item.explanation());
        questions.save(question);
        for (String text : item.choices()) {
          Choice choice = new Choice();
          choice.setQuestionId(question.getId());
          choice.setChoiceText(text);
          choices.save(choice);
        }
      }
    }
    return course;
  }
}
