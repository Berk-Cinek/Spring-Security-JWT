package com.berk.springsecurityjwt.service.impl;

import com.berk.springsecurityjwt.domain.entities.UserEntity;
import com.berk.springsecurityjwt.repository.UserRepository;
import com.berk.springsecurityjwt.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    public final UserRepository userRepository;


    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserEntity> allUsers(){
        List<UserEntity> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }
}
