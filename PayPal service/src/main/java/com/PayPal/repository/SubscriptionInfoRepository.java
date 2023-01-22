package com.PayPal.repository;

import com.PayPal.model.SubscriptionInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionInfoRepository extends JpaRepository<SubscriptionInfo, Long> {
}
