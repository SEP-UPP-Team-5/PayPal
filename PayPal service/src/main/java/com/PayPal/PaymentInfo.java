package com.PayPal;

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
public class PaymentInfo {


    @Id
    @GeneratedValue
    @Column(unique=true, nullable=false)
    private Long id;
    private String paymentId;  //transaction id sa paypala
    private String payerId;  //PayPal account id
    private String amount;
    private String currency; //poslati sa webshopa????????
    private String date; // vreme kad je izvrseno placanje na paypalu
    private String status;
    private String merchantId;
}
