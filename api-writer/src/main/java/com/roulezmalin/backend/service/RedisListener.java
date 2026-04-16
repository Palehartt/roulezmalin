package com.roulezmalin.backend.service;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roulezmalin.backend.model.MessageReponse;
import com.roulezmalin.backend.model.OffreAffichage;
import com.roulezmalin.backend.model.Trajet;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.roulezmalin.backend.client.*;

@Service
public class RedisListener implements MessageListener {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RentacarClient rentacarClient;

    @Autowired
    private MingatClient mingatClient;

    @Autowired
    private DriivemeClient driivemeClient;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonRecu = new String(message.getBody());
            System.out.println("[DEBUG] Message reçu : " + jsonRecu);
            Trajet trajet = objectMapper.readValue(jsonRecu, Trajet.class);
            System.out.println("[DEBUG] Trajet reçu : " + trajet.getIdentifiant());

            
            CompletableFuture<List<OffreAffichage>> futureRentacar = CompletableFuture.supplyAsync(() -> {
                try {
                    String json = rentacarClient.fetchDisponibilites(trajet);
                    return rentacarClient.parserResultatsRentacar(json);
                } catch (Exception e) {
                    System.err.println("[RENTACAR] Erreur : " + e.getMessage());
                    return List.of(); 
                }
            });

            CompletableFuture<List<OffreAffichage>> futureMingat = CompletableFuture.supplyAsync(() -> {
                try {
                    String json = mingatClient.fetchDisponibilites(trajet);
                    return mingatClient.parserResultatsMingat(json);
                } catch (Exception e) {
                    System.err.println("[MINGAT] Erreur : " + e.getMessage());
                    return List.of();
                }
            });

            CompletableFuture<List<OffreAffichage>> futureDriiveme = CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("[DriiveMe] Lancement de la requête pour le trajet : " + trajet.getAddresseDepart() + " → " + trajet.getAddresseArrivee());
                    String htmlDriiveme = driivemeClient.fetchResultsPage(trajet);
                    return driivemeClient.parserResultatsDriiveme(htmlDriiveme);
                } catch (Exception e) {
                    System.err.println("[DriiveMe] Echec : " + e.getMessage());
                    return List.of();
                }
            });

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureRentacar, futureMingat, futureDriiveme);

            List<OffreAffichage> toutesLesOffres = allFutures.thenApply(v -> {
                List<OffreAffichage> fusion = new ArrayList<>();
                fusion.addAll(futureRentacar.join()); 
                fusion.addAll(futureMingat.join());
                fusion.addAll(futureDriiveme.join());
                return fusion;
            }).get(15, TimeUnit.SECONDS);
                        

            String offresEnJson = objectMapper.writeValueAsString(toutesLesOffres);
            MessageReponse reponse = new MessageReponse(offresEnJson, trajet.getIdentifiant(), 200);
            redisTemplate.convertAndSend("reponses-trajets", objectMapper.writeValueAsString(reponse));

            System.out.println("[DEBUG] " + toutesLesOffres.size() + " offres envoyées pour " + trajet.getIdentifiant());

        } catch (Exception e) {
            System.err.println("[ERROR] Erreur critique : " + e.getMessage());
            e.printStackTrace();
        }
    }
}