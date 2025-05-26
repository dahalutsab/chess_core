package com.utsab.chess_engine.user.service;

import com.utsab.chess_engine.auth.dto.response.UserRegistrationResponse;

public interface UserService {
    UserRegistrationResponse getLoggedInUser();

    UserRegistrationResponse getUserById(Long userId);
}
