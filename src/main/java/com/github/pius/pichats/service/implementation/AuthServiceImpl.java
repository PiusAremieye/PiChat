package com.github.pius.pichats.service.implementation;

import com.github.pius.pichats.dto.LoginRequestDTO;
import com.github.pius.pichats.dto.AuthResponseDTO;
import com.github.pius.pichats.dto.SignupRequestDTO;
import com.github.pius.pichats.exceptions.CustomException;
import com.github.pius.pichats.model.EmailVerification;
import com.github.pius.pichats.model.Notification;
import com.github.pius.pichats.model.User;
import com.github.pius.pichats.repository.EmailVerificationRepository;
import com.github.pius.pichats.repository.NotificationRepository;
import com.github.pius.pichats.repository.UserRepository;
import com.github.pius.pichats.security.JwtProvider;
import com.github.pius.pichats.service.AuthService;
import com.github.pius.pichats.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final EmailVerificationRepository emailVerificationRepository;
  private final NotificationRepository notificationRepository;
  private final EmailSenderService emailSenderService;
  private final TokenService tokenService;

  @Autowired
  public AuthServiceImpl(JwtProvider jwtProvider, UserRepository userRepository, PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager, EmailVerificationRepository emailVerificationRepository,
      NotificationRepository notificationRepository, EmailSenderService emailSenderService, TokenService tokenService) {
    this.jwtProvider = jwtProvider;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.emailVerificationRepository = emailVerificationRepository;
    this.notificationRepository = notificationRepository;
    this.emailSenderService = emailSenderService;
    this.tokenService = tokenService;
  }

  // @Transactional
  @Override
  public AuthResponseDTO register(SignupRequestDTO user) throws Exception {
    try {
      User newUser = new User();
      if (userRepository.existsByUsername(user.getUsername().toLowerCase())) {
        throw new CustomException(user.getUsername() + " already exists", HttpStatus.BAD_REQUEST);
      }
      if (userRepository.existsByEmail(user.getEmail().toLowerCase())) {
        throw new CustomException(user.getEmail() + " already exists", HttpStatus.BAD_REQUEST);
      }

      String code = tokenService.activationToken();
      // send email to the user asynchronously
      newUser.setEmail(user.getEmail().toLowerCase());
      newUser.setFirstName(user.getFirstName());
      newUser.setLastName(user.getLastName());
      newUser.setPassword(passwordEncoder.encode(user.getPassword()));
      newUser.setUsername(user.getUsername().toLowerCase());
      EmailVerification em = new EmailVerification();
      em.setValidity(LocalDateTime.now().plusHours(15));
      em.setCode(code);
      em.setUser(newUser);
      emailVerificationRepository.save(em);
      this.emailSenderService.sendMail(newUser.getEmail(), "Activate pichat account",
          "Use the code below to activate your pichat account" + "\n code - " + code);
      newUser = userRepository.save(newUser);
      String token = jwtProvider.createToken(newUser.getUsername());

      // notify user that he/she has created an account
      Notification notify = new Notification();
      notify.setActor(newUser.getUsername());
      notify.setMessage(
          "Welcome!!!, and thank you for creating an account on this platform, don't worry you will have a blast once you activate your account");
      notificationRepository.save(notify);
      return new AuthResponseDTO(newUser.getId(), newUser.getFirstName(), newUser.getLastName(), newUser.getEmail(),
          newUser.getUsername(), false, newUser.getCreatedAt(), newUser.getUpdatedAt(), token);
    } catch (MailSendException ex) {
      throw new CustomException(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

  @Override
  public String activate(String code, HttpServletRequest request) {
    User user = jwtProvider.resolveUser(request);
    Optional<EmailVerification> em = emailVerificationRepository.findByCodeAndUser(code, user);
    if (!em.isPresent()) {
      throw new CustomException("Activation code is wrong", HttpStatus.BAD_REQUEST);
    }
    EmailVerification e = em.get();
    if (e.getValidity().isBefore(LocalDateTime.now())) {
      throw new CustomException("Activation code has expired", HttpStatus.BAD_REQUEST);
    }
    user.setActive(true);
    userRepository.save(user);

    // notify user that he/she has activated their account
    Notification notify = new Notification();
    notify.setActor(user.getUsername());
    notify.setMessage("Thank you for activating your account, please feel free an enjoy this platform");
    notificationRepository.save(notify);
    return "Account has been activated";
  }

  @Override
  public AuthResponseDTO login(LoginRequestDTO user) {
    String identifier = user.getIdentifier().toLowerCase();
    String password = user.getPassword();
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));
      Optional<User> authUserByEmail = userRepository.findByEmail(identifier);
      Optional<User> authUserByUsername = userRepository.findByUsername(identifier);
      if (!authUserByEmail.isPresent()) {
        if (authUserByUsername.isPresent()) {
          String token = jwtProvider.createToken(authUserByUsername.get().getUsername());
          return new AuthResponseDTO(authUserByUsername.get().getId(), authUserByUsername.get().getFirstName(),
              authUserByUsername.get().getLastName(), authUserByUsername.get().getEmail(),
              authUserByUsername.get().getUsername(), false, authUserByUsername.get().getCreatedAt(),
              authUserByUsername.get().getUpdatedAt(), token);
        }
        throw new CustomException("Invalid email or username/password supplied...", HttpStatus.UNPROCESSABLE_ENTITY);
      }
      String token = jwtProvider.createToken(authUserByEmail.get().getUsername());
      return new AuthResponseDTO(authUserByEmail.get().getId(), authUserByEmail.get().getFirstName(),
          authUserByEmail.get().getLastName(), authUserByEmail.get().getEmail(), authUserByEmail.get().getUsername(),
          false, authUserByEmail.get().getCreatedAt(), authUserByEmail.get().getUpdatedAt(), token);
    } catch (Exception ex) {
      throw new CustomException("Invalid email or username/password supplied...", HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }
}
