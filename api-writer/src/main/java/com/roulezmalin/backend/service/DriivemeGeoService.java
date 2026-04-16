package com.roulezmalin.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DriivemeGeoService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Integer getCityId(String cityName) {
        String url = "https://www.driiveme.com/fr-FR/search/cities";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Referer", "https://www.driiveme.com/");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("query", cityName);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode suggestions = root.path("suggestions");

            if (suggestions.isArray() && suggestions.size() > 0) {
                // On prend le premier résultat (le plus pertinent)
                return suggestions.get(0).path("id").asInt();
            }
        } catch (Exception e) {
            System.err.println("Erreur Driiveme cities: " + e.getMessage());
        }
        return null;
    }
}