package com.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * @author BAO 7/22/2023
 */
@Configuration
@PropertySource({"classpath:application-dev.properties"})
@Getter
@Setter
@Component
public class PropertiesConfig {
  @Value("${dir.resource.audioWord}")
  private String pathAudioWord;

  @Value("${dir.resource.audioSentence}")
  private String pathAudioSentence;

  @Value("${dir.resource.imgWord}")
  private String pathImgWord;

  @Value("${dir.resource.imgCourse}")
  private String pathImgCourse;

  @Value("${dir.resource.imgTopic}")
  private String pathImgTopic;

  @Value("${learnEnglish.app.defaultEF}")
  private float defaultEF;

  @Value("${learnEnglish.app.timeFirstDay}")
  private int defaultFirstDay;

  @Value("${learnEnglish.app.timeSecondDay}")
  private int defaultSecondDay;

  @Value("${learnEnglish.app.leastEF}")
  private float leastEF;

  @Value("${learnEnglish.app.highestEF}")
  private float highestEF;

  @Value("${learnEnglish.app.defaultQ}")
  private short defaultQ;

  @Value("${app.firebase-configuration-file}")
  private String filebaseConfigPath;

  @Value("${vapid.notification.public.key}")
  private String vapidPublicKey;

  @Value("${vapid.notification.private.key}")
  private String vapidPrivateKey;

  @Value("${learnEnglish.app.highestLv1}")
  private int highestLearnedLv1;

  @Value("${learnEnglish.app.highestLv2}")
  private int highestLearnedLv2;

  @Value("${learnEnglish.app.highestLv3}")
  private int highestLearnedLv3;

  @Value("${learnEnglish.app.highestLv4}")
  private int highestLearnedLv4;
}
