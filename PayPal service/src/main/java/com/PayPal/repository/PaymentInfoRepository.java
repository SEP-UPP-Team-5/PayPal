package com.PayPal.repository;

import com.PayPal.model.PaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInfoRepository extends JpaRepository<PaymentInfo, Long> {
    PaymentInfo findByPaymentId(String id);
}
