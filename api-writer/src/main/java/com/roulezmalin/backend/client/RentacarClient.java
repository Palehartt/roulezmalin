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
    
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    
    LocalDateTime date = LocalDateTime.parse(dateRecue, inputFormatter);
    
    
    
    return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
}

    public String fetchDisponibilites(Trajet trajet) {
        
        double[] coordsDep = geocodingService.getCoordinates(trajet.getAddresseDepart());
        double[] coordsArr = geocodingService.getCoordinates(trajet.getAddresseArrivee());

        System.out.println("[RENTACAR] : Coordonnées de départ : " + coordsDep[0] + ", " + coordsDep[1]);
        System.out.println("[RENTACAR] : Coordonnées d'arrivée : " + coordsArr[0] + ", " + coordsArr[1]);

        String dateDepFormatee = formaterDatePourRentacar(trajet.getDateDepart());
        String dateArrFormatee = formaterDatePourRentacar(trajet.getDateArrivee());

        
        String url = UriComponentsBuilder.fromHttpUrl("https://apiv2-www.rentacar.fr/api/agencies/v1/disponibilities")
                .queryParam("RideType", "RoundTrip")
                .queryParam("Pickup.Latitude", coordsDep[0])
                .queryParam("Pickup.Longitude", coordsDep[1])
                .queryParam("Pickup.MaxNumberOfAgencies", 3)
                .queryParam("Radius", 50)
                .queryParam("VehicleType", "Car")
                .queryParam("PickupDate", dateDepFormatee)
                .queryParam("DropoffDate", dateArrFormatee)
                
                .queryParam("pickupDescription", trajet.getAddresseDepart()) 
                .queryParam("isAgency", false)
                .queryParam("Dropoff.Latitude", coordsArr[0])
                .queryParam("Dropoff.Longitude", coordsArr[1])
                .queryParam("Dropoff.MaxNumberOfAgencies", 5)
                .queryParam("dropoffDescription", trajet.getAddresseArrivee())
                .toUriString();

        System.out.println("URL construite pour Rentacar : " + url);

        
        HttpHeaders headers = new HttpHeaders();
        
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Origin", "https:www.rentacar.fr");
        headers.set("Referer", "https://www.rentacar.fr/");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[RENTACAR] : Détail de l'erreur : " + e.getMessage());
            throw e;
        }
    }

    public List<OffreAffichage> parserResultatsRentacar(String jsonBrut) {
        List<OffreAffichage> offresTrouvees = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonBrut);
            JsonNode disponibilities = root.path("disponibilities");

            
            for (JsonNode dispo : disponibilities) {
                JsonNode categories = dispo.path("categories");

                
                for (JsonNode cat : categories) {
                    
                    String img = cat.path("vehicleImageUrl").asText();
                    
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
                        img 
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[RENTACAR] : Erreur lors du parsing JSON : " + e.getMessage());
        }
        return offresTrouvees;
    }
}
