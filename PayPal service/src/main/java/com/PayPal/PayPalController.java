package com.PayPal;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paypal")
public class PayPalController {

    @Autowired
    PayPalService service;

    Logger logger = LoggerFactory.getLogger(PayPalController.class);

    public static final String SUCCESS_URL = "pay/success";
    public static final String CANCEL_URL = "pay/cancel";

    @GetMapping("")
    public String home() {
        return "paypal home";
    }

    @PostMapping("/pay")
    public String payment(@RequestBody Order order) {
        try {
            Payment payment = service.createPayment(order.getPrice(), order.getCurrency(), order.getMethod(),
                    order.getIntent(), order.getDescription(), "http://localhost:8080/#/paypal home",
                    "http://localhost:8080/#/payPalParams", order.getClientId(), order.getClientSecret());
            logger.info("Paypal payment object created.");
            for(Links link:payment.getLinks()) {
                if(link.getRel().equals("approval_url")) {
                    logger.info("Sending paypal api link for redirection.");
                    return link.getHref();
                }
            }

        } catch (PayPalRESTException e) {
            logger.error("Exception with creating paypal payment object. Error is: " + e );
            e.printStackTrace();
        }
        return "greska";
    }

    @GetMapping(value = CANCEL_URL)
    public String cancelPay() {
        return "cancel";
    }

    @GetMapping(value = SUCCESS_URL)
    public String successPay(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId) {
        try {
            Payment payment = service.executePayment(paymentId, payerId);
            System.out.println(payment.toJSON());
            if (payment.getState().equals("approved")) {
                return "success";
            }
        } catch (PayPalRESTException e) {
            System.out.println(e.getMessage());
        }
        return "redirect:/";
    }


    }
