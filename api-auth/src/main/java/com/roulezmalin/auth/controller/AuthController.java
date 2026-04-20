package com.roulezmalin.auth.controller;

import com.roulezmalin.auth.dto.*;
import com.roulezmalin.auth.model.*;
import com.roulezmalin.auth.security.JwtService;
import com.roulezmalin.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;

    // ── Inscription ──────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.ok(authService.register(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Connexion ────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Profil utilisateur ───────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            User user = authService.getUser(userId);
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "prenom", user.getPrenom(),
                "nom", user.getNom(),
                "createdAt", user.getCreatedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Token invalide."));
        }
    }

    // ── Sauvegarder une recherche ────────────────────────────────
    @PostMapping("/history/search")
    public ResponseEntity<?> saveSearch(@RequestHeader("Authorization") String authHeader,
                                         @RequestBody SaveSearchRequest req) {
        try {
            Long userId = extractUserId(authHeader);
            authService.saveSearch(userId, req);
            return ResponseEntity.ok(Map.of("message", "Recherche sauvegardée."));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Non autorisé."));
        }
    }

    // ── Historique des recherches ────────────────────────────────
    @GetMapping("/history/search")
    public ResponseEntity<?> getSearchHistory(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            List<SearchHistory> list = authService.getSearchHistory(userId);
            return ResponseEntity.ok(list.stream().map(s -> Map.of(
                "id", s.getId(),
                "addresseDepart", s.getAddresseDepart(),
                "addresseArrivee", s.getAddresseArrivee(),
                "dateDepart", s.getDateDepart() != null ? s.getDateDepart() : "",
                "dateArrivee", s.getDateArrivee() != null ? s.getDateArrivee() : "",
                "nbOffres", s.getNbOffres(),
                "searchedAt", s.getSearchedAt().toString()
            )).toList());
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Non autorisé."));
        }
    }

    // ── Sauvegarder une négociation ──────────────────────────────
    @PostMapping("/history/negotiation")
    public ResponseEntity<?> saveNegotiation(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody SaveNegotiationRequest req) {
        try {
            Long userId = extractUserId(authHeader);
            authService.saveNegotiation(userId, req);
            return ResponseEntity.ok(Map.of("message", "Négociation sauvegardée."));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Non autorisé."));
        }
    }

    // ── Historique des négociations ──────────────────────────────
    @GetMapping("/history/negotiation")
    public ResponseEntity<?> getNegotiationHistory(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            List<NegotiationHistory> list = authService.getNegotiationHistory(userId);
            return ResponseEntity.ok(list.stream().map(n -> Map.of(
                "id", n.getId(),
                "nomVehicule", n.getNomVehicule(),
                "prixInitial", n.getPrixInitial(),
                "prixFinal", n.getPrixFinal(),
                "resultat", n.getResultat(),
                "entreprise", n.getEntreprise() != null ? n.getEntreprise() : "",
                "negotiatedAt", n.getNegotiatedAt().toString()
            )).toList());
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Non autorisé."));
        }
    }

    // ── Utilitaire ───────────────────────────────────────────────
    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
