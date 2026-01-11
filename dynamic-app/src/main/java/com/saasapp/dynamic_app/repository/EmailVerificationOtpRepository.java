package com.saasapp.dynamic_app.repository;

import com.saasapp.dynamic_app.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {
    Optional<EmailVerificationOtp> findByEmail(String email);
    Optional<EmailVerificationOtp> findByEmailAndOtp(String email, String otp);
}

