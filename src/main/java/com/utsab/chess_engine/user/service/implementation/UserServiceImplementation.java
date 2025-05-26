package com.utsab.chess_engine.user.service.implementation;

import com.utsab.chess_engine.auth.dto.response.UserRegistrationResponse;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import com.utsab.chess_engine.user.service.UserService;
import com.utsab.chess_engine.util.logged_in_user.LoggedInUserUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {

    private final UserInfoRepository userInfoRepository;
    private final LoggedInUserUtil loggedInUserUtil;

    @Override
    public UserRegistrationResponse getLoggedInUser() {
        return new UserRegistrationResponse(loggedInUserUtil.getLoggedInUser());
    }

    @Override
    public UserRegistrationResponse getUserById(Long userId) {
        return userInfoRepository.findById(userId)
                .map(UserRegistrationResponse::new)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
    }
}