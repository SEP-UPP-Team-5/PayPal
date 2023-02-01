package com.PayPal.service;

import com.PayPal.dto.CreateOrderFromPaymentInfoDTO;
import com.PayPal.dto.CreatePaymentResponseDTO;
import com.PayPal.model.MyOrder;
import com.PayPal.model.PaymentInfo;
import com.PayPal.repository.OrderRepository;
import com.PayPal.repository.PaymentInfoRepository;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.paypal.orders.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static java.lang.Long.parseLong;

@Service
@Slf4j
public class OrderService {
    private final String APPROVAL_LINK = "approve";
    private final PayPalHttpClient payPalHttpClient;

    @Autowired
    private PaymentInfoRepository paymentInfoRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Environment env;

    Logger logger = LoggerFactory.getLogger(OrderService.class);

    public OrderService(@Value("${paypal.clientId}") String clientId,
                        @Value("${paypal.clientSecret}") String clientSecret) {
        payPalHttpClient = new PayPalHttpClient(new PayPalEnvironment.Sandbox(clientId, clientSecret));
    }

    public MyOrder createOrder(CreateOrderFromPaymentInfoDTO dto, URI returnUrl) throws Exception {
        final OrderRequest orderRequest = createOrderRequest(dto, returnUrl, URI.create(dto.getFailedUrl() + dto.getOrderId()));
        final OrdersCreateRequest ordersCreateRequest = new OrdersCreateRequest().requestBody(orderRequest);
        final HttpResponse<com.paypal.orders.Order> orderHttpResponse = payPalHttpClient.execute(ordersCreateRequest);
        if(orderHttpResponse.statusCode() != 201){
            browse(dto.getErrorUrl() + "/" + dto.getOrderId());
            throw new Exception("Error-Code: " + orderHttpResponse.statusCode() + " Status-Message: " + orderHttpResponse.result().status());
        }
        final com.paypal.orders.Order order = orderHttpResponse.result();
        LinkDescription approveUri = extractApprovalLink(order);
        logger.info("Order: ID:" +  orderHttpResponse.result().id() + ", status:{}", orderHttpResponse.result().status());
        URI appUri = URI.create(approveUri.href());
        PaymentInfo transaction = new PaymentInfo();
        transaction.setMerchantId(dto.getMerchantId());
        transaction.setAmount(dto.getTotalAmount().toString());
        transaction.setStatus(orderHttpResponse.result().status());
        transaction.setPayPalOrderId(orderHttpResponse.result().id());
        paymentInfoRepository.save(transaction);

        return orderRepository.save(new MyOrder( 1l, order.id(), appUri.toString(), dto.getOrderId())) ;
    }

    public void captureOrder(String orderId) throws Exception, IOException {
        final OrdersCaptureRequest ordersCaptureRequest = new OrdersCaptureRequest(orderId);
        final HttpResponse<com.paypal.orders.Order> httpResponse = payPalHttpClient.execute(ordersCaptureRequest);

        if(httpResponse.statusCode() != 201){
            throw new Exception("Error-Code: " + httpResponse.statusCode() + " Status-Message: " + httpResponse.result().status());
        }
        logger.info("Order: ID:" +  httpResponse.result().id() + ", price: " + httpResponse.result().purchaseUnits().get(0).payments().captures().get(0).amount().value() + ", status:{}", httpResponse.result().status() + ", payerId: " + httpResponse.result().payer().payerId());

        PaymentInfo paymentInfo = paymentInfoRepository.findByPayPalOrderId(orderId);
        paymentInfo.setPaymentId(httpResponse.result().purchaseUnits().get(0).payments().captures().get(0).id());
        paymentInfo.setPayerId(httpResponse.result().payer().payerId());
        paymentInfo.setCurrency(httpResponse.result().purchaseUnits().get(0).payments().captures().get(0).amount().currencyCode());
        paymentInfo.setPaymentTime(httpResponse.result().purchaseUnits().get(0).payments().captures().get(0).createTime());
        paymentInfo.setStatus(httpResponse.result().status());
        paymentInfoRepository.save(paymentInfo);
    }


    private OrderRequest createOrderRequest(CreateOrderFromPaymentInfoDTO dto, URI returnUrl, URI failedUrl) {
        final OrderRequest orderRequest = new OrderRequest();
        setCheckoutIntent(orderRequest);  // CAPTURE, AUTHORIZE
        setPurchaseUnits(dto.getTotalAmount(), dto.getMerchantId(), orderRequest);
        setApplicationContext(returnUrl, failedUrl, orderRequest);
        System.out.println("Creating order request");

        return orderRequest;
    }
    private OrderRequest setApplicationContext(URI returnUrl, URI failedUrl, OrderRequest orderRequest) {
        return orderRequest.applicationContext(new ApplicationContext().returnUrl(returnUrl.toString()).cancelUrl(failedUrl.toString()));
    }

    private void setPurchaseUnits(Double totalAmount, String merchantId, OrderRequest orderRequest) {
        final PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .amountWithBreakdown(new AmountWithBreakdown().currencyCode("USD").value(totalAmount.toString()))
                .payee(new Payee().merchantId(merchantId));
        orderRequest.purchaseUnits(Arrays.asList(purchaseUnitRequest));
    }

    public MyOrder findOrder(String id){
        return orderRepository.findByPayPalOrderId(id);
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
