package com.saasapp.dynamic_app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Payment Email Request
 * Contains all payment details needed to send email receipt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEmailRequest {
    private String transactionId;
    private String email;
    private String userName;
    private Double amount;
    private String currency;
    private String date;
    private String description;
    private Long paymentId;
    private String status;
    private String qrCodeUrl;
}

