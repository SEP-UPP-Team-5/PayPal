package com.PayPal.service;

import com.PayPal.model.BillingPlan;
import com.PayPal.model.SubscriptionInfo;
import com.PayPal.repository.SubscriptionInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SubscriptionService {

    @Autowired
    private SubscriptionInfoRepository subscriptionInfoRepository;

    public void save(SubscriptionInfo subscriptionInfo){ subscriptionInfoRepository.save(subscriptionInfo); }

}
