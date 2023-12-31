package com.example.service.impl;

import com.example.config.PropertiesConfig;
import com.example.dto.fcm.PushNotificationRequest;
import com.example.dto.BaseResApi;
import com.example.dto.user.TypeLearnRes;
import com.example.dto.user.TypeQuestionRes;
import com.example.dto.user.UserVocabularyRequest;
import com.example.dto.user.learn.*;
import com.example.dto.user.learn.UserLearnRes;
import com.example.dto.user.review.UserNextWordsReq;
import com.example.dto.user.review.UserReviewReq;
import com.example.dto.user.review.UserReviewRes;
import com.example.entity.*;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.AnswerMapper;
import com.example.mapper.VocabularyMapper;
import com.example.payload.response.UserInfoResponse;
import com.example.repository.*;
import com.example.security.services.UserDetailsImpl;
import com.example.service.UserVocabularyService;
import com.example.service.VocabularyService;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author BAO 7/13/2023
 */
@Service
@AllArgsConstructor
public class UserVocabularyServiceImpl implements UserVocabularyService {
  private final UserVocabularyRepository userVocabularyRepository;
  private final VocabularyService vocabularyService;
  private final PropertiesConfig env;
  private final VocabularyMapper vocabularyMapper;
  private final AnswerMapper answerMapper;
  private final Random random;
  private final QuestionRepository questionRepository;
  private final VocabularyRepository vocabularyRepository;
  private final UserRepository userRepository;
  private final DeviceRepository deviceRepository;

  private void updateQAndEFInRightAnswer(UserVocabulary userVocabulary) {
    short currentQ = userVocabulary.getQ();
    int maxQ = 5;
    if (currentQ < maxQ) {
      userVocabulary.setQ(++currentQ);
    } else {
      userVocabulary.setQ(currentQ);
    }
    float currentEF = calculateCurrentEF(userVocabulary.getEf(), currentQ);
    userVocabulary.setEf(currentEF);
  }

  private void updateQAndEFInFalseAnswer(UserVocabulary userVocabulary) {
    short currentQ = userVocabulary.getQ();
    int minQ = 0;

    if (currentQ < minQ) {
      userVocabulary.setQ(currentQ);
    } else {
      currentQ = (short) (currentQ - 2);
      userVocabulary.setQ(currentQ);
    }
    float currentEF = calculateCurrentEF(userVocabulary.getEf(), currentQ);
    userVocabulary.setEf(currentEF);
  }

  private Date calculateDateToReview(Date currentDate, long dayInterval) {
    long endDateMillis = currentDate.getTime() + TimeUnit.DAYS.toMillis(dayInterval);
    return new Date(endDateMillis);
  }

  public UserVocabulary createUserVocabulary(Vocabulary vocabulary, long userId) {
    UserVocabulary userVocabulary = new UserVocabulary();
    Date currentDate = new Date();
    userVocabulary.setId(new UserVocabularyId(userId, vocabulary.getId()));
    userVocabulary.setSubmitDate(currentDate);
    userVocabulary.setEf(env.getDefaultEF());
    userVocabulary.setQ(env.getDefaultQ());
    userVocabulary.setCountLearn(1);
    userVocabulary.setDayInterval(env.getDefaultFirstDay());
    userVocabulary.setReviewDate(calculateDateToReview(currentDate, env.getDefaultFirstDay()));
    return userVocabularyRepository.save(userVocabulary);
  }

