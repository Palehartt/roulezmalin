package com.roulezmalin.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "negotiation_history")
public class NegotiationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "nom_vehicule", nullable = false)
    private String nomVehicule;

    @Column(name = "prix_initial")
    private double prixInitial;

    @Column(name = "prix_final")
    private double prixFinal;

    @Column(name = "resultat", nullable = false)
    private String resultat; // "accord" | "echec"

    @Column(name = "entreprise")
    private String entreprise;

    @Column(name = "negotiated_at")
    private LocalDateTime negotiatedAt = LocalDateTime.now();

    public NegotiationHistory() {}

    public NegotiationHistory(User user, String nomVehicule, double prixInitial,
                               double prixFinal, String resultat, String entreprise) {
        this.user = user;
        this.nomVehicule = nomVehicule;
        this.prixInitial = prixInitial;
        this.prixFinal = prixFinal;
        this.resultat = resultat;
        this.entreprise = entreprise;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getNomVehicule() { return nomVehicule; }
    public double getPrixInitial() { return prixInitial; }
    public double getPrixFinal() { return prixFinal; }
    public String getResultat() { return resultat; }
    public String getEntreprise() { return entreprise; }
    public LocalDateTime getNegotiatedAt() { return negotiatedAt; }
}
