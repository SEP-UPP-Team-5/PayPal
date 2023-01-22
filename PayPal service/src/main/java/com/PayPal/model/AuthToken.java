package com.PayPal.model;

import lombok.Data;

@Data
public class AuthToken {
    private String scope;
    private String access_token;
    private  String token_type;
    private String app_id;
}
