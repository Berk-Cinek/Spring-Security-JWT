package com.berk.springsecurityjwt.service;

import com.berk.springsecurityjwt.domain.dto.LoginUserDto;
import com.berk.springsecurityjwt.domain.dto.RegisterUserDto;
import com.berk.springsecurityjwt.domain.dto.VerifyUserDto;
import com.berk.springsecurityjwt.domain.entities.UserEntity;

public interface AuthenticationService {

    UserEntity signup(RegisterUserDto registerUserDto);

    UserEntity authenticate(LoginUserDto loginUserDto);

    void verifyUser(VerifyUserDto verifyUserDto);

    void resendVerificationCode(String email);

    void sendVerificationEmail(UserEntity userEntity);
}
