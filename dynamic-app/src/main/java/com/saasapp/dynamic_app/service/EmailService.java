package com.saasapp.dynamic_app.service;

import com.saasapp.dynamic_app.dto.PaymentEmailRequest;

/**
 * Email Service Interface
 * Defines contract for email sending operations with both synchronous and asynchronous support
 */
public interface EmailService {

    /**
     * Send payment receipt email synchronously (blocking)
     *
     * @param request PaymentEmailRequest containing email and payment details
     * @throws EmailSendingException if email sending fails
     */
    void sendPaymentReceiptEmail(PaymentEmailRequest request);

    /**
     * Send payment receipt email asynchronously (non-blocking)
     * Useful for not blocking the main transaction process
     *
     * @param request PaymentEmailRequest containing email and payment details
     */
    void sendPaymentReceiptEmailAsync(PaymentEmailRequest request);
}

