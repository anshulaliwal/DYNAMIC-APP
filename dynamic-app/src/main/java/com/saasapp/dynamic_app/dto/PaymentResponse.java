package com.saasapp.dynamic_app.dto;

import com.saasapp.dynamic_app.entity.PaymentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private String userId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private String paymentMethod;
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentResponse fromEntity(PaymentEntity payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .description(payment.getDescription())
                .status(payment.getStatus().toString())
                .paymentMethod(payment.getPaymentMethod())
                .customerEmail(payment.getCustomerEmail())
                .customerPhone(payment.getCustomerPhone())
                .customerName(payment.getCustomerName())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}

