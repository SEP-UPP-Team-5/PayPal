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

    public static final String RETURN_URL = "https://example.com/aprove";
    public static final String CANCEL_URL = "https://example.com/return";

    public String clientId = "AZGNaxtL_IE4kyAE1B_z3ZfcBiTFYqtlLLBPF1Xl9l1ayULedWlRw99KcNV5RmdQMO_YmceWKBqCqqay";
    public String clientSecret = "EMW9_CDav9jiDtfGr8N2llSzRjU5BeGp2CbTpuTmJmPFk3qQL1vLpuEHbwyDquyeLunEwasqxsPeV3bh";

    @GetMapping("")
    public String home() {
        return "paypal home";
    }

    @PostMapping("/pay")
    public String createPayment(@RequestBody Order order) {
        try {
            Payment payment = service.createPayment(order.getPrice(), order.getCurrency(), order.getMethod(),
                    order.getIntent(), order.getDescription(), RETURN_URL,
                     CANCEL_URL, order.getClientId(), order.getClientSecret());
            logger.info("Paypal payment object created.");
            System.out.println(payment.toJSON());
            for(Links link:payment.getLinks()) {
                if(link.getRel().equals("approval_url")) {
                    logger.info("Paypal api link for redirection.");
                    return link.getHref();
                }
            }

        } catch (PayPalRESTException e) {
            logger.error("Exception when creating paypal payment object. Error is: " + e );
            e.printStackTrace();
        }
        return "link";
    }

    @GetMapping(value = CANCEL_URL)
    public String cancelPay() {
        return "cancel";
    }

    @GetMapping("/success")
    public String approvePayment(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId) {
        try {
            Payment payment = service.executePayment(paymentId, payerId, clientId ,clientSecret );
            System.out.println(payment.toJSON());
            if (payment.getState().equals("approved")) {
                service.savePayment(payment);
                logger.info("Paypal payment finished.");
              //  service.browse(RETURN_URL);

                return "success";
            }
        } catch (PayPalRESTException e) {
            System.out.println(e.getMessage());
            logger.error("Exception with finalizing paypal payment. Error is: " + e );
          //  service.browse(CANCEL_URL);
        }
        return "error";
    }


}
