package com.roulezmalin.backend.model;

public class Trajet {
    private String addresseDepart;
    private String addresseArrivee;

    private String departLat;
    private String departLon;
    private String arriveLat;
    private String arriveLon;
    
    private String dateDepart;
    private String dateArrivee;
    private String identifiant;

    
    public Trajet() {}

    public Trajet(String identifiant, String addresseDepart, String addresseArrivee, 
                        String departLat,
                        String departLon,
                        String arriveLat,
                        String arriveLon,
                        String dateDepart, String dateArrivee) {
        this.identifiant = identifiant;
        this.addresseDepart = addresseDepart;
        this.addresseArrivee = addresseArrivee;
        this.dateDepart = dateDepart;
        this.dateArrivee = dateArrivee;
        this.departLat = departLat;
        this.departLon = departLon;
        this.arriveLat = arriveLat;
        this.arriveLon = arriveLon;
    }

    public void setAddresseDepart(String addresseDepart) { this.addresseDepart = addresseDepart; }
    public void setAddresseArrivee(String addresseArrivee) { this.addresseArrivee = addresseArrivee; }

    public void setDepartLat(String departLat) { this.departLat = departLat; }
    public void setDepartLon(String departLon) { this.departLon = departLon; }
    public void setArriveLat(String arriveLat) { this.arriveLat = arriveLat; }
    public void setArriveLon(String arriveLon) { this.arriveLon = arriveLon; }

    public void setDateDepart(String dateDepart) { this.dateDepart = dateDepart; }
    public void setDateArrivee(String dateArrivee) { this.dateArrivee = dateArrivee; }

    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getAddresseDepart() { return addresseDepart; }
    public String getAddresseArrivee() { return addresseArrivee; }
    

    public String getDepartLat(){ return departLat; }
    public String getDepartLon(){ return departLon; }
    public String getArriveLat(){ return arriveLat; }
    public String getArriveLon(){ return arriveLon; }

    public String getDateDepart() { return dateDepart; }
    public String getDateArrivee() { return dateArrivee; }

    public String getIdentifiant() { return identifiant; }
}