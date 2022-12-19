package com.PayPal;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URI;


@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    Logger logger = LoggerFactory.getLogger(OrderService.class);


    @GetMapping("/capture")
    public String captureOrder(@RequestParam String token) throws IOException {
        String orderId = token;
        orderService.captureOrder(token);
        return "redirect:"; // stavi redirect nazad na front new RedirectView....
    }

    @PostMapping
    public String createOrder(@RequestParam Double totalAmount, HttpServletRequest request) throws IOException{
        try {
            final URI returnUrl = orderService.buildReturnUrl(request);
            String webShopId = "BAGSGQXCCH7WU";
            Order order = orderService.createOrder(totalAmount, returnUrl, webShopId);
            logger.info("Paypal order object created and approval link for redirection.");
            System.out.println(order);
            orderService.browse(order.getApprovalLink().toString());
        } catch (Exception e) {
            logger.error("Exception with creating paypal order object. Error is: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return "redirect: approval_link";
    }


}
