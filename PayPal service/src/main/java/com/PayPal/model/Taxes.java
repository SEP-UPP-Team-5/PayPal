package com.PayPal.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Taxes {
    private String percentage;
    private Boolean inclusive;
}