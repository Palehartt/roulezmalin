package com.roulezmalin.backend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roulezmalin.backend.model.MingatAgence;
import com.roulezmalin.backend.model.MingatVehicleType;
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
import java.text.DecimalFormat;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MingatClient {

    // À ajouter en haut de la classe
    private static final double MAX_DISTANCE_KM = 10.0;

    @Autowired
    private GeocodingService geocodingService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private List<MingatAgence> agences = new ArrayList<>();
    private List<MingatVehicleType> vehicleTypes = new ArrayList<>();

    @PostConstruct
    public void chargerDonnees() {
        try {
            InputStream agencesStream = new ClassPathResource("data/mingat_agencies.json").getInputStream();
            agences = mapper.readValue(agencesStream, new TypeReference<List<MingatAgence>>() {});
            System.out.println("[MINGAT] " + agences.size() + " agences chargées.");

            InputStream typesStream = new ClassPathResource("data/mingat_vehicle_types.json").getInputStream();
            vehicleTypes = mapper.readValue(typesStream, new TypeReference<List<MingatVehicleType>>() {});
            System.out.println("[MINGAT] " + vehicleTypes.size() + " types de véhicules chargés.");
        } catch (Exception e) {
            System.err.println("[MINGAT] Erreur chargement JSON : " + e.getMessage());
        }
    }

    private double calculerDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371; // Rayon de la terre en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private String formaterDate(String dateRecue) {
        DateTimeFormatter input = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime dt = LocalDateTime.parse(dateRecue, input);
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    private MingatAgence trouverPlusProche(double lat, double lon) {
        return agences.stream()
            .min(Comparator.comparingDouble(a -> {
                double dLat = a.latitude - lat;
                double dLon = a.longitude - lon;
                return dLat * dLat + dLon * dLon;
            }))
            .orElseThrow(() -> new IllegalStateException("Aucune agence Mingat chargée"));
    }

    private int parseIntSafe(String s) {
        try {
            String digits = s.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    private Optional<MingatVehicleType> trouverTypeVehicule(String typeVehicule) {
        return vehicleTypes.stream()
            .filter(t -> t.title.equalsIgnoreCase(typeVehicule))
            .findFirst();
    }

    public record MingatResult(String json, MingatAgence agence) {}

    public MingatResult fetchDisponibilites(Trajet trajet) {
        if (agences.isEmpty() || vehicleTypes.isEmpty()) {
            System.err.println("[MINGAT] Données non chargées, skip.");
            return null;
        }

        // double[] coordsDep = geocodingService.getCoordinates(trajet.getAddresseDepart());
        String[] coordsDep = {trajet.getDepartLat(), trajet.getDepartLon()};
        MingatAgence agence = trouverPlusProche(Double.parseDouble(coordsDep[0]), Double.parseDouble(coordsDep[1]));

        double distance = calculerDistanceKm(Double.parseDouble(coordsDep[0]), Double.parseDouble(coordsDep[1]), agence.latitude, agence.longitude);
    
        if (distance > MAX_DISTANCE_KM) {
            System.out.println("[MINGAT] Trop loin de la zone Rhône-Alpes (" + String.format("%.2f", distance) + " km). Recherche annulée.");
            return null;
        } else if(!trajet.getAddresseDepart().equals(trajet.getAddresseArrivee())) {
            System.out.println("[MINGAT] Pas de trajet local, skip de Mingat.");
            return null;
        }

        System.out.println("[MINGAT] Agence sélectionnée : " + agence.nom + " pour les coordonnées " + String.valueOf(coordsDep[0]) + ", " + String.valueOf(coordsDep[1]));

        String typeLabel = "tourisme";
        Optional<MingatVehicleType> typeOpt = trouverTypeVehicule(typeLabel);
        if (typeOpt.isEmpty()) {
            System.err.println("[MINGAT] Type véhicule inconnu : " + typeLabel);
            return null;
        }

        // Dates initiales depuis le trajet
        String dateDepart  = formaterDate(trajet.getDateDepart());
        String dateArrivee = formaterDate(trajet.getDateArrivee());

        // On tente l'appel, avec un retry si l'agence est fermée
        String json = tenterAppel(agence, typeOpt.get(), dateDepart, dateArrivee, false);
        return new MingatResult(json, agence);
    }

    private String tenterAppel(MingatAgence agence, MingatVehicleType type,
                                String dateDepart, String dateArrivee, boolean estRetry) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder
            .fromHttpUrl("https://www.mingat.com/api/vehicle_category_price_lists")
            .queryParam("agency",      "/api/agencies/" + agence.uuid)
            .queryParam("vehicleType", "/api/vehicle_types/" + type.uuid)
            .queryParam("departure",   dateDepart)
            .queryParam("return",      dateArrivee)
            .toUriString();

        System.out.println("[MINGAT] " + (estRetry ? "Retry" : "Appel") + " → " + url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();

        } catch (org.springframework.web.client.HttpClientErrorException e) {

            if (e.getStatusCode().value() == 422 && !estRetry) {
                try {
                    String body = e.getResponseBodyAsString();
                    System.out.println("[MINGAT] Corps du 422 : " + body); // ← AJOUTE ÇA

                    JsonNode erreur = mapper.readTree(body);
                    String prochainDepart = null;
                    String prochainRetour = null;

                    for (JsonNode violation : erreur.path("violations")) {
                        String path    = violation.path("propertyPath").asText();
                        String message = violation.path("message").asText();

                        // Cherche directement le pattern de date "YYYY-MM-DD HH:mm:ss" dans le message
                        java.util.regex.Matcher matcher = java.util.regex.Pattern
                            .compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})")
                            .matcher(message);

                        if (matcher.find()) {
                            String creneau = matcher.group(1); // "2026-03-26 14:00:00"
                            System.out.println("[MINGAT] Créneau extrait : " + creneau);
                            if ("departure".equals(path)) prochainDepart = creneau;
                            if ("return".equals(path))    prochainRetour = creneau;
                        }
                    }

                    System.out.println("[MINGAT] prochainDepart=" + prochainDepart + " | prochainRetour=" + prochainRetour); // ← ET ÇA

                    if (prochainDepart != null || prochainRetour != null) {
                        // Si une seule date est bloquée, on garde l'autre inchangée
                        String retryDepart  = prochainDepart  != null 
                            ? prochainDepart.replace(" ", "T")  + ".000Z" 
                            : dateDepart;
                        String retryArrivee = prochainRetour != null 
                            ? prochainRetour.replace(" ", "T") + ".000Z" 
                            : dateArrivee;

                        System.out.println("[MINGAT] Agence fermée, retry avec : " + retryDepart + " → " + retryArrivee);
                        return tenterAppel(agence, type, retryDepart, retryArrivee, true);
                    } else {
                        System.err.println("[MINGAT] Aucun créneau extrait des violations, pas de retry possible.");
                    }

                } catch (Exception parseEx) {
                    System.err.println("[MINGAT] Impossible de parser le 422 : " + parseEx.getMessage());
                    parseEx.printStackTrace(); // ← AJOUTE ÇA aussi
                }
            }

            // 422 au retry, ou autre code erreur → on abandonne proprement
            System.err.println("[MINGAT] Erreur " + e.getStatusCode() + (estRetry ? " (après retry)" : "") 
                + " : " + e.getResponseBodyAsString());
            return null;
        }
    }

    /**
     * Parse le JSON de Mingat pour créer des objets OffreAffichage
     */
    public List<OffreAffichage> parserResultatsMingat(String json, MingatAgence agence) {
        List<OffreAffichage> offres = new ArrayList<>();
        if (json == null || json.isEmpty()) return offres;

        try {
            JsonNode root = mapper.readTree(json);
            // Le JSON est un tableau à la racine
            if (!root.isArray()) {
                System.err.println("[MINGAT] Format inattendu : pas un tableau");
                return offres;
            }

            for (JsonNode item : root) {
                JsonNode category = item.path("vehicleCategory");
                JsonNode prices   = item.path("vehicleCategoryPrices");

                // Pas de prix disponibles → on skip
                if (!prices.isArray() || prices.isEmpty()) continue;

                // On prend le premier prix disponible
                JsonNode firstPrice = prices.get(0);
                JsonNode priceNode  = firstPrice.path("price");

                // Prix en centimes → conversion en euros
                double prix = priceNode.path("priceWithVat").path("amount").asDouble(0) / 10000.0;

                DecimalFormat f = new DecimalFormat();
	            f.setMaximumFractionDigits(2);

                String prixTTC = f.format(prix);

                // Agence réelle (celle qui a le véhicule dispo)
                String nomAgenceDispo = firstPrice.path("agency").path("title").asText(agence.nom);

                // Infos catégorie
                String nomCat   = category.path("title").asText("Véhicule").trim();
                String description = category.path("description").asText("");
                int nbPlaces    = category.path("seatsCount").asInt(5);
                String driveType = category.path("driveType").asText("manual");
                String boite    = driveType.equalsIgnoreCase("automatic") ? "Automatique" : "Manuelle";

                // Carburants
                JsonNode fuelTypes = category.path("fuelTypes");
                String moteur = "Thermique";
                if (fuelTypes.isArray()) {
                    for (JsonNode fuel : fuelTypes) {
                        if (fuel.asText().equalsIgnoreCase("electric")) {
                            moteur = "Électrique";
                            break;
                        }
                    }
                }

                // Image
                String imagePath = category.path("image").path("relativePath").asText("");
                String imageUrl  = imagePath.isEmpty() ? "" : "https://www.mingat.com" + imagePath;

                // Kilométrage inclus
                int kmInclus = priceNode.path("includedKilometers").asInt(0);

                offres.add(new OffreAffichage(
                    nomCat,
                    description,
                    prixTTC,
                    boite,
                    nbPlaces,
                    moteur,
                    imageUrl,
                    true, // clim non fournie par l'API, valeur par défaut
                    2,    // nbBagages non fourni, valeur par défaut
                    String.valueOf(agence.latitude) + "," + String.valueOf(agence.longitude),
                    String.valueOf(agence.latitude) + "," + String.valueOf(agence.longitude),
                    nomAgenceDispo,
                    "Mingat"
                ));
            }

            System.out.println("[MINGAT] " + offres.size() + " offres parsées.");

        } catch (Exception e) {
            System.err.println("[MINGAT] Erreur parsing : " + e.getMessage());
            e.printStackTrace();
        }
        return offres;
    }
}