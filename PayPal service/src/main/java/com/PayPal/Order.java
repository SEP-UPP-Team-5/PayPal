package com.PayPal;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data

public class Order {

    private double price;
    private String currency;
    private String method;
    private String intent;
    private String description;
    private String clientId;
    private String clientSecret;

}
