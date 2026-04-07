package com.klef.fsad.sdp.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klef.fsad.sdp.dto.auth.AuthResponse;
import com.klef.fsad.sdp.dto.auth.ForgotPasswordRequest;
import com.klef.fsad.sdp.dto.auth.LoginRequest;
import com.klef.fsad.sdp.dto.auth.ResendOtpRequest;
import com.klef.fsad.sdp.dto.auth.ResetPasswordRequest;
import com.klef.fsad.sdp.dto.auth.RegisterRequest;
import com.klef.fsad.sdp.dto.auth.VerifyOtpRequest;
import com.klef.fsad.sdp.model.User;
import com.klef.fsad.sdp.repository.UserRepository;
import com.klef.fsad.sdp.security.JwtService;
import com.klef.fsad.sdp.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final long OTP_VALID_MINUTES = 5;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    @Value("${auth.otp.mail-required:false}")
    private boolean otpMailRequired;

    @Override
    public AuthResponse register(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String otp = generateOtp();

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() == null || request.getRole().isBlank() ? "USER" : request.getRole())
            .otp(otp)
            .otpGeneratedTime(LocalDateTime.now())
            .isVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        sendOtpEmail(savedUser.getEmail(), otp, "Account verification OTP");

        return AuthResponse.builder()
            .message("Registration successful. OTP sent to your email")
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .token(null)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .message("Login successful")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .build();
    }

            @Override
            public AuthResponse verifyOtp(VerifyOtpRequest request) {
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            validateOtp(user, request.getOtp());

            user.setIsVerified(true);
            user.setOtp(null);
            user.setOtpGeneratedTime(null);
            userRepository.save(user);

            return AuthResponse.builder()
                .message("OTP verified successfully")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(null)
                .build();
            }

            @Override
            public AuthResponse resendOtp(ResendOtpRequest request) {
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (Boolean.TRUE.equals(user.getIsVerified())) {
                throw new IllegalArgumentException("User is already verified");
            }

            String otp = generateOtp();
            user.setOtp(otp);
            user.setOtpGeneratedTime(LocalDateTime.now());
            userRepository.save(user);

            sendOtpEmail(user.getEmail(), otp, "Resent account verification OTP");

            return AuthResponse.builder()
                .message("OTP resent successfully")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(null)
                .build();
            }

    @Override
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpGeneratedTime(LocalDateTime.now());
        userRepository.save(user);

        sendOtpEmail(user.getEmail(), otp, "Password reset OTP");

        return AuthResponse.builder()
                .message("Password reset OTP sent to your email")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(null)
                .build();
    }

    @Override
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateOtp(user, request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtp(null);
        user.setOtpGeneratedTime(null);
        userRepository.save(user);

        return AuthResponse.builder()
                .message("Password reset successful")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(null)
                .build();
    }

    private String generateOtp() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(value);
    }

    private void validateOtp(User user, String otp) {
        if (user.getOtp() == null || user.getOtpGeneratedTime() == null) {
            throw new IllegalArgumentException("OTP is not generated. Please request a new OTP");
        }

        if (!user.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        LocalDateTime expiryTime = user.getOtpGeneratedTime().plusMinutes(OTP_VALID_MINUTES);
        if (LocalDateTime.now().isAfter(expiryTime)) {
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP");
        }
    }

    private void sendOtpEmail(String toEmail, String otp, String subject) {
        if (fromEmail == null || fromEmail.isBlank()) {
            if (!otpMailRequired) {
                LOGGER.warn("SMTP not configured. OTP for {} is {} (valid {} minutes)", toEmail, otp, OTP_VALID_MINUTES);
                return;
            }
            throw new IllegalStateException(
                    "OTP email service is not configured. Please set MAIL_USERNAME and MAIL_PASSWORD in backend environment.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText("Your OTP is: " + otp + ". It will expire in 5 minutes.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            if (!otpMailRequired) {
                LOGGER.warn("Unable to send OTP email. Falling back to console OTP for {}: {}", toEmail, otp);
                return;
            }
            throw new IllegalStateException(
                    "Unable to send OTP email. Verify SMTP settings and use a valid mail app password.", ex);
        }
    }
}
