package com.PayPal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentResponseDTO {

    private String returnUrl;
    private String payPalOrderId;
}
