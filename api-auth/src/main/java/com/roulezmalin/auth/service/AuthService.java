package com.roulezmalin.auth.service;

import com.roulezmalin.auth.dto.*;
import com.roulezmalin.auth.model.*;
import com.roulezmalin.auth.repository.*;
import com.roulezmalin.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    @Autowired private UserRepository userRepo;
    @Autowired private SearchHistoryRepository searchRepo;
    @Autowired private NegotiationHistoryRepository negoRepo;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email)) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }
        User user = new User(req.email, passwordEncoder.encode(req.password), req.prenom, req.nom);
        userRepo.save(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getPrenom(), user.getNom());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect."));
        if (!passwordEncoder.matches(req.password, user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect.");
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getPrenom(), user.getNom());
    }

    public User getUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));
    }

    public void saveSearch(Long userId, SaveSearchRequest req) {
        User user = getUser(userId);
        SearchHistory history = new SearchHistory(
            user, req.addresseDepart, req.addresseArrivee,
            req.dateDepart, req.dateArrivee, req.nbOffres
        );
        searchRepo.save(history);
    }

    public List<SearchHistory> getSearchHistory(Long userId) {
        return searchRepo.findByUserIdOrderBySearchedAtDesc(userId);
    }

    public void saveNegotiation(Long userId, SaveNegotiationRequest req) {
        User user = getUser(userId);
        NegotiationHistory history = new NegotiationHistory(
            user, req.nomVehicule, req.prixInitial,
            req.prixFinal, req.resultat, req.entreprise
        );
        negoRepo.save(history);
    }

    public List<NegotiationHistory> getNegotiationHistory(Long userId) {
        return negoRepo.findByUserIdOrderByNegotiatedAtDesc(userId);
    }
}
