package com.roulezmalin.backend.service;

import com.roulezmalin.backend.model.Agence;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeoService {

    private final List<Agence> agencesRentacar = new ArrayList<>();

    public GeoService() {
        
        agencesRentacar.add(new Agence("Lyon Dardilly", 45.804335, 4.766333, "LYON - DARDILLY (TECHLID)"));
        agencesRentacar.add(new Agence("Lyon Part-Dieu", 45.7606, 4.8592, "LYON - GARE PART DIEU"));
        
    }

    public Agence trouverPlusProche(double userLat, double userLon) {
        Agence plusProche = null;
        double distanceMin = Double.MAX_VALUE;

        for (Agence agence : agencesRentacar) {
            double dist = calculerDistance(userLat, userLon, agence.getLatitude(), agence.getLongitude());
            if (dist < distanceMin) {
                distanceMin = dist;
                plusProche = agence;
            }
        }
        return plusProche;
    }

    
    private double calculerDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}