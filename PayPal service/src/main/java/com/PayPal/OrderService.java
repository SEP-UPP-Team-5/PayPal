package com.PayPal;

import com.google.gson.Gson;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.paypal.orders.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class OrderService {
    private final String APPROVAL_LINK = "approve";
    private final PayPalHttpClient payPalHttpClient;

    @Autowired
    private PaymentInfoRepository paymentInfoRepository;

    Logger logger = LoggerFactory.getLogger(OrderService.class);

    public OrderService(@Value("${paypal.clientId}") String clientId,
                        @Value("${paypal.clientSecret}") String clientSecret) {
        payPalHttpClient = new PayPalHttpClient(new PayPalEnvironment.Sandbox(clientId, clientSecret));
    }

    public Order createOrder(Double totalAmount, URI returnUrl, String webShopId) throws IOException {
        final OrderRequest orderRequest = createOrderRequest(totalAmount, returnUrl, webShopId);
        System.out.println("orderRequest");
        System.out.println(new Gson().toJson(orderRequest));
        final OrdersCreateRequest ordersCreateRequest = new OrdersCreateRequest().requestBody(orderRequest);
        final HttpResponse<com.paypal.orders.Order> orderHttpResponse = payPalHttpClient.execute(ordersCreateRequest);
        final com.paypal.orders.Order order = orderHttpResponse.result();
        System.out.println("order");
        System.out.println(new Gson().toJson(order));
        LinkDescription approveUri = extractApprovalLink(order);

        logger.info("Order: ID:" +  orderHttpResponse.result().id() + ", status:{}", orderHttpResponse.result().status());

        return new Order(order.id(), URI.create(approveUri.href()));
    }

    public void captureOrder(String orderId) throws IOException {
        final OrdersCaptureRequest ordersCaptureRequest = new OrdersCaptureRequest(orderId);
        final HttpResponse<com.paypal.orders.Order> httpResponse = payPalHttpClient.execute(ordersCaptureRequest);

        System.out.println("httpResponse");
        System.out.println(new Gson().toJson(httpResponse.result()));
        logger.info("Order: ID:" +  httpResponse.result().id() + ", status:{}", httpResponse.result().status());

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentId(httpResponse.result().id());
        paymentInfo.setPayerId(httpResponse.result().payer().payerId()); // PayPal account ID
        paymentInfo.setAmount(httpResponse.result().purchaseUnits().get(0).payments().captures().get(0).amount().value());
        paymentInfo.setCurrency(httpResponse.result().purchaseUnits().get(0).payments().captures().get(0).amount().currencyCode());
        paymentInfo.setDate(httpResponse.result().purchaseUnits().get(0).payments().captures().get(0).createTime());
        paymentInfoRepository.save(paymentInfo);
    }


    private OrderRequest createOrderRequest(Double totalAmount, URI returnUrl, String webShopId) {
        final OrderRequest orderRequest = new OrderRequest();
        setCheckoutIntent(orderRequest);  // CAPTURE, AUTHORIZE
        setPurchaseUnits(totalAmount, webShopId, orderRequest);
        setApplicationContext(returnUrl, orderRequest);
        System.out.println("Creating order request");

        return orderRequest;
    }
    private OrderRequest setApplicationContext(URI returnUrl, OrderRequest orderRequest) {
        return orderRequest.applicationContext(new ApplicationContext().returnUrl(returnUrl.toString()));
    }

    private void setPurchaseUnits(Double totalAmount, String webShopId, OrderRequest orderRequest) {
        final PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .amountWithBreakdown(new AmountWithBreakdown().currencyCode("USD").value(totalAmount.toString()))
                .payee(new Payee().merchantId(webShopId));
        orderRequest.purchaseUnits(Arrays.asList(purchaseUnitRequest));
    }

    private void setCheckoutIntent(OrderRequest orderRequest) {
        orderRequest.checkoutPaymentIntent("CAPTURE");
    }

    private LinkDescription extractApprovalLink(com.paypal.orders.Order order) {
        LinkDescription approveUri = order.links().stream()
                .filter(link -> APPROVAL_LINK.equals(link.rel()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);
        return approveUri;
    }

    public URI buildReturnUrl(HttpServletRequest request) {
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

    public static void browse(String url) {
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


}
