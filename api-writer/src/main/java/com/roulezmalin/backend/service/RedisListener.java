package com.roulezmalin.backend.service;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roulezmalin.backend.model.MessageReponse;
import com.roulezmalin.backend.model.OffreAffichage;
import com.roulezmalin.backend.model.Trajet;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.roulezmalin.backend.client.RentacarClient;

@Service
public class RedisListener implements MessageListener {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RentacarClient rentacarClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 1. Réception du message Redis
            String jsonRecu = new String(message.getBody());
            System.out.println("\n[DEBUG] 📥 Message reçu de Redis: " + jsonRecu);

            Trajet trajet = objectMapper.readValue(jsonRecu, Trajet.class);
            System.out.println("[DEBUG] 🚗 Trajet décodé : ID=" + trajet.getIdentifiant() + 
                               " | De: " + trajet.getAddresseDepart() + 
                               " | A: " + trajet.getAddresseArrivee());

            // 2. Appel de l'API Rentacar
            System.out.println("[DEBUG] 🌐 Appel de l'API Rentacar en cours...");
            String jsonBrutApi = rentacarClient.fetchDisponibilites(trajet);
            
            // Debug de la taille de la réponse (pour éviter de saturer les logs si c'est énorme)
            if (jsonBrutApi != null) {
                System.out.println("[DEBUG] ✅ Réponse API reçue (Taille: " + jsonBrutApi.length() + " caractères)");
            } else {
                System.out.println("[DEBUG] ⚠️ L'API a renvoyé une réponse vide !");
            }

            // 3. Parsing des résultats (CORRECTION : on parse jsonBrutApi, pas jsonRecu)
            List<OffreAffichage> offres = rentacarClient.parserResultatsRentacar(jsonBrutApi);
            System.out.println("[DEBUG] 📦 Nombre d'offres extraites : " + (offres != null ? offres.size() : 0));

            // 4. Envoi de la réponse
            String offresEnJson = objectMapper.writeValueAsString(offres);
            MessageReponse reponse = new MessageReponse(offresEnJson, trajet.getIdentifiant(), 200);
            String jsonFinal = objectMapper.writeValueAsString(reponse);

            System.out.println("[DEBUG] 📤 Envoi au canal 'reponses-trajets'...");
            redisTemplate.convertAndSend("reponses-trajets", jsonFinal);
            
            System.out.println("[DEBUG] ✨ Terminé avec succès pour l'ID : " + trajet.getIdentifiant() + "\n");

        } catch (JsonProcessingException e) {
            System.err.println("[ERROR] 🔥 Erreur de format JSON : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] ❌ Erreur critique : " + e.getMessage());
            e.printStackTrace(); // Affiche la pile d'erreur complète dans les logs Docker
        }
    }
}