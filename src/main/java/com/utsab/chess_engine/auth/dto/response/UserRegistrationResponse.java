package com.utsab.chess_engine.auth.dto.response;


import com.utsab.chess_engine.user.dto.response.RolesResponse;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.util.file.FileUrlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRegistrationResponse {
    Long id;
    String fullName;
    String userEmail;
    URI profilePicture;
    List<RolesResponse> userRole;
    LocalDateTime otpExpiryTime;

    public UserRegistrationResponse(UserEntity user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.userEmail = user.getEmailId();
        this.profilePicture = FileUrlUtil.getFileUri(user.getProfileImage());
        this.userRole = user.getRoles().stream()
                .map(RolesResponse::new)
                .toList();
    }

    public UserRegistrationResponse(UserEntity user, LocalDateTime otpExpiryTime) {
        this(user);
        this.otpExpiryTime = otpExpiryTime;
    }
}
