package com.example.controller.user;

import com.example.dto.user.*;
import com.example.service.*;
import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

/**
 * @author BAO 7/3/2023
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final TopicService topicService;
  private final VocabularyService vocabularyService;
  private final UserVocabularyService userVocabularyService;
  private final CourseService courseService;

  @GetMapping("/courses")
  public ResponseEntity<List<UserCourseRes>> getAllCourses() {
    return new ResponseEntity<>(courseService.userGetCourses(), HttpStatus.OK);
  }

  @GetMapping("/courses/{id}/topics")
  public ResponseEntity<List<UserTopicRes>> getByIdCourse(@PathVariable("id") int courseId) {
    return new ResponseEntity<>(topicService.userGetTopics(courseId), HttpStatus.OK);
  }

  @GetMapping("/topics/{id}/vocabularies")
  public ResponseEntity<List<UserLearnRes>> listVocabulary(@PathVariable("id") int topicId) {
    return new ResponseEntity<>(userService.getVocabulariesByTopicId(topicId), HttpStatus.OK);
  }

  @PostMapping("/saveNewWord")
  public ResponseEntity<?> saveLearnedVocabulary(
      @RequestBody LearnedVocabularyRequest learnedVocabularyRequest) {
    userVocabularyService.saveNewLearnedVocabulary(learnedVocabularyRequest.getIdVocabulary());
    return new ResponseEntity<>("Saved learned word", HttpStatus.CREATED);
  }

  @PostMapping("/updateVocabulary")
  public ResponseEntity<?> updateLeanredVocabulary(
      @RequestBody UserVocabularyRequest userVocabularyRequest) {
    userVocabularyService.updateLearnedVocabulary(userVocabularyRequest);
    return new ResponseEntity<>("Updated learned word", HttpStatus.OK);
  }

  @GetMapping("/getNextWordToReview")
  public ResponseEntity<?> getTimeToReview() {
    return ResponseEntity.ok().body(userVocabularyService.getNextWordToReview());
  }
}
