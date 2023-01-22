package com.PayPal.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlan {
    private String product_id;
    private String name;
    private String description;
    private String status; //CREATED, INACTIVE, ACTIVE
    private ArrayList<BillingCycle> billing_cycles; // 1 regular cycle, 2 most , required
    private PaymentPreferences payment_preferences; // required
    private Taxes taxes;
}
