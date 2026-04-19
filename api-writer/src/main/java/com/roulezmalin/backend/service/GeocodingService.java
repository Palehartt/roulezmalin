package com.roulezmalin.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GeocodingService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    public double[] getCoordinates(String adresse) {
        // Nettoyage : on garde l'adresse complète mais on supprime
        // les segments parasites (ex: "Rose de Mesure Localizer 17L")
        String query = cleanAddress(adresse);

        if (cache.containsKey(query)) {
            System.out.println("[GEOCODING] Cache : " + query);
            return cache.get(query);
        }

        double[] result;

        // 1. Tentative API Gouv (adresses postales)
        result = tryApiGouv(query);
        if (result != null) {
            cache.put(query, result);
            return result;
        }

        // 2. Fallback Nominatim (POI, aéroports, gares...)
        result = tryNominatim(query);
        if (result != null) {
            cache.put(query, result);
            return result;
        }

        // 3. Fallback Photon (même base OSM, autre moteur)
        result = tryPhoton(query);
        if (result != null) {
            cache.put(query, result);
            return result;
        }

        System.err.println("[GEOCODING] Aucun résultat pour : " + query);
        return new double[]{45.7640, 4.8357}; // Lyon par défaut
    }

    // ----------------------------------------------------------------
    // Nettoyage de l'adresse
    // ----------------------------------------------------------------
    private String cleanAddress(String adresse) {
        if (adresse == null) return "";

        String[] parts = adresse.split(",");
        StringBuilder cleaned = new StringBuilder();

        for (String part : parts) {
            String p = part.trim();
            // On ignore les segments techniques/parasites
            if (p.matches(".*\\b(Localizer|ILS|DME|RWY|VOR|NDB|\\d{2}[LRC])\\b.*")) continue;
            if (p.isEmpty()) continue;
            if (cleaned.length() > 0) cleaned.append(", ");
            cleaned.append(p);
        }

        return cleaned.toString();
    }

    // ----------------------------------------------------------------
    // 1. API Adresse du gouvernement (adresses postales FR)
    // ----------------------------------------------------------------
    private double[] tryApiGouv(String query) {
        try {
            String url = "https://api-adresse.data.gouv.fr/search/?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&limit=1";

            System.out.println("[GEOCODING] API Gouv : " + url);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode features = mapper.readTree(response).path("features");

            if (features.isArray() && features.size() > 0) {
                // Score de confiance : on rejette les résultats trop flous
                double score = features.get(0).path("properties").path("score").asDouble(0);
                if (score < 0.5) {
                    System.out.println("[GEOCODING] API Gouv score trop bas (" + score + "), fallback");
                    return null;
                }
                JsonNode coords = features.get(0).path("geometry").path("coordinates");
                return new double[]{coords.get(1).asDouble(), coords.get(0).asDouble()};
            }
        } catch (Exception e) {
            System.err.println("[GEOCODING] API Gouv erreur : " + e.getMessage());
        }
        return null;
    }

    // ----------------------------------------------------------------
    // 2. Nominatim / OpenStreetMap (POI, lieux, aéroports...)
    // ----------------------------------------------------------------
    private double[] tryNominatim(String query) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=fr&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);

            System.out.println("[GEOCODING] Nominatim : " + url);

            HttpHeaders headers = new HttpHeaders();
            // Nominatim exige un User-Agent identifiable
            headers.set("User-Agent", "RoulezMalin/1.0 (contact@roulezmalin.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            if (root.isArray() && root.size() > 0) {
                double lat = root.get(0).path("lat").asDouble();
                double lon = root.get(0).path("lon").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("[GEOCODING] Nominatim erreur : " + e.getMessage());
        }
        return null;
    }

    // ----------------------------------------------------------------
    // 3. Photon (OSM, tolérant, sans rate limit strict)
    // ----------------------------------------------------------------
    private double[] tryPhoton(String query) {
        try {
            // bbox France métropolitaine
            String url = "https://photon.komoot.io/api/?q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&limit=1&lang=fr&bbox=-5.1,41.3,9.6,51.1";

            System.out.println("[GEOCODING] Photon : " + url);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode features = mapper.readTree(response).path("features");

            if (features.isArray() && features.size() > 0) {
                JsonNode coords = features.get(0).path("geometry").path("coordinates");
                // GeoJSON = [lon, lat]
                return new double[]{coords.get(1).asDouble(), coords.get(0).asDouble()};
            }
        } catch (Exception e) {
            System.err.println("[GEOCODING] Photon erreur : " + e.getMessage());
        }
        return null;
    }
}