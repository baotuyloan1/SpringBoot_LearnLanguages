package com.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author BAO 7/5/2023
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "topic")
public class Topic {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private int id;

  private String titleEn;

  private String titleVn;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @ManyToOne
  @JoinColumn(name = "id_course")
  private Course course;

  private String img;

  @OneToMany(mappedBy = "topic",cascade = CascadeType.ALL)
  private List<Vocabulary> vocabulary = new ArrayList<>();
}
