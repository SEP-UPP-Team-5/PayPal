package com.PayPal.controller;

import com.PayPal.dto.BillingPlanDTO;
import com.PayPal.dto.SubscriptionDTO;
import com.PayPal.model.*;
import com.PayPal.model.enums.IntervalUnit;
import com.PayPal.model.enums.SetupFeeFailureAction;
import com.PayPal.model.enums.TenureType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.base.codec.binary.Base64;
import com.paypal.orders.LinkDescription;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/subscriptions")
@Slf4j
public class SubscriptionController {

    private final String APPROVAL_LINK = "approve";

    @Value("${paypal.clientId}") String clientId;
    @Value("${paypal.clientSecret}") String clientSecret;

    @PostMapping("/product")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) throws JSONException {
        String token = accessToken();

        String paypalUrl = "https://api-m.sandbox.paypal.com/v1/catalogs/products";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        JSONObject obj = new JSONObject();
        obj.put("name", product.getName());
        obj.put("description", product.getDescription());
        obj.put("type", product.getType());  //PHYSICAL, DIGITAL, SERVICE

        HttpEntity<String> request = new HttpEntity<>(obj.toString(), headers);
        Product productResponse = restTemplate.postForObject(paypalUrl, request, Product.class);
        System.out.println(productResponse);

        return ResponseEntity.ok(productResponse);
    }

    @PostMapping("/billingPlan")
    public ResponseEntity<?> createSubscriptionPlan(@RequestBody BillingPlanDTO billingPlanDTO) throws JSONException {
        String token = accessToken();

        String paypalUrl = "https://api-m.sandbox.paypal.com/v1/billing/plans";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        BillingPlan billingPlan = new BillingPlan();
        billingPlan.setProduct_id(billingPlanDTO.getProduct_id());
        billingPlan.setName("Book renting service plan");
        billingPlan.setDescription("Book renting service plan, per month charging");
        billingPlan.setStatus("ACTIVE");
        // Create Biiling cycle
        Frequency f = new Frequency(IntervalUnit.MONTH,1);

        //First Billing Cycle
        BillingCycle billingCycle = new BillingCycle();
        PricingScheme pricingScheme = new PricingScheme(new FixedPrice("10","USD"));
        billingCycle.setFrequency(f);
        billingCycle.setTenure_type(TenureType.TRIAL);
        billingCycle.setSequence(1);
        billingCycle.setTotal_cycles(1);
        billingCycle.setPricing_scheme(pricingScheme);
        //Second Billing cycle
        BillingCycle billingCycle2 = new BillingCycle();
        billingCycle2.setFrequency(f);
        billingCycle2.setTenure_type(TenureType.REGULAR);
        billingCycle2.setSequence(2);
        billingCycle2.setTotal_cycles(12);

        billingCycle2.setPricing_scheme(pricingScheme);
        ArrayList<BillingCycle> billingCycles = new ArrayList<>();
        billingCycles.add(billingCycle);
        billingCycles.add(billingCycle2);
        billingPlan.setBilling_cycles(billingCycles);

        SetupFee setupFee = new SetupFee("10","USD");
        PaymentPreferences paymentPreferences = new PaymentPreferences();
        paymentPreferences.setAuto_bill_outstanding(true);
        paymentPreferences.setSetup_fee(setupFee);
        paymentPreferences.setSetup_fee_failure_action(SetupFeeFailureAction.CONTINUE);
        paymentPreferences.setPayment_failure_threshold(3);
        billingPlan.setPayment_preferences(paymentPreferences);
        Taxes taxes = new Taxes("10",false);
        billingPlan.setTaxes(taxes);

        Gson builder = new GsonBuilder().create();
        HttpEntity<String> request = new HttpEntity<>(builder.toJson(billingPlan), headers);
        System.out.println("REQUEST");
        System.out.println(builder.toJson(request));
        String response = restTemplate.postForObject(paypalUrl, request, String.class);
        System.out.println("RESPONSE");
        System.out.println(response);

        return new ResponseEntity<>(builder.toJson(response), HttpStatus.OK);
    }

    @PostMapping(path = "/subscription")
    public ResponseEntity<?> createSubscriptionPlan(@RequestBody SubscriptionDTO subscriptionDTO){
        String token = accessToken();

        String paypalUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);


        Gson builder = new GsonBuilder().create();
        HttpEntity<String> request = new HttpEntity<>(builder.toJson(subscriptionDTO), headers);
        System.out.println("REQUEST");
        System.out.println(builder.toJson(request));
        String response = restTemplate.postForObject(paypalUrl, request, String.class);
        System.out.println("RESPONSE");
        System.out.println(response);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    private LinkDescription extractApprovalLink(com.paypal.orders.Order order) {
        LinkDescription approveUri = order.links().stream()
                .filter(link -> APPROVAL_LINK.equals(link.rel()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        return approveUri;
    }
    private String accessToken(){
        String tokenUrl = "https://api.sandbox.paypal.com/v1/oauth2/token";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        AuthToken token = restTemplate.postForObject(tokenUrl, request, AuthToken.class);
        if(token != null){
            return token.getAccess_token();
        }

        return "";
    }

    HttpHeaders createHeaders(){
        return new HttpHeaders() {{
            String auth = clientId + ":" + clientSecret;
            byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(Charset.forName("US-ASCII")) );
            String authHeader = "Basic " + new String( encodedAuth );
            set( "Authorization", authHeader );
        }};
    }
}
