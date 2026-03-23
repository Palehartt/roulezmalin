package com.roulezmalin.backend.client;

import com.roulezmalin.backend.model.OffreAffichage;
import com.roulezmalin.backend.model.Trajet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.roulezmalin.backend.service.GeocodingService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.util.Collections;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@Service
public class RentacarClient {

    @Autowired
    private GeocodingService geocodingService;

    private final RestTemplate restTemplate = new RestTemplate();

    private String formaterDatePourRentacar(String dateRecue) {
    // 1. On définit le format qui arrive du front (ex: 04/03/2026 12:00)
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // 2. On parse la String en objet LocalDateTime
    LocalDateTime date = LocalDateTime.parse(dateRecue, inputFormatter);
    
    // 3. On ressort le format attendu par Rentacar (ex: 2026-03-04T12:00:00)
    // Le format ISO_LOCAL_DATE_TIME ajoute automatiquement le 'T'
    return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
}

    public String fetchDisponibilites(Trajet trajet) {
        // 1. On transforme les adresses saisies par l'utilisateur en coordonnées
        double[] coordsDep = geocodingService.getCoordinates(trajet.getAddresseDepart());
        double[] coordsArr = geocodingService.getCoordinates(trajet.getAddresseArrivee());

        System.out.println("Coordonnées de départ : " + coordsDep[0] + ", " + coordsDep[1]);
        System.out.println("Coordonnées d'arrivée : " + coordsArr[0] + ", " + coordsArr[1]);

        String dateDepFormatee = formaterDatePourRentacar(trajet.getDateDepart());
        String dateArrFormatee = formaterDatePourRentacar(trajet.getDateArrivee());

        // 2. On construit l'URL avec ces coordonnées en direct
        String url = UriComponentsBuilder.fromHttpUrl("https://apiv2-www.rentacar.fr/api/agencies/v1/disponibilities")
                .queryParam("RideType", "RoundTrip")
                .queryParam("Pickup.Latitude", coordsDep[0])
                .queryParam("Pickup.Longitude", coordsDep[1])
                .queryParam("Pickup.MaxNumberOfAgencies", 3)
                .queryParam("Radius", 50)
                .queryParam("VehicleType", "Car")
                .queryParam("PickupDate", dateDepFormatee)
                .queryParam("DropoffDate", dateArrFormatee)
                // On met l'adresse brute ici, mais c'est le point fragile
                .queryParam("pickupDescription", trajet.getAddresseDepart()) 
                .queryParam("isAgency", false)
                .queryParam("Dropoff.Latitude", coordsArr[0])
                .queryParam("Dropoff.Longitude", coordsArr[1])
                .queryParam("Dropoff.MaxNumberOfAgencies", 5)
                .queryParam("dropoffDescription", trajet.getAddresseArrivee())
                .toUriString();

        System.out.println("URL construite pour Rentacar : " + url);

        // 1. Création des Headers pour "tromper" le pare-feu
        HttpHeaders headers = new HttpHeaders();
        // On simule un navigateur Chrome sur Windows
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Origin", "https://www.rentacar.fr");
        headers.set("Referer", "https://www.rentacar.fr/");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // 2. On utilise exchange au lieu de getForObject pour passer les headers
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Détail de l'erreur : " + e.getMessage());
            throw e;
        }
    }

    public List<OffreAffichage> parserResultatsRentacar(String jsonBrut) {
        List<OffreAffichage> offresTrouvees = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonBrut);
            JsonNode disponibilities = root.path("disponibilities");

            // On parcourt chaque groupe de disponibilités
            for (JsonNode dispo : disponibilities) {
                JsonNode categories = dispo.path("categories");

                // On parcourt chaque catégorie de véhicule dans ce groupe
                for (JsonNode cat : categories) {
                    // Dans ta boucle for (JsonNode cat : categories)
                    String img = cat.path("vehicleImageUrl").asText();
                    // Si l'URL est relative, on peut rajouter le domaine (à vérifier selon le JSON reçu)
                    if (img.startsWith("/")) {
                        img = "https://www.rentacar.fr" + img;
                    }

                    offresTrouvees.add(new OffreAffichage(
                        cat.path("vehicleLabel").asText(),
                        cat.path("vehicleExample").asText(),
                        cat.path("price").path("finalPrice").asDouble(),
                        cat.path("gearboxTypeLabel").asText(),
                        cat.path("seaterNumber").asInt(),
                        cat.path("motorizationTypeLabel").asText(),
                        img // <-- On passe l'URL ici
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing JSON : " + e.getMessage());
        }
        return offresTrouvees;
    }
}
