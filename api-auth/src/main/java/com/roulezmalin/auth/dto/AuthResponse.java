package com.roulezmalin.auth.dto;

public class AuthResponse {
    public String token;
    public Long userId;
    public String email;
    public String prenom;
    public String nom;

    public AuthResponse(String token, Long userId, String email, String prenom, String nom) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.prenom = prenom;
        this.nom = nom;
    }
}
