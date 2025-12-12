package vn.hoang.datn92demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RemoteSmsService implements NotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sms-gateway.url}")
    private String smsGatewayUrl;

    @Value("${sms-gateway.api-key}")
    private String smsGatewayApiKey;

    @Override
    @Async("smsExecutor") // dùng AsyncConfig để gửi nền
    public void sendSms(String phone, String message) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("apiKey", smsGatewayApiKey);
            body.put("phone", phone);
            body.put("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp =
                    restTemplate.postForEntity(smsGatewayUrl, entity, String.class);

            System.out.println("[REMOTE-SMS] Gateway response: " +
                    resp.getStatusCode() + " - " + resp.getBody());
        } catch (Exception e) {
            System.err.println("[REMOTE-SMS] Error calling SMS gateway: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
