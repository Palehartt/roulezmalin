package com.roulezmalin.backend.service;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roulezmalin.backend.model.MessageReponse;
import com.roulezmalin.backend.model.Trajet;

@Service
public class RedisListener implements MessageListener {

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SearchService searchService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonRecu = new String(message.getBody());
            System.out.println("Message : " + jsonRecu);

            MessageReponse reponse = objectMapper.readValue(jsonRecu, MessageReponse.class);

            searchService.traiterReponseRedis(reponse.getRequestId(), reponse);

        } catch (JsonProcessingException e) {
            System.err.println("Erreur de format JSON : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
        }
    }
}
