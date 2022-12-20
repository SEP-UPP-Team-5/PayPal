package com.PayPal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;

import java.net.URI;

@Data
public class Order {

    private final String orderId;
    private final URI approvalLink;
    private final String applicationName;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
