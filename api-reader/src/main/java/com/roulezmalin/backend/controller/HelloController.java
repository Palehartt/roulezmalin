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

@CrossOrigin(origins = "http://localhost:5500")

@RestController
public class HelloController {

    @Autowired
    private SearchService searchService;

    @PostMapping("/trajet") // On passe en POST car on envoie des données
    public CompletableFuture<MessageReponse> nvTrajet(@RequestBody Trajet trajet) {
        // Plus besoin de créer l'objet manuellement, Spring le fait pour toi !
        
        // Si ton objet Trajet n'a pas encore d'ID à ce stade :
        if (trajet.getIdentifiant() == null) {
            trajet.setIdentifiant(UUID.randomUUID().toString());
        }

        return searchService.initiateSearch(trajet);
    }

}
