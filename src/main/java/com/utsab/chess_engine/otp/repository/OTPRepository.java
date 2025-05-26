package com.utsab.chess_engine.otp.repository;

import com.utsab.chess_engine.otp.entity.OTP;
import com.utsab.chess_engine.otp.entity.OTPPurpose;
import com.utsab.chess_engine.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OTPRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByUserAndOtpValue(UserEntity user, String otp);

    Optional<OTP> findByOtpValueAndPurpose(String otp, OTPPurpose purpose);
}
