package com.PayPal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptureOrderResponseDTO {
    private String webShopOrderId;
    private String payerId;
}
