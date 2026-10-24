package com.utsab.chess_engine.mail;

import com.utsab.chess_engine.otp.entity.OTP;
import com.utsab.chess_engine.user.entity.UserEntity;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    @Override
    public void sendForgotPasswordMail(UserEntity userEntity, String forgotPasswordUrl, LocalDateTime expiry) {
        String email = userEntity.getEmailId();
        String name = userEntity.getFullName();

        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("resetLink", forgotPasswordUrl);
        context.setVariable("expiryTime", expiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.setVariable("email", email);

        String content = templateEngine.process("forgot-password-link.html", context);
        sendEmail(email, "Reset your password", content);
    }

    @Override
    public void sendRegistrationMail(UserEntity userEntity, OTP otp, URI frontEndUri) {
        String email = userEntity.getEmailId();
        String name = userEntity.getFullName();

        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otp", otp.getOtpValue());
        context.setVariable("expiryTime", otp.getExpiryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        context.setVariable("email", email);
        context.setVariable("frontEndUri", frontEndUri.toString());

        String content = templateEngine.process("registration-otp.html", context);
        sendEmail(email, "Verify your email address", content);
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true = isHtml

            // 1. Embed the image inline (from classpath or file)
            File imageFile = new File("src/main/resources/static/images/uchess.png"); // Adjust path if needed
            helper.addInline("uchessLogo", imageFile);

            // Optional: Also attach the image (or any file)
//            helper.addAttachment("uchess-logo.png", imageFile);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            log.error("Error sending mail to {}: {}", to, e.getMessage());
            throw new RuntimeException("Error sending mail", e);
        }
    }
}
