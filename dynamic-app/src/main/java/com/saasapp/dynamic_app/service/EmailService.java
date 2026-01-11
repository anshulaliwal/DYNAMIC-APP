package com.saasapp.dynamic_app.service;

import com.saasapp.dynamic_app.dto.PaymentEmailRequest;

public interface EmailService {
    void sendOtpEmail(String email, String otp);
    void sendWelcomeEmail(String email, String fullName);
    void sendPaymentReceiptEmail(PaymentEmailRequest request);
    void sendPaymentReceiptEmailAsync(PaymentEmailRequest request);
}

