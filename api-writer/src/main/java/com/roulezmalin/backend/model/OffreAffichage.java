package com.roulezmalin.backend.model;

public class OffreAffichage {
    public String entreprise;
    public String nomVehicule;
    public String exempleModele;
    public String prixTotal;
    public String boiteVitesse;
    public int nbPlaces;
    public String typeMoteur;
    public String imageUrl;
    public boolean clim;
    public int nbBagages;
    public String gpsDemarrage;
    public String gpsArrivee;
    public String nomAgence;
    public String url;

    public OffreAffichage(String nom, String exemple, String prix, String boite, int places, 
                          String moteur, String image, boolean clim, int nbBagages, 
                          String gpsDemarrage, String gpsArrivee, String nomAgence, String entreprise,
                          String url) {
        this.nomVehicule = nom;
        this.exempleModele = exemple;
        this.prixTotal = prix;
        this.boiteVitesse = boite;
        this.nbPlaces = places;
        this.typeMoteur = moteur;
        this.imageUrl = image;
        this.clim = clim;
        this.nbBagages = nbBagages;
        this.gpsDemarrage = gpsDemarrage;
        this.gpsArrivee = gpsArrivee;
        this.nomAgence = nomAgence;
        this.entreprise = entreprise;
        this.url = url;
    }
}