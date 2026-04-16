package com.roulezmalin.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {
    private final ObjectMapper mapper = new ObjectMapper();

    public double[] getCoordinates(String adresse) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q="
                + java.net.URLEncoder.encode(adresse, java.nio.charset.StandardCharsets.UTF_8)
                + "&format=json&limit=1";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "RoulezMalin/1.0 (projet-universitaire)");

            var entity = new org.springframework.http.HttpEntity<>(headers);
            var restTemplate = new org.springframework.web.client.RestTemplate();

            var response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
            );

            JsonNode root = mapper.readTree(response.getBody());
            if (root.isArray() && root.size() > 0) {
                double lat = root.get(0).get("lat").asDouble();
                double lon = root.get(0).get("lon").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("[GEOCODING] Erreur : " + e.getMessage());
        }
        return new double[]{45.7640, 4.8357}; 
    }
}