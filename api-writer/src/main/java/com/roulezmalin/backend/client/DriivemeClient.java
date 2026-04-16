package com.roulezmalin.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roulezmalin.backend.model.OffreAffichage;
import com.roulezmalin.backend.model.Trajet;
import com.roulezmalin.backend.service.DriivemeGeoService;
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

        String villeDepart = extraireVille(trajet.getAddresseDepart());
        String villeArrivee = extraireVille(trajet.getAddresseArrivee());
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

        // 1. Extraire les URLs de détail depuis POIS_TRANSPORTS
        List<String> detailUrls = extraireDetailUrls(htmlBrut);

        // 2. Parser chaque bloc trajet
        Elements blocks = doc.select("div.block-trajet");
        System.out.println("[DriiveMe] " + blocks.size() + " trajets trouvés");

        for (int i = 0; i < blocks.size(); i++) {
            Element block = blocks.get(i);

            try {
                // Départ
                Element depSpan = block.selectFirst("span.departure span.strong");
                String depart    = depSpan != null ? depSpan.text() : "";
                String departCp  = depSpan != null ? depSpan.attr("title") : "";

                // Arrivée
                Element arrSpan  = block.selectFirst("span.arrival span.strong");
                String arrivee   = arrSpan != null ? arrSpan.text() : "";

                // Dates de disponibilité
                String dateDebut = "", dateFin = "";
                Element availSpan = block.selectFirst("span.availability");
                if (availSpan != null) {
                    String availText = availSpan.text();
                    Matcher m = Pattern.compile("Départ du (.+?) au (.+?)(?:proposée|$)").matcher(availText);
                    if (m.find()) {
                        dateDebut = m.group(1).trim();
                        dateFin   = m.group(2).trim();
                    }
                }

                // Agence
                String agence = "";
                Element agenceSpan = block.selectFirst("span.visible-xs");
                if (agenceSpan != null) {
                    agence = agenceSpan.text().replace("proposée par ", "").trim();
                }

                // Logo agence
                String agenceLogo = "";
                Element cellRenter = block.selectFirst("div.cell-renter img");
                if (cellRenter != null) agenceLogo = cellRenter.attr("src");

                // Catégorie véhicule
                String categorieVehicule = "";
                Element vehiculeInput = block.selectFirst("input[name=filterVehiculeValue]");
                if (vehiculeInput != null) categorieVehicule = vehiculeInput.attr("value");

                // Détail véhicule (depuis alt de l'image)
                String vehiculeDetail = "";
                Element vehiculeImg = block.selectFirst("div.cell-vehicle img");
                if (vehiculeImg != null) vehiculeDetail = vehiculeImg.attr("alt");

                // Options (places, durée, km)
                String nbPlaces = "", dureeLocation = "", kmInclus = "";
                Elements items = block.select("div.cell-option span.item");
                for (Element item : items) {
                    String title = item.attr("title").toLowerCase();
                    Element valEl = item.selectFirst("span.value");
                    if (valEl == null) continue;
                    String val = valEl.text().trim();
                    if (title.contains("place"))  nbPlaces     = val;
                    if (title.contains("heure"))  dureeLocation = val;
                    if (title.contains("km"))     kmInclus     = val;
                }

                // Labels (ex: "+ de 24H")
                List<String> labels = new ArrayList<>();
                for (Element label : block.select("div.labels span.label")) {
                    labels.add(label.text().trim());
                }

                // URL de détail
                String detailUrl = i < detailUrls.size() ? detailUrls.get(i) : "";

                // Construire le nom affiché
                // Format: "Catégorie | Départ → Arrivée"
                String nomVehicule = categorieVehicule.isEmpty() ? vehiculeDetail : categorieVehicule;
                String exempleModele = depart + " → " + arrivee;

                // Prix : toujours 1€ chez DriiveMe
                double prix = 1.0;

                // Créer l'offre en utilisant la classe existante OffreAffichage
                // On stocke les infos DriiveMe dans les champs disponibles
                OffreAffichage offre = new OffreAffichage(
                    nomVehicule,      // nomVehicule  → catégorie (ex: "Berline")
                    exempleModele,    // exempleModele → "Lyon → Paris"
                    prix,             // prixTotal    → 1.0
                    dureeLocation,    // boiteVitesse → durée (ex: "24H") — champ réutilisé
                    parseIntSafe(nbPlaces),  // nbPlaces
                    agence,           // typeMoteur   → agence (ex: "Avis") — champ réutilisé
                    agenceLogo        // imageUrl     → logo agence
                );

                // Afficher dans les logs pour debug
                System.out.printf("[DriiveMe] %s → %s | %s | %s | %s | %s%n",
                    depart, arrivee, dateDebut, agence, categorieVehicule, kmInclus);

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
    private String extraireVille(String adresse) {
        if (adresse == null) return "";
        // Prend le premier segment avant la virgule
        return adresse.split(",")[0].trim();
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