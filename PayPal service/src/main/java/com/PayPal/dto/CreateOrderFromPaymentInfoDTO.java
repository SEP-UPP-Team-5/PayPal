package com.PayPal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderFromPaymentInfoDTO {

    private Double totalAmount;
    private String transactionId; //id transakcije sa psp
    private String merchantId; // merchant iz payment metoda

}
