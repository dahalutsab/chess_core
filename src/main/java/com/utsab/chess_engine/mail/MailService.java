package com.utsab.chess_engine.mail;

import com.utsab.chess_engine.user.entity.UserEntity;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;

public interface MailService {

    @Async
    void sendForgotPasswordMail(UserEntity userEntity, String forgotPasswordUrl, LocalDateTime expiry);
}
