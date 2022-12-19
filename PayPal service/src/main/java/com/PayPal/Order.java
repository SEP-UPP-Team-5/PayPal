package com.PayPal;

import lombok.Data;

import java.net.URI;

@Data
public class Order {

    private final String orderId;
    private final URI approvalLink;
}
