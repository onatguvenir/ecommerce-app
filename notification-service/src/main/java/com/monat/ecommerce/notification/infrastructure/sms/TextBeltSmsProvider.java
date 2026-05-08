package com.monat.ecommerce.notification.infrastructure.sms;

import com.monat.ecommerce.notification.domain.service.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Free SMS provider via TextBelt.
 * - key=textbelt_test → always succeeds, no SMS sent (good for dev/CI)
 * - key=textbelt      → 1 free real SMS per day per IP
 * - Custom paid key   → higher quota
 * No account or SDK needed — plain HTTP POST.
 */
@Slf4j
public class TextBeltSmsProvider implements SmsProvider {

    private static final String TEXTBELT_URL = "https://textbelt.com/text";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public TextBeltSmsProvider(String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    @Override
    public void send(String to, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("phone", to);
            body.add("message", message);
            body.add("key", apiKey);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TEXTBELT_URL, entity, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("TextBelt SMS sent to {} (textId={})", to, response.get("textId"));
            } else {
                String error = response != null ? String.valueOf(response.get("error")) : "null response";
                log.warn("TextBelt SMS failed to {}: {}", to, error);
            }
        } catch (Exception e) {
            log.error("TextBelt SMS error sending to {}: {}", to, e.getMessage());
        }
    }
}
