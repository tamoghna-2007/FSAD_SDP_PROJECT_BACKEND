package com.klef.fsad.sdp.service;

import com.klef.fsad.sdp.dto.auth.AuthResponse;
import com.klef.fsad.sdp.dto.auth.ForgotPasswordRequest;
import com.klef.fsad.sdp.dto.auth.LoginRequest;
import com.klef.fsad.sdp.dto.auth.ResendOtpRequest;
import com.klef.fsad.sdp.dto.auth.ResetPasswordRequest;
import com.klef.fsad.sdp.dto.auth.RegisterRequest;
import com.klef.fsad.sdp.dto.auth.VerifyOtpRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    AuthResponse resendOtp(ResendOtpRequest request);

    AuthResponse forgotPassword(ForgotPasswordRequest request);

    AuthResponse resetPassword(ResetPasswordRequest request);
}
