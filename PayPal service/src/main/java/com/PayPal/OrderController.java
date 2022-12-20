package com.PayPal;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;


@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    Logger logger = LoggerFactory.getLogger(OrderService.class);


    @GetMapping("/capture")
    public String captureOrder(@RequestParam String token) throws IOException {
        String orderId = token;
        orderService.captureOrder(token);

        ServiceInstance serviceInstance = loadBalancerClient.choose("web-shop");
        URI url = serviceInstance.getUri();
        System.out.println(url);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        //TODO: headers.setBearerAuth(token);
        JSONObject obj = new JSONObject();
        try {
            obj.put("purchaseId", orderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HttpEntity<String> request = new HttpEntity<>(obj.toString(), headers);
        String paymentResponse = restTemplate.postForObject(url +"/purchase/confirm", request, String.class);    //TODO response class
        System.out.println(paymentResponse);
        return "redirect:"; // stavi redirect nazad na front new RedirectView....
    }
/*
    @PostMapping
    public String createOrder(@RequestParam Double totalAmount, HttpServletRequest request) throws IOException{
        try {
            final URI returnUrl = orderService.buildReturnUrl(request);
            String webShopId = "BAGSGQXCCH7WU";
            Order order = orderService.createOrder(totalAmount, returnUrl, webShopId, "");
            logger.info("Paypal order object created and approval link for redirection.");
            System.out.println(order);
            orderService.browse(order.getApprovalLink().toString());
        } catch (Exception e) {
            logger.error("Exception with creating paypal order object. Error is: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return "redirect: approval_link";
    }*/

    @PostMapping("/create")
    public CreatePaymentResponseDTO creatingOrder(@RequestBody CreatePaymentDTO createPaymentDTO, HttpServletRequest request){

        try {
            final URI returnUrl = orderService.buildReturnUrl(request);
            String webShopId = createPaymentDTO.getMerchantId();
            Order order = orderService.createOrder(createPaymentDTO.getPrice(), returnUrl, webShopId, createPaymentDTO.applicationName);
            logger.info("Paypal order object created and approval link for redirection.");
            System.out.println(order);
            orderService.browse(order.getApprovalLink().toString());

            return new CreatePaymentResponseDTO(returnUrl.toString(), order.getOrderId());
        } catch (Exception e) {
            logger.error("Exception with creating paypal order object. Error is: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }


}
