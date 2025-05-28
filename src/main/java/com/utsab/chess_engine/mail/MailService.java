package com.utsab.chess_engine.mail;

import com.utsab.chess_engine.otp.entity.OTP;
import com.utsab.chess_engine.user.entity.UserEntity;
import org.springframework.scheduling.annotation.Async;

import java.net.URI;
import java.time.LocalDateTime;

public interface MailService {

    @Async
    void sendForgotPasswordMail(UserEntity userEntity, String forgotPasswordUrl, LocalDateTime expiry);

    @Async
    void sendRegistrationMail(UserEntity userEntity, OTP otp, URI frontEndUri);
}
