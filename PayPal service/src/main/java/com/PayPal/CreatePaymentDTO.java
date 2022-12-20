package com.PayPal;

import lombok.Data;

@Data
public class CreatePaymentDTO {

    Double price;
    String applicationName;
    String purchaseId;
    String merchantId;

}
