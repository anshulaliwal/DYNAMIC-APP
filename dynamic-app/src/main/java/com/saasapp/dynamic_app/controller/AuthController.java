package com.saasapp.dynamic_app.controller;

import com.saasapp.dynamic_app.dto.*;
import com.saasapp.dynamic_app.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"https://nyayapathlegal.in", "https://client-ca-saas-app.vercel.app", "https://super-saas-app.vercel.app", "http://localhost:3000", "http://localhost:3001"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true",
        maxAge = 3600)

public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            logger.info("Send OTP request received for: {}", request.getEmail());
            OtpResponse response = authService.sendOtp(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Send OTP failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), "SEND_OTP_FAILED"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        try {
            logger.info("Signup request received for: {}", request.getEmail());
            SignupResponse signupResponse = authService.signup(request);

            // Create and add authToken cookie
            Cookie cookie = new Cookie("authToken", signupResponse.getToken());
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(604800); // 7 days
            response.addCookie(cookie);

            // Also add Set-Cookie header with SameSite attribute
            response.addHeader("Set-Cookie", String.format(
                "authToken=%s; Path=/; Max-Age=604800; HttpOnly; Secure; SameSite=Lax",
                signupResponse.getToken()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(signupResponse);
        } catch (RuntimeException e) {
            logger.error("Signup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), "SIGNUP_FAILED"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            logger.info("Login request received for: {}", request.getEmail());
            AuthResponse authResponse = authService.login(request);

            // Create and add authToken cookie
            Cookie cookie = new Cookie("authToken", authResponse.getToken());
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(604800); // 7 days
            response.addCookie(cookie);

            // Also add Set-Cookie header with SameSite attribute
            response.addHeader("Set-Cookie", String.format(
                "authToken=%s; Path=/; Max-Age=604800; HttpOnly; Secure; SameSite=Lax",
                authResponse.getToken()
            ));

            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            logger.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password", "INVALID_CREDENTIALS"));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            logger.info("Token refresh request received");
            AuthResponse authResponse = authService.refreshToken(request);

            // Create and add authToken cookie
            Cookie cookie = new Cookie("authToken", authResponse.getToken());
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(604800); // 7 days
            response.addCookie(cookie);

            // Also add Set-Cookie header with SameSite attribute
            response.addHeader("Set-Cookie", String.format(
                "authToken=%s; Path=/; Max-Age=604800; HttpOnly; Secure; SameSite=Lax",
                authResponse.getToken()
            ));

            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), "TOKEN_REFRESH_FAILED"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new HealthResponse("Auth service is running"));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorResponse {
        private String error;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class HealthResponse {
        private String message;
    }
}
