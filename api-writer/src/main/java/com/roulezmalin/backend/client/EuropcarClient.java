package com.roulezmalin.backend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roulezmalin.backend.model.EuropcarAgence;
import com.roulezmalin.backend.model.OffreAffichage;
import com.roulezmalin.backend.model.Trajet;
import com.roulezmalin.backend.service.GeocodingService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EuropcarClient {

    @Autowired
    private GeocodingService geocodingService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private List<EuropcarAgence> agences = new ArrayList<>();
    private static final double MAX_DISTANCE_KM = 50.0;

    @PostConstruct
    public void chargerDonnees() {
        try {
            InputStream is = new ClassPathResource("data/europcar_agencies.json").getInputStream();
            agences = mapper.readValue(is, new TypeReference<List<EuropcarAgence>>() {});
            System.out.println("[EUROPCAR] " + agences.size() + " agences chargées.");
        } catch (Exception e) {
            System.err.println("[EUROPCAR] Erreur chargement JSON agences : " + e.getMessage());
        }
    }

    public List<OffreAffichage> fetchOffres(Trajet trajet) {
        if (agences.isEmpty()) return new ArrayList<>();

        // 1. Géocodage et calcul de l'agence la plus proche
        // double[] coordsDep = geocodingService.getCoordinates(trajet.getAddresseDepart());
        // double[] coordsArr = geocodingService.getCoordinates(trajet.getAddresseArrivee());

        String[] coordsDep = {trajet.getDepartLat(), trajet.getDepartLon()};
        String[] coordsArr = {trajet.getArriveLat(), trajet.getArriveLon()};

        // if (coordsDep == null) return new ArrayList<>();

        EuropcarAgence agenceDep = trouverPlusProche(Double.parseDouble(coordsDep[0]), Double.parseDouble(coordsDep[1]));
        EuropcarAgence agenceArr = trouverPlusProche(Double.parseDouble(coordsArr[0]), Double.parseDouble(coordsArr[1]));

        try {
            // 2. Construction de l'URL (Dates au format ISO 8601 comme requis par l'API)
            String url = UriComponentsBuilder
                .fromHttpUrl("https://api.aws.emobg.io/group-direct-channel-context/v1/offers")
                .queryParam("apikey", "mqK5fTg12djSMga6sl1NbgeuOwbMhAxR")
                .queryParam("origin", "www.europcar.fr/onesite")
                .queryParam("vehicle-types", "CAR,LUXURY")
                .queryParam("country-of-residence", "FR")
                .queryParam("pickup-station-code", agenceDep.id)
                .queryParam("drop-off-station-code", agenceArr.id)
                .queryParam("pickup-date", formaterDateISO(trajet.getDateDepart()))
                .queryParam("drop-off-date", formaterDateISO(trajet.getDateArrivee()))
                .queryParam("driver-age", "26")
                .queryParam("sort", "-recommendation,price")
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            System.out.println("[EUROPCAR] Url envoyée : " + url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            List<OffreAffichage> offres = parserResultats(response.getBody(), agenceDep);
            return offres;

        } catch (Exception e) {
            System.err.println("[EUROPCAR] Erreur : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<OffreAffichage> parserResultats(String json, EuropcarAgence agence) {
        List<OffreAffichage> offres = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.path("data");

            for (JsonNode item : data) {
                JsonNode car = item.path("car");
                
                String nomVehicule = car.path("category").asText();
                String modele = car.path("model").asText();
                
                JsonNode firstRate = item.path("rates").get(0);
            
                if (firstRate == null) continue;

                // Prix
                double prixVal = firstRate.path("priceInDestinationCurrency").path("amount").asDouble();                
                System.out.println("[EUROPCAR] Prix : " + prixVal);
                String prixTotal = String.valueOf(prixVal);

                // Caractéristiques
                int places = car.path("capacity").path("seats").asInt();
                boolean isAuto = car.path("engine").path("automatic").asBoolean();
                String boite = isAuto ? "Automatique" : "Manuelle";
                String moteur = car.path("engine").path("type").asText("Thermique");
                String imageUrl = car.path("photos").path("medium").asText();
                boolean clim = car.path("airConditioning").asBoolean(true);
                int bagages = car.path("capacity").path("luggageItems").asInt(2);

                offres.add(new OffreAffichage(
                    nomVehicule,
                    modele,
                    prixTotal,
                    boite,
                    places,
                    moteur,
                    imageUrl,
                    clim,
                    bagages,
                    agence.latitude + "," + agence.longitude,
                    agence.latitude + "," + agence.longitude,
                    agence.Nom,
                    "Europcar"
                ));
            }
        } catch (Exception e) {
            System.err.println("[EUROPCAR] Erreur parsing : " + e.getMessage());
        }
        return offres;
    }

    // Utilitaires (identiques à Mingat)
    private String formaterDateISO(String date) {
        // "dd/MM/yyyy HH:mm" -> "yyyy-MM-ddTHH:mm"
        DateTimeFormatter in = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime dt = LocalDateTime.parse(date, in);
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    private EuropcarAgence trouverPlusProche(double lat, double lon) {
        return agences.stream()
            .min(Comparator.comparingDouble(a -> Math.pow(a.latitude - lat, 2) + Math.pow(a.longitude - lon, 2)))
            .orElse(null);
    }

    private double calculerDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}