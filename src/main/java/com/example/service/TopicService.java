package com.example.service;

import com.example.dto.admin.AdminTopicRes;
import com.example.dto.user.UserTopicRes;
import com.example.entity.Topic;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author BAO 7/7/2023
 */
public interface TopicService {
    Topic save(Topic topic, MultipartFile img);

    List<AdminTopicRes> adminFindByCourseId(int courseId);

    List<AdminTopicRes> listAll();

    void deleteTopicById(int id);

    List<UserTopicRes> userGetTopics(int courseId);

}
