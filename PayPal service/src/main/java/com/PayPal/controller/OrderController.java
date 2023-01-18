package com.PayPal.controller;

import com.PayPal.dto.CaptureOrderResponseDTO;
import com.PayPal.dto.CreateOrderFromPaymentInfoDTO;
import com.PayPal.dto.CreatePaymentResponseDTO;
import com.PayPal.model.MyOrder;
import com.PayPal.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URI;

import static java.lang.Long.parseLong;


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
    public ResponseEntity<?> captureOrder(@RequestParam String token, @RequestParam String PayerID) throws IOException {

        String orderId = token;
        orderService.captureOrder(token);
        MyOrder payPalOrder = orderService.findOrder(orderId);
        String pspUrl = "http://localhost:8761/paymentInfo/confirm";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject obj = new JSONObject();
        try {
            obj.put("webShopOrderId", payPalOrder.getWebShopOrderId());
            obj.put("payerId", PayerID);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        HttpEntity<String> request = new HttpEntity<>(obj.toString(), headers);
        CaptureOrderResponseDTO captureOrderResponse = restTemplate.postForObject(pspUrl, request, CaptureOrderResponseDTO.class);

        return new ResponseEntity<>(captureOrderResponse, HttpStatus.OK);
    }


    @PostMapping("/create")
    public CreatePaymentResponseDTO creatingOrder(@RequestBody CreateOrderFromPaymentInfoDTO dto, HttpServletRequest request){

        try {
            final URI returnUrl = orderService.buildReturnUrl(request);
            MyOrder order = orderService.createOrder(dto, returnUrl);
            logger.info("Paypal order object created and approval link for redirection.");
            //orderService.browse(order.getApprovalLink());

            return new CreatePaymentResponseDTO(order.getApprovalLink());

        } catch (Exception e) {
            logger.error("Exception with creating paypal order object. Error is: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
