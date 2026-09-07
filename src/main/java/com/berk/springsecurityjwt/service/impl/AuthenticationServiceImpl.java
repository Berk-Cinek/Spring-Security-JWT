package com.berk.springsecurityjwt.service.impl;

import com.berk.springsecurityjwt.domain.dto.LoginUserDto;
import com.berk.springsecurityjwt.domain.dto.RegisterUserDto;
import com.berk.springsecurityjwt.domain.dto.VerifyUserDto;
import com.berk.springsecurityjwt.domain.entities.UserEntity;
import com.berk.springsecurityjwt.repository.UserRepository;
import com.berk.springsecurityjwt.service.AuthenticationService;
import com.berk.springsecurityjwt.service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    public UserEntity signup(RegisterUserDto registerUserDto){
        UserEntity user = new UserEntity(registerUserDto.getUsername(),
                registerUserDto.getEmail(),
                passwordEncoder.encode(registerUserDto.getPassword()));

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpire(LocalDateTime.now().plusMinutes(25));
        user.setEnabled(false);
        sendVerificationEmail(user);
        return userRepository.save(user);
    }

    public UserEntity authenticate(LoginUserDto loginUserDto){
        UserEntity user = userRepository.findByEmail(loginUserDto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()){
            throw new RuntimeException("Account not verified. Please verify your account");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginUserDto.getEmail(),
                        loginUserDto.getPassword()
                )
        );
        return user;
    }

    public void verifyUser(VerifyUserDto verifyUserDto){
        Optional<UserEntity> optionalUserEntity = userRepository.findByEmail(verifyUserDto.getEmail());
        if (optionalUserEntity.isPresent()){
            UserEntity user = optionalUserEntity.get();
            if (user.getVerificationCodeExpire().isBefore(LocalDateTime.now())){
                throw new RuntimeException("Verification code has expired");
            }
            if (user.getVerificationCode().equals(verifyUserDto.getVerificationCode())){
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpire(null);
                userRepository.save(user);
            }else {
                throw new RuntimeException("Invalid verification code");
            }
        }else {
            throw new RuntimeException("User not Found");
        }
    }

    public void resendVerificationCode(String email){
        Optional<UserEntity> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()){
            UserEntity user = optionalUser.get();
            if (user.isEnabled()){
                throw new RuntimeException("Account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpire(LocalDateTime.now().minusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        }else {
            throw new RuntimeException("User not found");
        }
    }

    public void sendVerificationEmail(UserEntity userEntity){
        String subject = "Account Verification";
        String verificationCode = userEntity.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(userEntity.getEmail(), subject,htmlMessage);
        }catch (MessagingException e){
            e.printStackTrace();
        }
    }

    private String generateVerificationCode(){
        Random random = new Random();
        int code = random.nextInt(900000) + 1000000;
        return String.valueOf(code);
    }
}
