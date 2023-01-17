package com.PayPal;

import com.PayPal.dto.CreateOrderFromPaymentInfoDTO;
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
import org.springframework.web.servlet.view.RedirectView;

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
    public RedirectView captureOrder(@RequestParam String token) throws IOException {

        String orderId = token;
        String pspUrl = "http://localhost:8761/paymentInfo/confirm";

        orderService.captureOrder(token);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject obj = new JSONObject();
        try {
            obj.put("transactionId", orderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HttpEntity<String> request = new HttpEntity<>(obj.toString(), headers);
        String paymentResponse = restTemplate.postForObject(pspUrl, request, String.class);

        return new RedirectView("http://localhost:4200/confirmation");
    }


    @PostMapping("/create")
    public CreatePaymentResponseDTO creatingOrder(@RequestBody CreateOrderFromPaymentInfoDTO dto, HttpServletRequest request){

        try {
            final URI returnUrl = orderService.buildReturnUrl(request);
            Order order = orderService.createOrder(dto.getTotalAmount(), returnUrl, dto.getMerchantId());
            logger.info("Paypal order object created and approval link for redirection.");
            orderService.browse(order.getApprovalLink().toString());

            return new CreatePaymentResponseDTO(returnUrl.toString(), order.getOrderId());

        } catch (Exception e) {
            logger.error("Exception with creating paypal order object. Error is: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
