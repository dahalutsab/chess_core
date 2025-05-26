package com.utsab.chess_engine.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public record UserRegistrationRequest(
        @NotEmpty(message = "Full name must not be empty")
        String fullName,
        @NotEmpty(message = "User recipientEmail must not be empty") // Neither null nor 0 size
        @Email(message = "Invalid recipientEmail format")
        String userEmail,
        @NotEmpty(message = "Password must not be empty")
        String password,
        MultipartFile profileImage
) { }