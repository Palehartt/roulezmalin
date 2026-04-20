package com.roulezmalin.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "adresse_depart", nullable = false)
    private String addresseDepart;

    @Column(name = "adresse_arrivee", nullable = false)
    private String addresseArrivee;

    @Column(name = "date_depart")
    private String dateDepart;

    @Column(name = "date_arrivee")
    private String dateArrivee;

    @Column(name = "nb_offres")
    private int nbOffres;

    @Column(name = "searched_at")
    private LocalDateTime searchedAt = LocalDateTime.now();

    public SearchHistory() {}

    public SearchHistory(User user, String addresseDepart, String addresseArrivee,
                         String dateDepart, String dateArrivee, int nbOffres) {
        this.user = user;
        this.addresseDepart = addresseDepart;
        this.addresseArrivee = addresseArrivee;
        this.dateDepart = dateDepart;
        this.dateArrivee = dateArrivee;
        this.nbOffres = nbOffres;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getAddresseDepart() { return addresseDepart; }
    public String getAddresseArrivee() { return addresseArrivee; }
    public String getDateDepart() { return dateDepart; }
    public String getDateArrivee() { return dateArrivee; }
    public int getNbOffres() { return nbOffres; }
    public LocalDateTime getSearchedAt() { return searchedAt; }
}
