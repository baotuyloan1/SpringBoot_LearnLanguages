package com.example.convertRequestParamToObject;

import com.example.entity.VocabularyEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author BAO
 * 7/2/2023
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyUpload {
    private MultipartFile audioWord;
    private MultipartFile audioSentence;
    private VocabularyEntity vocabularyEntity;
}
