package com.utsab.chess_engine.auth.service;

import com.utsab.chess_engine.auth.dto.request.AuthRequest;
import com.utsab.chess_engine.auth.dto.request.UserRegistrationRequest;
import com.utsab.chess_engine.auth.dto.request.forgot_password.ResetPasswordRequest;
import com.utsab.chess_engine.auth.dto.response.AuthResponse;
import com.utsab.chess_engine.auth.dto.response.ForgotPasswordResponse;
import com.utsab.chess_engine.auth.dto.response.UserRegistrationResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;

public interface AuthService {


    AuthResponse getJwtTokensAfterAuthentication(AuthRequest authenticationRequest, HttpServletResponse response);

    AuthResponse getAccessTokenUsingRefreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken);

    ForgotPasswordResponse forgotPassword(String email);

    String resetPassword(ResetPasswordRequest resetPasswordRequest);

    UserRegistrationResponse registerUser(UserRegistrationRequest registration);

    String verifyEmail(String email, String token);

    UserRegistrationResponse resendVerificationEmail(String email);
}