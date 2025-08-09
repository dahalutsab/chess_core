package com.utsab.chess_engine.user.service;

import com.utsab.chess_engine.auth.dto.response.UserRegistrationResponse;
import com.utsab.chess_engine.user.dto.request.UserUpdateRequest;

public interface UserService {
    UserRegistrationResponse getLoggedInUser();

    UserRegistrationResponse getUserById(Long userId);

    UserRegistrationResponse updateUser(UserUpdateRequest request);
}
