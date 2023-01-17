package com.PayPal.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.*;
import lombok.*;

@Data
public class Order {

    @Id
    @GeneratedValue
    @Column(unique=true, nullable=false)
    private Long id;  //order id sa paypalla
    private String approvalLink;
    private String webShopOrderId;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
