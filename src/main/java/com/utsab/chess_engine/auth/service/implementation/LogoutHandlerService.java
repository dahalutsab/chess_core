package com.utsab.chess_engine.auth.service.implementation;

import com.utsab.chess_engine.auth.repository.RefreshTokenRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogoutHandlerService implements LogoutHandler {

    private final RefreshTokenRepo refreshTokenRepo;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    /** Revoke the refresh token from the cookie and clear the cookie, so the session can't be renewed */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> AuthServiceImpl.REFRESH_COOKIE.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .forEach(value -> refreshTokenRepo.findByRefreshToken(value).ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepo.save(token);
                    }));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, AuthServiceImpl.refreshCookie("", Duration.ZERO, cookieSecure).toString());
    }
}
