package com.utsab.chess_engine.security.jwt_auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.utsab.chess_engine.auth.repository.RefreshTokenRepo;
import com.utsab.chess_engine.config.RSAKeyRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class JwtRefreshTokenFilter extends OncePerRequestFilter {

    private final RSAKeyRecord rsaKeyRecord;
    private final JwtTokenUtils jwtTokenUtils;
    private final RefreshTokenRepo refreshTokenRepo;

    private Optional<String> extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        return Optional.empty();
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            log.info("[JwtRefreshTokenFilter:doFilterInternal] :: Started ");
            log.info("[JwtRefreshTokenFilter:doFilterInternal] Filtering request: {}", request.getRequestURI());

            Optional<String> tokenOpt = extractTokenFromCookies(request);
            if (tokenOpt.isEmpty()) {
                log.info("[JwtRefreshTokenFilter] No refresh token found in cookies.");
                filterChain.doFilter(request, response);
                return;
            }

            final String token = tokenOpt.get();

            // Decrypt the JWE
            JWEObject jweObject = JWEObject.parse(token);
            RSADecrypter decrypter = new RSADecrypter(rsaKeyRecord.rsaPrivateKey());
            jweObject.decrypt(decrypter);

            // Get claims
            JWTClaimsSet claims = JWTClaimsSet.parse(jweObject.getPayload().toJSONObject());

            final String userName = jwtTokenUtils.getUserName(claims);
            if (!userName.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Check refresh token validity in database
                boolean isRefreshTokenValid = refreshTokenRepo.findByRefreshToken(token)
                        .map(refreshTokenEntity -> !refreshTokenEntity.isRevoked())
                        .orElse(false);

                UserDetails userDetails = jwtTokenUtils.userDetails(userName);
                if (jwtTokenUtils.isTokenValid(claims, userDetails) && isRefreshTokenValid) {
                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                    UsernamePasswordAuthenticationToken createdToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    createdToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    securityContext.setAuthentication(createdToken);
                    SecurityContextHolder.setContext(securityContext);
                }
            }

            log.info("[JwtRefreshTokenFilter:doFilterInternal] Completed");
            filterChain.doFilter(request, response);
        } catch (ParseException | JOSEException e) {
            log.error("[JwtRefreshTokenFilter:doFilterInternal] Exception: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Invalid refresh token: " + e.getMessage());
        }
    }
}