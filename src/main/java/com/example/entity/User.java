package com.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * @author BAO 7/3/2023
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames =  "username"),
        @UniqueConstraint(columnNames = "email")
})
@Entity
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;


  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST})
  @JoinTable(
      name = "user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  @JsonIgnoreProperties("users")
  private List<Role> roles = new ArrayList<>();

  private String firstName;
  private String lastName;
  private int countWords;
  private String password;

  @Email
  @Column(name = "email",nullable = false)
  private String email;
  private String phone;

  @OneToMany(mappedBy = "user")
  private List<Device> devices;




}
