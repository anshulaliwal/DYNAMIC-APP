package com.saasapp.dynamic_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQRCodeDTO {
    private Long paymentId;
    private String transactionId;
    private Double amount;
    private String currency;
}

