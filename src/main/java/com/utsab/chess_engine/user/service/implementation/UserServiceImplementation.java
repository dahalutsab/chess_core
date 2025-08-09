package com.utsab.chess_engine.user.service.implementation;

import com.utsab.chess_engine.auth.dto.response.UserRegistrationResponse;
import com.utsab.chess_engine.user.dto.request.UserUpdateRequest;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import com.utsab.chess_engine.user.service.UserService;
import com.utsab.chess_engine.util.file.FileHandlerUtil;
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
    private final FileHandlerUtil fileHandlerUtil;

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

    @Override
    public UserRegistrationResponse updateUser(UserUpdateRequest request) {
        UserEntity user = loggedInUserUtil.getLoggedInUser();
        log.info("[UserServiceImplementation:updateUser] Updating user: {}", user.getEmailId());
        user.setFullName(request.getFullName());
        if (request.getProfileImage() != null) {
            user.setProfileImage(fileHandlerUtil.saveFile(request.getProfileImage(), "profile_images").getFileDownloadUri());
        }
        return new UserRegistrationResponse(userInfoRepository.save(user));
    }
}