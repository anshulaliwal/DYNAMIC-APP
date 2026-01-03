package com.saasapp.dynamic_app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {
    private String userId;
    private Long amount; // Amount in paise (smallest currency unit)
    private String currency;
    private String description;
    private String receipt;
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private String notes;
}

