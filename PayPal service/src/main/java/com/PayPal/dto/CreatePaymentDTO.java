package com.PayPal.dto;

import lombok.Data;

@Data
public class CreatePaymentDTO {

    Double price;
    String applicationName;
    String purchaseId;
    String merchantId;

}
