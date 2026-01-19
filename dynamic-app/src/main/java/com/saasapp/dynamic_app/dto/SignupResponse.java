package com.saasapp.dynamic_app.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {

    private Long id;

    private String username;

    private String email;

    private String fullName;

    private String role;

    private String createdAt;

    private String message;

    private String token;

    private String refreshToken;

    private Long expiresIn;
}

