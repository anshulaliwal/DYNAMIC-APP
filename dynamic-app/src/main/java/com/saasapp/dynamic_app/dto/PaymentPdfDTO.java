package com.saasapp.dynamic_app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPdfDTO {
    private Long paymentId;
    private String transactionId;
    private String userName;
    private String userEmail;
    private Double amount;
    private String currency;
    private String paymentStatus;
    private String paymentDate;
    private String description;
}

