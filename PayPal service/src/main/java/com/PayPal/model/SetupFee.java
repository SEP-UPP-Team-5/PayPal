package com.PayPal.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetupFee {
    private String value;
    private String currency_code;
}