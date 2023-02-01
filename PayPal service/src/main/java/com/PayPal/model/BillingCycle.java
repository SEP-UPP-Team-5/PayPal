package com.PayPal.model;

import com.PayPal.model.enums.TenureType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BillingCycle {

    private Frequency  frequency;
    private TenureType tenure_type; //TRIAL,
    private Integer sequence;
    private Integer total_cycles;
    private PricingScheme pricing_scheme;
}
