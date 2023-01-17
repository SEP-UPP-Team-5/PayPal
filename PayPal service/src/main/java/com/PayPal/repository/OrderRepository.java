package com.PayPal.repository;

import com.PayPal.model.MyOrder;
import com.PayPal.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<MyOrder, Long> {
    MyOrder findByWebShopOrderId(String id);
    MyOrder findByPayPalOrderId(String id);
}
