package com.roulezmalin.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeocodingService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public double[] getCoordinates(String adresse) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" + adresse + "&format=json&limit=1";
            // Note: Nominatim demande un User-Agent pour ne pas bloquer
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);
            
            if (root.isArray() && root.size() > 0) {
                double lat = root.get(0).get("lat").asDouble();
                double lon = root.get(0).get("lon").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new double[]{48.8566, 2.3522}; // Retourne Paris par défaut en cas d'erreur
    }
}