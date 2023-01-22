package com.PayPal.dto;

import com.paypal.api.payments.Links;
import com.paypal.orders.LinkDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSubscriptionResponseDTO {

    private String status;
    private String id;
    private String create_time;
    private ArrayList<Links> links;
}
