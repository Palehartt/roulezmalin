package com.roulezmalin.backend.service;

import java.util.concurrent.*;

import com.roulezmalin.backend.model.*;

import java.util.UUID;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
public class SearchService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Map<String, CompletableFuture<MessageReponse>> casiers = new ConcurrentHashMap<>();

    public void demarrer() {
        System.out.println("Le Reader est prêt avec 10 threads.");
    }

    public CompletableFuture<MessageReponse> initiateSearch(Trajet trajet) {
        String requestId = trajet.getIdentifiant();
        
        
        CompletableFuture<MessageReponse> promise = new CompletableFuture<MessageReponse>()
            .orTimeout(20, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                casiers.remove(requestId);
                System.err.println("Timeout pour " + requestId);
                return new MessageReponse("Service temporairement indisponible", requestId, 504);
            });
        
        casiers.put(requestId, promise);

        
        System.out.println("Envoi de la requête " + requestId + " pour : " + trajet.getAddresseArrivee());
        
        executor.submit(() -> {
            publierTrajet(trajet);
        });

        System.out.println("Promesse retournée : " + promise);

        return promise;
    }

    public void publierTrajet(Trajet trajet) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String trajetString = objectMapper.writeValueAsString(trajet);

            redisTemplate.convertAndSend("demandes-trajets", trajetString);

            System.out.println("Message envoyé à Redis ! 🚀");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi : " + e.getMessage());
        }
    }

    public void traiterReponseRedis(String requestId, MessageReponse rep) {

        System.err.println("[DEBUG] : Traite la réponse : " + requestId);
        
        CompletableFuture<MessageReponse> promise = casiers.get(requestId);

        if (promise != null) {
            promise.complete(rep);

            casiers.remove(requestId);
        }
    }
}