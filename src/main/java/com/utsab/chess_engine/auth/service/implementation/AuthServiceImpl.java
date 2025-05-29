package com.utsab.chess_engine.auth.service.implementation;

import com.utsab.chess_engine.auth.dto.TokenType;
import com.utsab.chess_engine.auth.dto.request.AuthRequest;
import com.utsab.chess_engine.auth.dto.request.UserRegistrationRequest;
import com.utsab.chess_engine.auth.dto.request.forgot_password.ResetPasswordRequest;
import com.utsab.chess_engine.auth.dto.response.AuthResponse;
import com.utsab.chess_engine.auth.dto.response.ForgotPasswordResponse;
import com.utsab.chess_engine.auth.dto.response.UserRegistrationResponse;
import com.utsab.chess_engine.auth.entity.RefreshTokenEntity;
import com.utsab.chess_engine.auth.messages.AuthExceptionMessages;
import com.utsab.chess_engine.auth.messages.AuthLogMessages;
import com.utsab.chess_engine.auth.messages.AuthResponseMessages;
import com.utsab.chess_engine.auth.repository.RefreshTokenRepo;
import com.utsab.chess_engine.auth.service.AuthService;
import com.utsab.chess_engine.mail.MailService;
import com.utsab.chess_engine.otp.entity.OTP;
import com.utsab.chess_engine.otp.entity.OTPPurpose;
import com.utsab.chess_engine.otp.service.OTPService;
import com.utsab.chess_engine.security.jwt_auth.JwtTokenGenerator;
import com.utsab.chess_engine.user.dto.response.RolesResponse;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import com.utsab.chess_engine.user.role.entity.Roles;
import com.utsab.chess_engine.user.role.entity.UserRole;
import com.utsab.chess_engine.user.role.repository.RolesRepository;
import com.utsab.chess_engine.util.file.FileHandlerUtil;
import com.utsab.chess_engine.util.file.FileType;
import com.utsab.chess_engine.util.validator.EmailValidator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserInfoRepository userInfoRepository;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final RefreshTokenRepo refreshTokenRepo;
    private final AuthenticationManager authenticationManager;
    private final FileHandlerUtil fileHandlerUtil;
    private final RolesRepository rolesRepository;
    private final OTPService otpService;
    private final MailService mailService;

    @Value("${frontend.domain}")
    private String frontEndUrl;
    @Value("${frontend.forgot_password}")
    private String forgotPasswordUrl;
    @Value("${frontend.verify_email}")
    private String verifyEmailUrl;

    @Override
    public AuthResponse getJwtTokensAfterAuthentication(AuthRequest authenticationRequest, HttpServletResponse response) {
        try {
            // Authenticate the user credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.email(),
                            authenticationRequest.password()
                    )
            );

            // Fetch user information after successful authentication
            var userInfoEntity = userInfoRepository.findByEmailId(authenticationRequest.email())
                    .orElseThrow(() -> {
                        log.error(AuthLogMessages.USER_NOT_FOUND, authenticationRequest.email());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "USER NOT FOUND");                    });

            // Generate JWT tokens
            String accessToken = jwtTokenGenerator.generateAccessToken(authentication);
            String refreshToken = jwtTokenGenerator.generateRefreshToken(authentication);

            // Save refresh token and create cookie
            saveUserRefreshToken(userInfoEntity, refreshToken);
            createRefreshTokenCookie(response, refreshToken);

            log.info(AuthLogMessages.ACCESS_TOKEN_GENERATED, userInfoEntity.getEmailId());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .accessTokenExpiry(15 * 60)
                    .userRole(userInfoEntity.getRoles().stream().map(RolesResponse::new).toList())
                    .tokenType(TokenType.Bearer)
                    .build();

        } catch (AuthenticationException e) {
            log.error(AuthLogMessages.INVALID_CREDENTIALS, authenticationRequest.email());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AuthExceptionMessages.INVALID_CREDENTIALS);
        } catch (Exception e) {
            log.error(AuthLogMessages.EXCEPTION_AUTHENTICATING, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, AuthExceptionMessages.TRY_AGAIN);
        }
    }


    private void saveUserRefreshToken(UserEntity userEntity, String refreshToken) {
        var refreshTokenEntity = RefreshTokenEntity.builder()
                .user(userEntity)
                .refreshToken(refreshToken)
                .revoked(false)
                .build();
        refreshTokenRepo.save(refreshTokenEntity);
    }

    private void createRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(15 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);
    }

    @Override
    public AuthResponse getAccessTokenUsingRefreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AuthExceptionMessages.REFRESH_TOKEN_MISSING);
        }

        // Validate refresh token from database
        var refreshTokenEntity = refreshTokenRepo.findByRefreshToken(refreshToken)
                .filter(tokens -> !tokens.isRevoked())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, AuthExceptionMessages.REFRESH_TOKEN_REVOKED));

        UserEntity userEntity = refreshTokenEntity.getUser();

        // Generate new access token
        Authentication authentication = createAuthenticationObject(userEntity);
        String accessToken = jwtTokenGenerator.generateAccessToken(authentication);

        log.info(AuthLogMessages.ACCESS_TOKEN_GENERATED, userEntity.getEmailId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .accessTokenExpiry(5 * 60)
                .tokenType(TokenType.Bearer)
                .build();
    }

    private static Authentication createAuthenticationObject(UserEntity userEntity) {
        // Extract user details from UserDetailsEntity
        String username = userEntity.getEmailId();
        String password = userEntity.getPassword();
        List<Roles> roles = userEntity.getRoles();

        // Extract authorities from roles
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) role::getName)
                .toList();

        return new UsernamePasswordAuthenticationToken(username, password, authorities);
    }


    @Override
    public ForgotPasswordResponse forgotPassword(String email) {
        UserEntity userEntity = userInfoRepository.findByEmailId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AuthExceptionMessages.USER_NOT_FOUND + email));
        OTP otp = otpService.saveOTP(userEntity, OTPPurpose.FORGOT_PASSWORD);
        String forgotPasswordLink = frontEndUrl + forgotPasswordUrl + "?token=" + otp.getOtpValue();
        mailService.sendForgotPasswordMail(userEntity, forgotPasswordLink, otp.getExpiryTime());
        return new ForgotPasswordResponse(AuthResponseMessages.PASSWORD_RESET_LINK_SENT, userEntity.getEmailId(), otp.getExpiryTime());
    }

    @Override
    public String resetPassword(ResetPasswordRequest resetPasswordRequest) {
        OTP otp = otpService.getOTP(resetPasswordRequest.getOtp(), OTPPurpose.FORGOT_PASSWORD);
        UserEntity userEntity = otp.getUser();
        userEntity.setPassword(new BCryptPasswordEncoder().encode(resetPasswordRequest.getNewPassword()));
        userInfoRepository.save(userEntity);
        return AuthResponseMessages.PASSWORD_RESET_SUCCESS;
    }

    @Override
    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest registration) {
        if (!EmailValidator.isValid(registration.userEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AuthExceptionMessages.INVALID_EMAIL);
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setFullName(registration.fullName());
        userEntity.setEmailId(registration.userEmail());
        userEntity.setPassword(new BCryptPasswordEncoder().encode(registration.password()));
        Roles role = rolesRepository.findByName(UserRole.USER.name()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        userEntity.getRoles().add(role);
        userEntity.setVerified(false);
        OTP otp = otpService.saveOTP(userEntity, OTPPurpose.REGISTER);
        String verifyUrl = this.frontEndUrl + verifyEmailUrl + "?email=" + registration.userEmail();
        URI frontEndUri = URI.create(verifyUrl);
        mailService.sendRegistrationMail(userEntity, otp, frontEndUri);
        if (registration.profileImage() == null || registration.profileImage().isEmpty()) {
            return new UserRegistrationResponse(userInfoRepository.save(userEntity), otp.getExpiryTime());
        }

        FileType fileType = fileHandlerUtil.determineFileType(Objects.requireNonNull(registration.profileImage().getOriginalFilename()));
        if (fileType == FileType.IMAGE) {
            String fileName = fileHandlerUtil.saveFile(registration.profileImage(), registration.userEmail()).getFileDownloadUri();
            userEntity.setProfileImage(fileName);
            userInfoRepository.save(userEntity);
            return new UserRegistrationResponse(userInfoRepository.save(userEntity), otp.getExpiryTime());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AuthExceptionMessages.INVALID_FILE_TYPE);
    }

    @Override
    public String verifyEmail(String email, String token) {
        if (!EmailValidator.isValid(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AuthExceptionMessages.INVALID_EMAIL);
        }

        UserEntity user = userInfoRepository.findByEmailId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AuthExceptionMessages.USER_NOT_FOUND + email));

        if (user.isVerified()) {
            return AuthResponseMessages.EMAIL_ALREADY_VERIFIED;
        }

        OTP otp = otpService.getOTP(token, OTPPurpose.REGISTER);
        if (!otp.getOtpValue().equals(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AuthExceptionMessages.INVALID_OTP);
        }
        otpService.validateOTP(user,token, OTPPurpose.REGISTER);

        user.setVerified(true);
        userInfoRepository.save(user);
        return AuthResponseMessages.EMAIL_VERIFIED;
    }

    @Override
    public UserRegistrationResponse resendVerificationEmail(String email) {
        if (!EmailValidator.isValid(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, AuthExceptionMessages.INVALID_EMAIL);
        }

        UserEntity user = userInfoRepository.findByEmailId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, AuthExceptionMessages.USER_NOT_FOUND + email));

        if (user.isVerified()) {
            return new UserRegistrationResponse(user, null);
        }

        OTP otp = otpService.saveOTP(user, OTPPurpose.REGISTER);
        String verifyUrl = this.frontEndUrl + verifyEmailUrl + "?email=" + email;
        URI frontEndUri = URI.create(verifyUrl);
        mailService.sendRegistrationMail(user, otp, frontEndUri);

        return new UserRegistrationResponse(userInfoRepository.save(user), otp.getExpiryTime());
    }
}