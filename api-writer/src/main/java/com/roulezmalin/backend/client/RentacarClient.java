package com.roulezmalin.backend.client;

import com.roulezmalin.backend.model.MingatAgence;
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

    public record RentacarResult(String json, String url) {}

    public RentacarResult fetchDisponibilites(Trajet trajet) {
        
        // double[] coordsDep = geocodingService.getCoordinates(trajet.getAddresseDepart());
        // double[] coordsArr = geocodingService.getCoordinates(trajet.getAddresseArrivee());

        String[] coordsDep = {trajet.getDepartLat(), trajet.getDepartLon()};
        String[] coordsArr = {trajet.getArriveLat(), trajet.getArriveLon()};

        System.out.println("[RENTACAR] : Coordonnées de départ : " + coordsDep[0] + ", " + coordsDep[1]);
        System.out.println("[RENTACAR] : Coordonnées d'arrivée : " + coordsArr[0] + ", " + coordsArr[1]);

        String dateDepFormatee = formaterDatePourRentacar(trajet.getDateDepart());
        String dateArrFormatee = formaterDatePourRentacar(trajet.getDateArrivee());

        String rideType = "OneWay";

        System.out.println("[RENTACAR] : Addresse de départ : " + trajet.getAddresseDepart());
        System.out.println("[RENTACAR] : Addresse d'arrivée : " + trajet.getAddresseArrivee());

        if(trajet.getDepartLat().equals(trajet.getArriveLat()) && trajet.getDepartLon().equals(trajet.getArriveLon())) {
            System.out.println("[RENTACAR] : Trajet aller-retour détecté.");
            rideType = "RoundTrip";
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://apiv2-www.rentacar.fr/api/agencies/v1/disponibilities")
                .queryParam("RideType", rideType)
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
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-site");


        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return new RentacarResult(response.getBody(), url);
        } catch (Exception e) {
            System.err.println("[RENTACAR] : Détail de l'erreur : " + e.getMessage());
            throw e;
        }
    }

    public List<OffreAffichage> parserResultatsRentacar(String jsonBrut, String urlTrajet) {
        List<OffreAffichage> offresTrouvees = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonBrut);
            JsonNode disponibilities = root.path("disponibilities");

            for (JsonNode dispo : disponibilities) {
                // Extraction des infos de l'agence au niveau "disponibility"
                String nomAgence = dispo.path("pickupAgency").path("name").asText();
                String gpsStart = dispo.path("pickupAgency").path("latitude").asText() + "," + 
                                dispo.path("pickupAgency").path("longitude").asText();
                String gpsEnd = dispo.path("dropoffAgency").path("latitude").asText() + "," + 
                                dispo.path("dropoffAgency").path("longitude").asText();

                JsonNode categories = dispo.path("categories");

                for (JsonNode cat : categories) {
                    // Traitement de l'image
                    String img = cat.path("vehicleImageUrl").asText();
                    if (img.startsWith("/")) {
                        img = "https://www.rentacar.fr" + img;
                    }

                    // Détection de la clim dans la liste des équipements
                    boolean aLaClim = false;
                    for (JsonNode eq : cat.path("details").path("equipments")) {
                        if (eq.asText().toLowerCase().contains("clim") || eq.asText().toLowerCase().contains("air conditionné")) {
                            aLaClim = true;
                            break;
                        }
                    }

                    // Gestion du type de moteur (souvent null dans ce JSON)
                    String moteur = cat.path("motorizationTypeLabel").isMissingNode() || cat.path("motorizationTypeLabel").isNull() 
                                    ? "Non spécifié" 
                                    : cat.path("motorizationTypeLabel").asText();

                    // Ajout à la liste via le nouveau constructeur
                    offresTrouvees.add(new OffreAffichage(
                        cat.path("vehicleLabel").asText(),           // nomVehicule
                        cat.path("vehicleExample").asText(),         // exempleModele
                        String.valueOf(cat.path("price").path("finalPrice").asDouble()), // prixTotal
                        cat.path("gearboxTypeLabel").asText(),       // boiteVitesse
                        cat.path("seaterNumber").asInt(),            // nbPlaces
                        moteur,                                      // typeMoteur
                        img,                                         // imageUrl
                        aLaClim,                                     // clim
                        cat.path("largeLuggageNumber").asInt(),      // nbBagages
                        gpsStart,                                    // gpsDemarrage
                        gpsEnd,                                      // gpsArrivee
                        nomAgence,                                  // nomAgence
                        "RentaCar", 
                        urlTrajet
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[RENTACAR] : Erreur lors du parsing : " + e.getMessage());
        }
        return offresTrouvees;
    }
}