  @Transactional
  @Override
  public List<UserNextWordsReq> getWordToReview() {
    Date currentDate = new Date();
    List<UserVocabulary> userVocabularyList;
    UserDetailsImpl userDetails =
        (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Date nearestDate = userVocabularyRepository.getNearestDate(userDetails.getId());
    if (currentDate.compareTo(nearestDate) >= 1) {
      userVocabularyList =
          userVocabularyRepository.getVocabulariesAfterCurrent(userDetails.getId(), new Date());
    } else {
      int oneHour = 60 * 60 * 1000;
      userVocabularyList =
          userVocabularyRepository.getVocabulariesAfterCurrent(
              userDetails.getId(), new Date(nearestDate.getTime() + oneHour));
    }

    return convertReviewVocabulariesToUserRes(userVocabularyList);
  }

  @Transactional
  @Override
  public List<UserLearnRes> getVocabulariesByTopicId(int topicId) {
    UserDetailsImpl userDetails =
        (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<Vocabulary> notLearnedWords =
        vocabularyService.findWordsNotLearnedByUserIdInTopicId(userDetails.getId(), topicId);
    return convertNewVocabulariesToUserRes(notLearnedWords);
  }

  @Transactional
  @Override
  public UserVocabularyId createUserVocabulary(UserLearnNewReq req) {
    UserDetailsImpl userDetails =
        (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Vocabulary vocabulary =
        vocabularyRepository
            .findById(req.getIdVocabulary())
            .orElseThrow(() -> new ResourceNotFoundException(false, "Vocabulary not found"));
    long idUser = userDetails.getId();
    UserVocabulary userVocabulary = createUserVocabulary(vocabulary, idUser);
    return userVocabulary.getId();
  }

  @Override
  public UserReviewRes updateReviewSelect(UserReviewSelectionReq req) {
    UserDetailsImpl userDetails =
        (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    UserReviewRes res = new UserReviewRes();
    Question question =
        questionRepository
            .findById(req.getIdQuestion())
            .orElseThrow(() -> new ResourceNotFoundException(false, "Question can't be find"));
    boolean isLearnAgain = !isRightAnswer(req, question);
    Vocabulary vocabulary =
        vocabularyRepository
            .findById(question.getVocabulary().getId())
            .orElseThrow(() -> new ResourceNotFoundException(false, "Vocabulary not found"));

    UserVocabularyId id = new UserVocabularyId(userDetails.getId(), vocabulary.getId());
    UserVocabulary userVocabulary =
        userVocabularyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(false, " User Vocabulary not found"));
    Date currentDate = new Date();
    userVocabulary.setSubmitDate(currentDate);
    updateUserVocabulary(isLearnAgain, userVocabulary, currentDate);
    res.setLearnAgain(isLearnAgain);
    return res;
  }

  @Override
  public UserReviewRes updateReview(UserReviewReq req) {
    UserDetailsImpl userDetails =
        (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    UserVocabularyId id = new UserVocabularyId(userDetails.getId(), req.getVocabularyId());
    UserVocabulary userVocabulary =
        userVocabularyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(false, " User Vocabulary not found"));
    Date currentDate = new Date();
    Vocabulary vocabulary =
        vocabularyRepository
            .findById(req.getVocabularyId())
            .orElseThrow(() -> new ResourceNotFoundException(false, "Vocabulary not found"));
    boolean isLearnAgain = !isRightAnswer(req, vocabulary);
    updateUserVocabulary(isLearnAgain, userVocabulary, currentDate);
    UserReviewRes res = new UserReviewRes();
    res.setLearnAgain(isLearnAgain);
    return res;
  }

  @Override
  public UserInfoResponse getInfo() {
    return null;
  }

  @Override
  public void saveDeviceToken(PushNotificationRequest req) {
    boolean isExistDevice = deviceRepository.existsByDeviceToken(req.getDeviceToken());
    if (!isExistDevice) {
      Device device = new Device();
      device.setDeviceToken(req.getDeviceToken());
      UserDetailsImpl userDetails =
          (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      User user = new User();
      user.setId(userDetails.getId());
      device.setUser(user);
      device.setDeviceType(req.getDeviceType());
      deviceRepository.save(device);
    }
  }

  @Override
  public void updateLearnedVocabulary(UserVocabularyRequest userVocabularyRequest) {}

  private UserLearnedWordsRes countLearnedWords(List<UserVocabulary> userVocabularies) {
    UserLearnedWordsRes data = new UserLearnedWordsRes();

    int countLv1 = 0;
    int countLv2 = 0;
    int countLv3 = 0;
    int countLv4 = 0;
    int countLv5 = 0;
    for (UserVocabulary userVocabulary : userVocabularies) {
      if (userVocabulary.getDayInterval() < env.getHighestLearnedLv1()) {
        ++countLv1;
      } else if (userVocabulary.getDayInterval() < env.getHighestLearnedLv2()) {
        ++countLv2;
      } else if (userVocabulary.getDayInterval() < env.getHighestLearnedLv3()) {
        ++countLv3;
      } else if (userVocabulary.getDayInterval() < env.getHighestLearnedLv4()) {
        ++countLv4;
      } else {
        ++countLv5;
      }
    }
    data.setCountWordsLv1(countLv1);
    data.setCountWordsLv2(countLv2);
    data.setCountWordsLv3(countLv3);
    data.setCountWordsLv4(countLv4);
    data.setCountWordsLv5(countLv5);
    return data;
  }

  @Override
  public BaseResApi getLearnedWords() {
    UserDetailsImpl userDetails =
        (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<UserVocabulary> userVocabularies =
        userVocabularyRepository.getLearnedVocabulariesByUserId(userDetails.getId());

    UserLearnedWordsRes data = countLearnedWords(userVocabularies);

    /** Level 5, day interval between (120,...) */
    BaseResApi res = new BaseResApi();
    res.setMessage("sucess");
    res.setData(data);
    return res;
  }

  private UserVocabularyId updateUserVocabulary(
      boolean isLearnAgain, UserVocabulary userVocabulary, Date currentDate) {
    if (isLearnAgain) {
      updateQAndEFInFalseAnswer(userVocabulary);
      short currentQ = userVocabulary.getQ();
      int minFalse = 3;
      int requireQToLearnAgain = 4;
      if (currentQ < minFalse) {
        userVocabulary.setCountLearn(0);
        userVocabulary.setQ((short) 3);
        userVocabulary.setDayInterval(0);
        userVocabulary.setDayInterval(env.getDefaultFirstDay());
      }
      if (currentQ < requireQToLearnAgain) {
        userVocabulary.setReviewDate(currentDate);
      } else {
        Date reviewDate =
            calculateDateToReview(userVocabulary.getSubmitDate(), userVocabulary.getDayInterval());
        userVocabulary.setReviewDate(reviewDate);
      }
    } else {
      updateQAndEFInRightAnswer(userVocabulary);
      updateUserVocabularyWithRightAnswer(userVocabulary, currentDate);
    }
    UserVocabulary updated = userVocabularyRepository.save(userVocabulary);
    return updated.getId();
  }

  private void updateUserVocabularyWithRightAnswer(
      UserVocabulary userVocabulary, Date currentDate) {
    int newCountLearn = 2;
    if (userVocabulary.getCountLearn() < newCountLearn) {
      int firstTime = 0;
      if (userVocabulary.getCountLearn() == firstTime) {
        userVocabulary.setDayInterval(env.getDefaultFirstDay());
        userVocabulary.setReviewDate(calculateDateToReview(currentDate, env.getDefaultFirstDay()));
      } else {
        userVocabulary.setDayInterval(env.getDefaultSecondDay());
        userVocabulary.setReviewDate(calculateDateToReview(currentDate, env.getDefaultSecondDay()));
      }
    } else {
      int dayInterval = (int) Math.floor(userVocabulary.getDayInterval() * userVocabulary.getEf());
      userVocabulary.setDayInterval(dayInterval);
      userVocabulary.setReviewDate(calculateDateToReview(currentDate, dayInterval));
    }
    userVocabulary.setCountLearn(userVocabulary.getCountLearn() + 1);
  }

  public float calculateCurrentEF(float currentEF, short q) {
    float ef = (float) (currentEF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)));
    if (ef < env.getLeastEF()) ef = env.getLeastEF();
    else if (ef > env.getHighestEF()) {
      ef = env.getHighestEF();
    }
    return ef;
  }

  private static boolean isRightAnswer(UserReviewSelectionReq req, Question question) {
    return question.getRightAnswer().getId() == req.getIdAnswer();
  }

  private static boolean isRightAnswer(UserReviewReq req, Vocabulary vocabulary) {
    return req.getAnswer().trim().equalsIgnoreCase(vocabulary.getWord());
  }

  private List<UserLearnRes> convertNewVocabulariesToUserRes(List<Vocabulary> vocabularies) {
    return vocabularies.stream()
        .map(
            vocabulary -> {
              Set<TypeLearnRes> typeLearnResList = new LinkedHashSet<>();
              addInfoType(typeLearnResList);
              addSelectType(typeLearnResList, vocabulary);
              addMeaningType(typeLearnResList);
              addListeningType(typeLearnResList);
              UserLearnRes userLearnRes = vocabularyMapper.vocabularyToUserLearnRes(vocabulary);
              userLearnRes.setLearnTypes(typeLearnResList);
              return userLearnRes;
            })
        .toList();
  }

  private void addListeningType(Set<TypeLearnRes> typeLearnResList) {
    typeLearnResList.add(new TypeLearnRes("listen"));
  }

  private void addMeaningType(Set<TypeLearnRes> typeLearnResList) {
    typeLearnResList.add(new TypeLearnRes("mean"));
  }

  private void addInfoType(Set<TypeLearnRes> typeLearnResList) {
    typeLearnResList.add(new TypeLearnRes("info"));
  }

  private void addSelectType(Set<TypeLearnRes> typeLearnResList, Vocabulary vocabulary) {
    List<Question> questionList = vocabulary.getQuestion();
    if (!questionList.isEmpty()) {
      Question question = getQuestionRandom(questionList);
      TypeQuestionRes typeQuestionRes = new TypeQuestionRes();
      typeQuestionRes.setIdQuestion(question.getId());
      typeQuestionRes.setQuestion(question.getQuestion());
      typeQuestionRes.setType("select");
      typeQuestionRes.setAnswers(answerMapper.answersToUserAnswersRes(question.getAnswers()));
      typeQuestionRes.setIdRightAnswer(question.getRightAnswer().getId());
      typeLearnResList.add(typeQuestionRes);
    }
  }

  private List<UserNextWordsReq> convertReviewVocabulariesToUserRes(
      List<UserVocabulary> userVocabylaries) {
    return userVocabylaries.stream()
        .map(
            userVocabulary -> {
              Set<TypeLearnRes> typeLearnResList = new LinkedHashSet<>();
              addRandomLearningTypes(typeLearnResList, userVocabulary.getVocabulary());
              UserNextWordsReq userLearnRes =
                  vocabularyMapper.vocabularyToUserNextWordsReq(userVocabulary);
              userLearnRes.setLearnTypes(typeLearnResList);
              return userLearnRes;
            })
        .toList();
  }

  private void addRandomLearningTypes(Set<TypeLearnRes> typeLearnResList, Vocabulary vocabulary) {
    int randomTypeQuestion = random.nextInt(12);

    if (randomTypeQuestion <= 2) {
      addSelectType(typeLearnResList, vocabulary);
    } else if (randomTypeQuestion <= 6) {
      addListeningType(typeLearnResList);
    } else {
      addMeaningType(typeLearnResList);
    }
  }

  private static Question getQuestionRandom(List<Question> questions) {
    int indexQuestion = (int) Math.floor(Math.random() * questions.size());
    return questions.get(indexQuestion);
  }
}
