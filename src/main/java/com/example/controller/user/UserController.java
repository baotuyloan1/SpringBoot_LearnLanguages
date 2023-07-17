package com.example.controller.user;

import com.example.dto.user.LearnedVocabularyRequest;
import com.example.dto.user.UserVocabularyRequest;
import com.example.entity.Course;
import com.example.entity.Topic;
import com.example.payload.request.LoginRequest;
import com.example.payload.response.UserInfoResponse;
import com.example.security.jwt.JwtUtils;
import com.example.security.services.UserDetailsImpl;
import com.example.service.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * @author BAO 7/3/2023
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;
  private final AuthenticationManager authenticationManager;

  private final TopicService topicService;
  private final VocabularyService vocabularyService;

  private final UserVocabularyService userVocabularyService;
  private final CourseService courseService;
  private final JwtUtils jwtUtils;

  public UserController(
          UserService userService, AuthenticationManager authenticationManager, TopicService topicService, VocabularyService vocabularyService, UserVocabularyService userVocabularyService, CourseService courseService, JwtUtils jwtUtils) {
    this.userService = userService;
    this.authenticationManager = authenticationManager;
    this.topicService = topicService;
    this.vocabularyService = vocabularyService;
    this.userVocabularyService = userVocabularyService;
    this.courseService = courseService;
    this.jwtUtils = jwtUtils;
  }

  @GetMapping({"/courses"})
  public ResponseEntity<List<Course>> getAllCourses() {
    List<Course>  courseList= courseService.listCourse();
    return new ResponseEntity<>(courseList, HttpStatus.OK);
  }

  @GetMapping("/getTopicsByCourseId/{id}")
  public ResponseEntity<List<Topic>> getByIdCourse(@PathVariable("id") int courseId) {
    return new ResponseEntity<>(topicService.findByCourseId(courseId), HttpStatus.OK);
  }


  @GetMapping("/learn/{topicId}")
  public List<Map<String, Object>> listVocabulary(@PathVariable("topicId") int topicId) {
    return vocabularyService.getLearnVocabulary(topicId);
  }


  @PostMapping("/saveNewWord")
  public ResponseEntity<?> saveLearnedVocabulary(@RequestBody LearnedVocabularyRequest learnedVocabularyRequest){
    userVocabularyService.saveNewLearnedVocabulary(learnedVocabularyRequest.getIdVocabulary());
    return new ResponseEntity<>("Saved learned word",  HttpStatus.CREATED);
  }

  @PostMapping("/signin")
  public ResponseEntity<UserInfoResponse> singInUser(@RequestBody LoginRequest loginRequest) {
    Authentication authentication =
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
    List<String> roles =
            userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

    HttpHeaders headers = new HttpHeaders();
    /**
     * THong bao cho trình duyệt biết máy chủ đồng ý chia sẻ cookie và thông tin xác thực
     * do cấu hiình bên MvcConfig rồi nên không cần cấu hình lại
     */
//    headers.add("Access-Control-Allow-Credentials","true");
//    headers.add("Access-Control-Allow-Origin", "http://localhost:3000");
    headers.add(HttpHeaders.SET_COOKIE, jwtCookie.toString());
    return ResponseEntity.ok()
            .headers(headers)
            .body(
                    new UserInfoResponse(
                            userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
  }

  @PostMapping("/updateVocabulary")
  public ResponseEntity<?> updateLeanredVocabulary(@RequestBody UserVocabularyRequest userVocabularyRequest){
    userVocabularyService.updateLearnedVocabulary(userVocabularyRequest);
    return new ResponseEntity<>("Updated learned word", HttpStatus.OK);
  }


  @GetMapping("/getNextWordToReview")
  public ResponseEntity<?> getTimeToReview(){
    return ResponseEntity.ok().body(userVocabularyService.getNextWordToReview());
  }


}
