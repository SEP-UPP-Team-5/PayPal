package com.PayPal;

import com.paypal.api.payments.*;
import com.paypal.api.payments.Order;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

@Service
public class PayPalService {

    private APIContext context;

    @Autowired
    PaymentInfoRepository paymentInfoRepository;


    public Payment createPayment(
            Double total,
            String currency,
            String method,
            String intent,
            String description,
            String cancelUrl,
            String successUrl,
            String clientId,
            String clientSecret) throws PayPalRESTException {
        Amount amount = new Amount();
        amount.setCurrency(currency);
        total = new BigDecimal(total).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        amount.setTotal(String.format(Locale.ROOT, "%.2f", total));


        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod(method);

        Payment payment = new Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        String requestId = Long.toString(System.nanoTime());
        context = new APIContext(oAuthTokenCredential(clientId,clientSecret).getAccessToken(),requestId);
        context.setConfigurationMap(paypalSdkConfig());

        return payment.create(context);
    }

    public Payment executePayment(String paymentId, String payerId, String clientId, String clientSecret) throws PayPalRESTException{
        Payment payment = new Payment();
        payment.setId(paymentId);
        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);
        String requestId = Long.toString(System.nanoTime());
        context = new APIContext(oAuthTokenCredential(clientId,clientSecret).getAccessToken(),requestId);
        context.setConfigurationMap(paypalSdkConfig());

        return payment.execute(context, paymentExecute);
    }

    public void savePayment(Payment payment) {

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentId(payment.getId());
        paymentInfo.setPayerId(payment.getPayer().getPayerInfo().getPayerId());
        paymentInfo.setAmount(payment.getTransactions().get(0).getAmount().getTotal());
        paymentInfo.setCurrency(payment.getTransactions().get(0).getAmount().getCurrency());
        paymentInfo.setDate(payment.getCreateTime());

        paymentInfoRepository.save(paymentInfo);
    }

    @Bean
    public Map<String, String> paypalSdkConfig() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mode", "sandbox");
        return configMap;
    }

    @Scope("prototype")
    @Bean
    public OAuthTokenCredential oAuthTokenCredential(String clientId, String clientSecret) {
        return new OAuthTokenCredential(
                clientId,
                clientSecret,
                paypalSdkConfig());
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

