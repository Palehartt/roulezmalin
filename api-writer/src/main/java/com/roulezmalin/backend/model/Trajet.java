package com.roulezmalin.backend.model;

public class Trajet {
    private String addresseDepart;
    private String addresseArrivee;
    private String dateDepart;
    private String dateArrivee;
    private String identifiant;
    private String vehiculeType;

    
    public Trajet() {}

    public Trajet(String identifiant, String addresseDepart, String addresseArrivee, String dateDepart, String dateArrivee, String vehiculeType) {
        this.identifiant = identifiant;
        this.addresseDepart = addresseDepart;
        this.addresseArrivee = addresseArrivee;
        this.dateDepart = dateDepart;
        this.dateArrivee = dateArrivee;
        this.vehiculeType = vehiculeType;
    }

    
    public String getDateArrivee() { return dateArrivee; }
    public void setDateArrivee(String dateArrivee) { this.dateArrivee = dateArrivee; }
    public void setAddresseDepart(String addresseDepart) { this.addresseDepart = addresseDepart; }
    public void setAddresseArrivee(String addresseArrivee) { this.addresseArrivee = addresseArrivee; }
    public void setDateDepart(String dateDepart) { this.dateDepart = dateDepart; }
    public String getIdentifiant() { return identifiant; }
    public String getAddresseDepart() { return addresseDepart; }
    public String getAddresseArrivee() { return addresseArrivee; }
    public String getDateDepart() { return dateDepart; }
    public String getTypeVehicule() { return vehiculeType; }
}