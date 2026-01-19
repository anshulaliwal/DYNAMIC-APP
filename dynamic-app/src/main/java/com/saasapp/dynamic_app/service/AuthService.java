package com.saasapp.dynamic_app.service;

import com.saasapp.dynamic_app.dto.*;
import com.saasapp.dynamic_app.entity.User;
import com.saasapp.dynamic_app.entity.EmailVerificationOtp;
import com.saasapp.dynamic_app.repository.UserRepository;
import com.saasapp.dynamic_app.repository.EmailVerificationOtpRepository;
import com.saasapp.dynamic_app.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationOtpRepository otpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private EmailService emailService;

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public OtpResponse sendOtp(SendOtpRequest request) {
        try {
            String email = request.getEmail();
            logger.info("Sending OTP to email: {}", email);

            // Check if email already registered
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already registered");
            }

            // Generate OTP
            String otp = generateOtp();

            // Save or update OTP in database
            EmailVerificationOtp existingOtp = otpRepository.findByEmail(email).orElse(null);
            if (existingOtp != null) {
                existingOtp.setOtp(otp);
                existingOtp.setIsUsed(false);
                existingOtp.setCreatedAt(LocalDateTime.now());
                existingOtp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
                otpRepository.save(existingOtp);
            } else {
                EmailVerificationOtp newOtp = EmailVerificationOtp.builder()
                        .email(email)
                        .otp(otp)
                        .isUsed(false)
                        .build();
                otpRepository.save(newOtp);
            }

            // Send OTP via email
            emailService.sendOtpEmail(email, otp);

            logger.info("OTP sent successfully to: {}", email);

            return OtpResponse.builder()
                    .success(true)
                    .message("OTP sent successfully to your email")
                    .build();

        } catch (Exception e) {
            logger.error("Error sending OTP: {}", e.getMessage());
            throw new RuntimeException("Failed to send OTP: " + e.getMessage(), e);
        }
    }

    public SignupResponse signup(SignupRequest request) {
        try {
            logger.debug("Processing signup for email: {}", request.getEmail());

            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                logger.warn("Email already registered: {}", request.getEmail());
                throw new RuntimeException("Email already registered");
            }

            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                logger.warn("Username already taken: {}", request.getUsername());
                throw new RuntimeException("Username already taken");
            }

            // Validate passwords match
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("Passwords do not match");
            }

            // Verify OTP provided by user
            EmailVerificationOtp otpRecord = otpRepository.findByEmailAndOtp(request.getEmail(), request.getOtp())
                    .orElseThrow(() -> new RuntimeException("Invalid OTP"));

            if (otpRecord.isExpired()) {
                throw new RuntimeException("OTP has expired");
            }

            if (otpRecord.getIsUsed()) {
                throw new RuntimeException("OTP already used");
            }


            // Mark OTP as used
            otpRecord.setIsUsed(true);
            otpRepository.save(otpRecord);

            // Determine role
            User.UserRole role = User.UserRole.USER;
            if (request.getRole() != null && "ADMIN".equalsIgnoreCase(request.getRole())) {
                role = User.UserRole.ADMIN;
            }

            // Create new user
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(role)
                    .isActive(true)
                    .isEmailVerified(true)
                    .build();

            User savedUser = userRepository.save(user);
            logger.info("User registered successfully with id: {}", savedUser.getId());

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(savedUser.getEmail());
            String refreshToken = tokenProvider.generateRefreshToken(savedUser.getEmail());

            // Send welcome email
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());

            return SignupResponse.builder()
                    .id(savedUser.getId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .fullName(savedUser.getFullName())
                    .role(savedUser.getRole().name())
                    .createdAt(savedUser.getCreatedAt().toString())
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(86400L)
                    .message("User registered successfully")
                    .build();

        } catch (Exception e) {
            logger.error("Error during signup: {}", e.getMessage());
            throw new RuntimeException("Signup failed: " + e.getMessage(), e);
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            logger.debug("Processing login for email: {}", request.getEmail());

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // Get the authenticated user
            User user = (User) authentication.getPrincipal();

            // Generate tokens using email (not authentication object)
            String accessToken = tokenProvider.generateAccessToken(user.getEmail());
            String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            logger.info("User logged in successfully: {}", request.getEmail());

            return AuthResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .user(AuthResponse.UserInfo.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .role(user.getRole().name())
                            .build())
                    .expiresIn(86400L)
                    .message("Login successful")
                    .build();

        } catch (Exception e) {
            logger.error("Error during login: {}", e.getMessage());
            throw new RuntimeException("Invalid email or password", e);
        }
    }

    public AuthResponse refreshToken(HttpServletRequest request) {
        try {
            logger.debug("Processing token refresh from cookie");

            // Extract token from cookie first
            String token = null;
            Cookie[] cookies = request.getCookies();

            if (cookies != null && cookies.length > 0) {
                for (Cookie cookie : cookies) {
                    logger.debug("Found cookie: {}", cookie.getName());
                    if ("authToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        logger.debug("Found authToken cookie");
                        break;
                    }
                }
            } else {
                logger.debug("No cookies found in request");
            }

            // Fallback: Check Authorization header if no cookie found
            if (token == null || token.isEmpty()) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7); // Remove "Bearer " prefix
                    logger.debug("Token found in Authorization header");
                }
            }

            if (token == null || token.isEmpty()) {
                logger.error("No authentication token found in cookie or Authorization header");
                throw new RuntimeException("No authentication token found in cookie or Authorization header");
            }

            if (!tokenProvider.validateToken(token)) {
                throw new RuntimeException("Invalid or expired token");
            }

            String email = tokenProvider.getUsernameFromToken(token);
            logger.debug("Extracted email from token: {}", email);

            if (email == null || email.trim().isEmpty()) {
                logger.error("Email extracted from token is null or empty");
                throw new RuntimeException("Invalid token: email is empty");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found for email: {}", email);
                        return new RuntimeException("User not found for email: " + email);
                    });

            logger.debug("User found with ID: {}", user.getId());
            String newAccessToken = tokenProvider.generateAccessToken(email);

            logger.info("Token refreshed for user: {}", email);

            return AuthResponse.builder()
                    .token(newAccessToken)
                    .user(AuthResponse.UserInfo.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .role(user.getRole().name())
                            .build())
                    .expiresIn(86400L)
                    .message("Token refreshed successfully")
                    .build();

        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed: " + e.getMessage(), e);
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

