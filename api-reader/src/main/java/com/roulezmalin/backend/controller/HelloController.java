package com.roulezmalin.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.roulezmalin.backend.model.MessageReponse;
import com.roulezmalin.backend.model.Trajet;

import com.roulezmalin.backend.service.SearchService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;

@CrossOrigin(origins = "*")

@RestController
public class HelloController {

    @Autowired
    private SearchService searchService;

    @PostMapping("/trajet") 
    public CompletableFuture<MessageReponse> nvTrajet(@RequestBody Trajet trajet) {
        System.out.println("[API-READER] Trajet reçu : " + trajet.getAddresseDepart() + " → " + trajet.getAddresseArrivee() + " | Date: " + trajet.getDateDepart());
        
        
        if (trajet.getIdentifiant() == null) {
            trajet.setIdentifiant(UUID.randomUUID().toString());
        }

        return searchService.initiateSearch(trajet);
    }

}
