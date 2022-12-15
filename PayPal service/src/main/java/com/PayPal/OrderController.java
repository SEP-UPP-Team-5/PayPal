package com.PayPal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;
    private String orderId = "";


    @GetMapping("/capture")   //2. posle logovanja i kliktanja na dugme u paypalu
    public String captureOrder(@RequestParam String token) throws IOException {
        orderId = token;
        orderService.captureOrder(token);
        return "redirect:/orders"; // stavi redirect nazad na front new RedirectView....
    }

    @PostMapping  //1. kreira link za preusmeravanje na paypal
    public String placeOrder(@RequestParam Double totalAmount, HttpServletRequest request) throws IOException {
        final URI returnUrl = buildReturnUrl(request);
        Order order = orderService.createOrder(totalAmount, returnUrl);
        orderService.browse(order.getApprovalLink().toString());
        return "redirect:"+ order.getApprovalLink();
    }

    private URI buildReturnUrl(HttpServletRequest request) {
        try {
            URI requestUri = URI.create(request.getRequestURL().toString());
            return new URI(requestUri.getScheme(),
                    requestUri.getUserInfo(),
                    requestUri.getHost(),
                    requestUri.getPort(),
                    "/orders/capture",
                    null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
