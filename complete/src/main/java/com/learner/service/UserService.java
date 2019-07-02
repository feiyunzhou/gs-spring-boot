package com.learner.service;

import com.learner.UserRepository;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@Log4j2
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public boolean isUserPresent(String username) {
        return userRepository.existsByUserName(username);
    }
}
