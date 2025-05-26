package com.utsab.chess_engine.otp.service;


import com.utsab.chess_engine.otp.entity.OTP;
import com.utsab.chess_engine.otp.entity.OTPPurpose;
import com.utsab.chess_engine.user.entity.UserEntity;

public interface OTPService {
    OTP saveOTP(UserEntity user, OTPPurpose purpose);

    void validateOTP(UserEntity user, String otp);

    OTP getOTP(String otp, OTPPurpose purpose);
}
