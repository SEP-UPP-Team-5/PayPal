package com.PayPal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class SubscriptionInfo {
    @Id
    @GeneratedValue
    @Column(unique=true, nullable=false)
    private Long id;
    private String billingPlanId;
    private String billingPlanName;
}