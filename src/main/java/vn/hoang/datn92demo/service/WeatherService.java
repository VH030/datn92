package vn.hoang.datn92demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import vn.hoang.datn92demo.dto.response.WeatherResponseDTO;

import java.util.List;
import java.util.Map;

/**
 * WeatherService (SimpleClientHttpRequestFactory version)
 * - Không dùng API deprecated
 * - Tương thích Spring Boot 3.5.x (Spring Web 6.x)
 * - Logging chi tiết, dễ debug khi provider trả lỗi
 */
@Service
public class WeatherService {

    private final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${weather.api.url:https://api.openweathermap.org/data/2.5/weather}")
    private String apiUrl;

    @Value("${weather.api.key:}")
    private String apiKey;

    @Value("${weather.api.timeout-ms:5000}")
    private long timeoutMs;

    public WeatherService(RestTemplateBuilder builder,
                          @Value("${weather.api.timeout-ms:5000}") long timeoutMsConfig) {

        // Configure simple HTTP request factory (non-deprecated)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int to = (int) Math.min(timeoutMsConfig, Integer.MAX_VALUE);
        factory.setConnectTimeout(to);
        factory.setReadTimeout(to);

        this.restTemplate = builder
                .requestFactory(() -> factory)
                .build();
    }

    public WeatherResponseDTO getWeatherByLatLon(Double lat, Double lon) {
        if (lat == null || lon == null) {
            log.warn("getWeatherByLatLon called with null lat/lon");
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Weather API key missing (weather.api.key).");
            return null;
        }

        String url = String.format("%s?lat=%s&lon=%s&units=metric&appid=%s",
                apiUrl, lat, lon, apiKey);

        log.debug("Calling weather provider: {}", maskApiKey(url));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> req = new HttpEntity<>(headers);

            ResponseEntity<String> respEntity =
                    restTemplate.exchange(url, HttpMethod.GET, req, String.class);

            // FIX: use HttpStatusCode (Spring Web 6)
            HttpStatusCode status = respEntity.getStatusCode();
            String body = respEntity.getBody();

            if (!status.is2xxSuccessful()) {
                log.error("Weather provider returned {}. Body = {}", status.value(), safe(body));
                return null;
            }
            if (body == null || body.isBlank()) {
                log.error("Weather provider returned empty body (200)");
                return null;
            }

            Map<String, Object> json = objectMapper.readValue(body, Map.class);

            Map<String, Object> main = (Map<String, Object>) json.get("main");
            Map<String, Object> wind = (Map<String, Object>) json.get("wind");
            List<Map<String, Object>> weatherArr = (List<Map<String, Object>>) json.get("weather");

            String desc = null;
            if (weatherArr != null && !weatherArr.isEmpty()) {
                desc = (String) weatherArr.get(0).get("description");
            }

            Double temp = main != null && main.get("temp") != null ? ((Number) main.get("temp")).doubleValue() : null;
            Double feelsLike = main != null && main.get("feels_like") != null ? ((Number) main.get("feels_like")).doubleValue() : null;
            Integer humidity = main != null && main.get("humidity") != null ? ((Number) main.get("humidity")).intValue() : null;
            Double windSpeed = wind != null && wind.get("speed") != null ? ((Number) wind.get("speed")).doubleValue() : null;

            WeatherResponseDTO dto = new WeatherResponseDTO();
            dto.setDescription(desc);
            dto.setTemperature(temp);
            dto.setFeelsLike(feelsLike);
            dto.setHumidity(humidity);
            dto.setWindSpeed(windSpeed);
            dto.setTimestamp(System.currentTimeMillis());
            dto.setProvider("openweathermap");

            return dto;

        } catch (HttpClientErrorException e) {
            log.error("Weather provider returned 4xx {} {}. Body={}",
                    e.getStatusCode(), e.getStatusText(), safe(e.getResponseBodyAsString()));
        } catch (HttpServerErrorException e) {
            log.error("Weather provider returned 5xx {} {}. Body={}",
                    e.getStatusCode(), e.getStatusText(), safe(e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            log.error("Network timeout / DNS / refusing connection: {}", e.toString());
        } catch (Exception e) {
            log.error("Unexpected error calling weather provider", e);
        }
        return null;
    }

    // ----- helper methods -----

    private String safe(String s) {
        if (s == null) return "";
        return s.length() > 1000 ? s.substring(0, 1000) + "...(truncated)" : s;
    }

    private String maskApiKey(String url) {
        if (url == null) return "";
        return url.replaceAll("appid=[^&]+", "appid=****");
    }
}
