package com.berk.springsecurityjwt.service;

import com.berk.springsecurityjwt.domain.entities.UserEntity;

import java.util.List;

public interface UserService {
    List<UserEntity> allUsers();
}
