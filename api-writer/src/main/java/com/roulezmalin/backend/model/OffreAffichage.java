package com.roulezmalin.backend.model;

public class OffreAffichage {
    public String nomVehicule;
    public String exempleModele;
    public double prixTotal;
    public String boiteVitesse;
    public int nbPlaces;
    public String typeMoteur;
    public String imageUrl; 

    public OffreAffichage(String nom, String exemple, double prix, String boite, int places, String moteur, String image) {
        this.nomVehicule = nom;
        this.exempleModele = exemple;
        this.prixTotal = prix;
        this.boiteVitesse = boite;
        this.nbPlaces = places;
        this.typeMoteur = moteur;
        this.imageUrl = image;
    }
}