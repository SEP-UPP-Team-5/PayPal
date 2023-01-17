package com.PayPal.model;


import jakarta.persistence.*;
import lombok.*;

import java.net.URI;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class MyOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private String payPalOrderId;
    private URI approvalLink;
    private String webShopOrderId;

}
