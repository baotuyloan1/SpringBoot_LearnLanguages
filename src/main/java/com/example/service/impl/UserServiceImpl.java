package com.example.service.impl;

import com.example.entity.Role;
import com.example.entity.User;
import com.example.enums.RoleUser;
import com.example.exception.ResourceNotFoundException;
import com.example.payload.request.SignupRequest;
import com.example.payload.response.MessageResponse;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author BAO 7/3/2023
 */
@Service
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public List<User> getAllUser() {
    return userRepository.findAll();
  }

  @Transactional(rollbackOn = Exception.class)
  @Override
  public User signUpUser(SignupRequest signupRequest) {
    Set<RoleUser> enumRoles = signupRequest.getRoles();
    List<Role> roles = new LinkedList<>();
      if (enumRoles == null || enumRoles.isEmpty()) {
      Role userRole =
          roleRepository
              .findByName(RoleUser.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
      roles.add(userRole);
    } else {
      enumRoles.forEach(
          roleUser -> {
            Role role =
                roleRepository
                    .findByName(roleUser)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
            roles.add(role);
          });
    }
    User user =
        new User(
            signupRequest.getId(),
            signupRequest.getUsername(),
                roles,
            signupRequest.getFirstName(),
            signupRequest.getLastname(),
            0,
            passwordEncoder.encode(signupRequest.getPassword()),
            signupRequest.getEmail(),
            signupRequest.getPhone());
    return userRepository.save(user);
  }

  @Override
  public boolean exitsByUserName(String username) {
    return userRepository.existsByUsername(username);
  }

  @Override
  public boolean exitsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  @Override
  public ResponseEntity<MessageResponse> checkValidSignupRequest(SignupRequest signupRequest) {
    if (exitsByUserName(signupRequest.getUsername())) {
      return ResponseEntity.badRequest()
              .body(
                      new MessageResponse(
                              HttpStatus.BAD_REQUEST.value(), "Error: Username is already taken!!"));
    }
    if (exitsByEmail(signupRequest.getEmail())) {
      return ResponseEntity.badRequest()
              .body(
                      new MessageResponse(
                              HttpServletResponse.SC_BAD_REQUEST, "Error: Email is already in use!"));
    }
    return null;
  }

  @Override
  public User getInfoById(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(false,"User not found"));
  }
}
