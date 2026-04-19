package com.roulezmalin.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roulezmalin.backend.model.OffreAffichage;
import com.roulezmalin.backend.model.Trajet;
import com.roulezmalin.backend.service.DriivemeGeoService;
import com.roulezmalin.backend.service.GeocodingService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DriivemeClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DriivemeGeoService driivemeGeoService;

    @Autowired
    private GeocodingService geocodingService;

    /**
     * Récupère la page HTML de résultats DriiveMe et retourne le HTML brut.
     */
    public String fetchResultsPage(Trajet trajet) {
        // DriiveMe accepte des requêtes POST sur sa page de recherche
        // Les paramètres de ville sont passés comme form-data
        // NOTE : DriiveMe nécessite une recherche par nom de ville, pas coordonnées.
        // Pour simplifier, on passe les adresses directement.

        String url = "https://www.driiveme.com/fr-FR/rechercher-trajet.html";

        // Extraire la ville du départ (ex: "Lyon, France" → "Lyon")
        System.out.println("[DriiveMe] Préparation requête pour trajet : " + trajet.getAddresseDepart() + " → " + trajet.getAddresseArrivee());

        String villeDepart = extraireVille(Double.parseDouble(trajet.getDepartLat()), Double.parseDouble(trajet.getDepartLon()));
        String villeArrivee = extraireVille(Double.parseDouble(trajet.getArriveLat()), Double.parseDouble(trajet.getArriveLon()));
        String dateDepart  = formaterDatePourDriiveme(trajet.getDateDepart());
        Integer departId = driivemeGeoService.getCityId(villeDepart);
        Integer arriveeId = driivemeGeoService.getCityId(villeArrivee);

        System.out.printf("[DriiveMe] Recherche : %s (%d) → %s (%d) | Date: %s%n",
            villeDepart, departId, villeArrivee, arriveeId, dateDepart);

        String body = "alert_quick_form%5BdepartureCityName%5D=" + encode(villeDepart) +
              "&alert_quick_form%5BdepartureCity%5D=" + departId +
              "&alert_quick_form%5BarrivalCityName%5D=" + encode(villeArrivee) +
              "&alert_quick_form%5BarrivalCity%5D=" + arriveeId +
              "&alert_quick_form%5BminDate%5D=" + encode(dateDepart) +
              "&alert_quick_form%5BvehicleType%5D=0";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept", "text/html,application/xhtml+xml");
        headers.set("Origin", "https://www.driiveme.com");
        headers.set("Referer", "https://www.driiveme.com/fr-FR/rechercher-trajet.html");

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        System.out.println("[DriiveMe] Envoi requête POST : " + entity);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            // Si redirection 302, on suit l'URL indiquée dans le Location header
            if (response.getStatusCode().value() == 302) {
                String redirectUrl = response.getHeaders().getLocation().toString();
                
                // Si l'URL est relative, on ajoute le domaine
                if (redirectUrl.startsWith("/")) {
                    redirectUrl = "https://www.driiveme.com" + redirectUrl;
                }
                
                System.out.println("[DriiveMe] Redirection vers : " + redirectUrl);
                
                HttpHeaders getHeaders = new HttpHeaders();
                getHeaders.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36");
                getHeaders.set("Accept", "text/html,application/xhtml+xml");
                getHeaders.set("Referer", "https://www.driiveme.com/fr-FR/rechercher-trajet.html");
                
                HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders);
                ResponseEntity<String> finalResponse = restTemplate.exchange(redirectUrl, HttpMethod.GET, getEntity, String.class);
                return finalResponse.getBody();
            }
            
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[DriiveMe] Erreur HTTP : " + e.getMessage());
            throw e;
        }
    }

    /**
     * Parse le HTML retourné par DriiveMe et retourne une liste d'offres.
     */
    public List<OffreAffichage> parserResultatsDriiveme(String htmlBrut) {
        List<OffreAffichage> offres = new ArrayList<>();

        if (htmlBrut == null || htmlBrut.isEmpty()) return offres;

        Document doc = Jsoup.parse(htmlBrut);
        List<String> detailUrls = extraireDetailUrls(htmlBrut);

        Elements blocks = doc.select("div.block-trajet");
        System.out.println("[DriiveMe] " + blocks.size() + " trajets trouvés");

        for (int i = 0; i < blocks.size(); i++) {
            Element block = blocks.get(i);

            try {
                // 1. Extraction des lieux pour le GPS/Modèle
                Element depSpan = block.selectFirst("span.departure span.strong");
                String departNom = depSpan != null ? depSpan.text() : "Inconnu";
                
                Element arrSpan = block.selectFirst("span.arrival span.strong");
                String arriveeNom = arrSpan != null ? arrSpan.text() : "Inconnu";

                // 2. Agence
                String agence = "DriiveMe";
                Element agenceSpan = block.selectFirst("span.visible-xs");
                if (agenceSpan != null) {
                    agence = agenceSpan.text().replace("proposée par ", "").trim();
                }

                // 3. Véhicule et Image
                String categorieVehicule = "";
                Element vehiculeInput = block.selectFirst("input[name=filterVehiculeValue]");
                if (vehiculeInput != null) categorieVehicule = vehiculeInput.attr("value");

                String vehiculeDetail = "";
                String photoVehicule = "";
                Element vehiculeImg = block.selectFirst("div.cell-vehicle img");
                if (vehiculeImg != null) {
                    vehiculeDetail = vehiculeImg.attr("alt");
                    photoVehicule = vehiculeImg.attr("src");
                }

                // 4. Caractéristiques (Places, Boite, Clim)
                String nbPlacesStr = "5";
                String boite = "Manuelle"; // Par défaut sur DriiveMe sauf si précisé
                boolean clim = true; // Généralement standard, mais non précisé dans le block
                
                Elements items = block.select("div.cell-option span.item");
                for (Element item : items) {
                    String title = item.attr("title").toLowerCase();
                    String val = item.select("span.value").text().trim();
                    if (title.contains("place")) nbPlacesStr = val;
                    if (title.contains("automatique")) boite = "Automatique";
                }

                nbPlacesStr = nbPlacesStr.split(" ")[0];

                // 5. Coordonnées GPS (DriiveMe ne les donne pas en clair dans le HTML block)
                // On met les noms des villes par défaut ou "0,0" si non trouvé
                double[] coordsDep = geocodingService.getCoordinates(departNom);
                double[] coordsArr = geocodingService.getCoordinates(arriveeNom);

                String gpsStart = String.valueOf(coordsDep[0])+","+String.valueOf(coordsDep[1]);
                String gpsEnd = String.valueOf(coordsArr[0])+","+String.valueOf(coordsArr[1]);

                // 6. Création de l'offre avec le nouveau constructeur
                OffreAffichage offre = new OffreAffichage(
                    categorieVehicule.isEmpty() ? "Véhicule" : categorieVehicule, // nomVehicule
                    vehiculeDetail.isEmpty() ? departNom + " -> " + arriveeNom : vehiculeDetail, // exempleModele
                    "1.0",                           // prixTotal (Toujours 1€)
                    boite,                         // boiteVitesse
                    Integer.parseInt(nbPlacesStr),      // nbPlaces
                    "Essence/Gasoil",              // typeMoteur (Indéterminé sur la liste)
                    photoVehicule,                 // imageUrl
                    clim,                          // clim
                    1,                             // nbBagages (Par défaut 1)
                    gpsStart,                      // gpsDemarrage
                    gpsEnd,                        // gpsArrivee
                    agence,                         // nomAgence
                    "DriiveMe",
                    detailUrls.size() > i ? detailUrls.get(i) : null // url
                );

                offres.add(offre);

            } catch (Exception e) {
                System.err.println("[DriiveMe] Erreur parsing bloc " + i + " : " + e.getMessage());
            }
        }
        return offres;
    }
    
    // ===== Méthodes utilitaires =====

    /**
     * Extrait les URLs de détail depuis le bloc JS POIS_TRANSPORTS dans le HTML.
     */
    private List<String> extraireDetailUrls(String html) {
        List<String> urls = new ArrayList<>();
        Pattern poiPattern = Pattern.compile("POIS_TRANSPORTS = (\\[.*?\\]);", Pattern.DOTALL);
        Matcher m = poiPattern.matcher(html);

        if (m.find()) {
            try {
                JsonNode pois = objectMapper.readTree(m.group(1));
                Pattern hrefPattern = Pattern.compile("href=\"(/fr-FR/trajet/[^\"]+)\"");

                for (JsonNode poi : pois) {
                    String tooltip = poi.path("a").path("tooltip").asText();
                    Matcher hm = hrefPattern.matcher(tooltip);
                    if (hm.find()) {
                        urls.add("https://www.driiveme.com" + hm.group(1));
                    } else {
                        urls.add(null);
                    }
                }
            } catch (Exception e) {
                System.err.println("[DriiveMe] Erreur extraction POIS : " + e.getMessage());
            }
        }
        return urls;
    }

    /**
     * Extrait le nom de ville depuis une adresse complète.
     * Ex: "Lyon, Rhône-Alpes, France" → "Lyon"
     */
    private String extraireVille(double lat, double lon) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?lat=" + lat 
                    + "&lon=" + lon 
                    + "&format=json&zoom=10";

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

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response.getBody());

            // Nominatim retourne address.city, address.town ou address.village selon la taille
            com.fasterxml.jackson.databind.JsonNode address = root.path("address");
            
            if (!address.path("city").isMissingNode())   return address.path("city").asText();
            if (!address.path("town").isMissingNode())   return address.path("town").asText();
            if (!address.path("village").isMissingNode()) return address.path("village").asText();

        } catch (Exception e) {
            System.err.println("[DriiveMe] Erreur reverse geocoding : " + e.getMessage());
        }
        return "";
    }

    /**
     * Convertit "dd/MM/yyyy HH:mm" → "dd/MM/yyyy" (format attendu par DriiveMe)
     */
    private String formaterDatePourDriiveme(String dateRecue) {
        if (dateRecue == null) return "";
        // DriiveMe attend juste la date, pas l'heure
        return dateRecue.split(" ")[0];
    }

    private String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}