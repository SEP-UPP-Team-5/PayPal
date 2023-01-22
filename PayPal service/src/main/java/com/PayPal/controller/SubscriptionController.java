package com.PayPal.controller;

import com.PayPal.dto.BillingPlanDTO;
import com.PayPal.dto.CreateSubscriptionResponseDTO;
import com.PayPal.dto.SubscriptionDTO;
import com.PayPal.model.*;
import com.PayPal.model.enums.IntervalUnit;
import com.PayPal.model.enums.SetupFeeFailureAction;
import com.PayPal.model.enums.TenureType;
import com.PayPal.service.SubscriptionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.api.payments.Links;
import com.paypal.base.codec.binary.Base64;
import com.paypal.orders.LinkDescription;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/subscriptions")
@Slf4j
public class SubscriptionController {

    private final String APPROVAL_LINK = "approve";
    @Autowired
    private SubscriptionService subscriptionService;

    @Value("${paypal.clientId}") String clientId;
    @Value("${paypal.clientSecret}") String clientSecret;

    @PostMapping("/product")
    public ResponseEntity<?> createProduct(@RequestBody Product product) throws JSONException {
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

        return new ResponseEntity<>(productResponse.getId(), HttpStatus.OK);
    }

    @PostMapping("/billingPlan/{product_id}")
    public ResponseEntity<?> createBillingPlan(@PathVariable String product_id) throws JSONException {
        String token = accessToken();

        String paypalUrl = "https://api-m.sandbox.paypal.com/v1/billing/plans";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        BillingPlan billingPlan = new BillingPlan();
        billingPlan.setProduct_id(product_id);
        billingPlan.setName("Subscribe to the paying plan");
        billingPlan.setDescription("Book paying plan, per month charging");
        billingPlan.setStatus("ACTIVE");

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
        BillingPlan response = restTemplate.postForObject(paypalUrl, request, BillingPlan.class);
        System.out.println("RESPONSE");
        System.out.println(response);
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
        subscriptionInfo.setBillingPlanName(response.getName());
        subscriptionInfo.setBillingPlanId(response.getId());
        subscriptionService.save(subscriptionInfo);

        return new ResponseEntity<>(response.getId(), HttpStatus.OK);
    }

    @PostMapping(path = "/subscription", produces = "application/json")
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
        CreateSubscriptionResponseDTO response = restTemplate.postForObject(paypalUrl, request, CreateSubscriptionResponseDTO.class);
        System.out.println("RESPONSE");
        System.out.println(response);

       /* for(Links link: response.getLinks()) {
            if(link.getRel() == "approve") {
                return new ResponseEntity<>(extractApprovalLink(), HttpStatus.OK);
            }
        }*/

        browse(extractApprovalLink(response).getHref());
        return new ResponseEntity<>(extractApprovalLink(response).getHref(), HttpStatus.OK);
    }

    private static void browse(String url) {
        if(Desktop.isDesktopSupported()){
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }else{
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Links extractApprovalLink(CreateSubscriptionResponseDTO dto) {
        Links approveUri = dto.getLinks().stream()
                .filter(link -> APPROVAL_LINK.equals(link.getRel()))
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
