package com.saasapp.dynamic_app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderResponse {
    private String razorpayOrderId;
    private Long amount;
    private String currency;
    private String status;
    private String message;
}

