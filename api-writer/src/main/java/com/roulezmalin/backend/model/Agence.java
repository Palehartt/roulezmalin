package com.roulezmalin.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Agence {
    
    @JsonProperty("nom")
    private String nom;
    
    @JsonProperty("latitude")
    private double latitude;
    
    @JsonProperty("longitude")
    private double longitude;
    
    @JsonProperty("pickupDescription")
    private String pickupDescription;

    
    public Agence() {}

    
    public Agence(String nom, double latitude, double longitude, String pickupDescription) {
        this.nom = nom;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pickupDescription = pickupDescription;
    }

    
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPickupDescription() { return pickupDescription; }
    public void setPickupDescription(String pickupDescription) { this.pickupDescription = pickupDescription; }
}