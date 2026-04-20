// js/auth.js — utilitaire partagé pour l'authentification

const AUTH_API = "http://localhost:8082/auth";

export function getToken() {
    return localStorage.getItem("rm_token");
}

export function getUser() {
    const raw = localStorage.getItem("rm_user");
    return raw ? JSON.parse(raw) : null;
}

export function isLoggedIn() {
    return !!getToken();
}

export function logout() {
    localStorage.removeItem("rm_token");
    localStorage.removeItem("rm_user");
    window.location.href = "login.html";
}

export async function login(email, password) {
    const res = await fetch(`${AUTH_API}/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Erreur de connexion");
    _saveSession(data);
    return data;
}

export async function register(email, password, prenom, nom) {
    const res = await fetch(`${AUTH_API}/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, prenom, nom })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Erreur d'inscription");
    _saveSession(data);
    return data;
}

export async function saveSearch(searchData) {
    const token = getToken();
    if (!token) return;
    try {
        await fetch(`${AUTH_API}/history/search`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(searchData)
        });
    } catch (e) {
        console.warn("[AUTH] Impossible de sauvegarder la recherche :", e);
    }
}

export async function saveNegotiation(negoData) {
    const token = getToken();
    if (!token) return;
    try {
        await fetch(`${AUTH_API}/history/negotiation`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(negoData)
        });
    } catch (e) {
        console.warn("[AUTH] Impossible de sauvegarder la négociation :", e);
    }
}

export async function fetchSearchHistory() {
    const token = getToken();
    if (!token) return [];
    const res = await fetch(`${AUTH_API}/history/search`, {
        headers: { "Authorization": `Bearer ${token}` }
    });
    return res.ok ? res.json() : [];
}

export async function fetchNegotiationHistory() {
    const token = getToken();
    if (!token) return [];
    const res = await fetch(`${AUTH_API}/history/negotiation`, {
        headers: { "Authorization": `Bearer ${token}` }
    });
    return res.ok ? res.json() : [];
}

function _saveSession(data) {
    localStorage.setItem("rm_token", data.token);
    localStorage.setItem("rm_user", JSON.stringify({
        userId: data.userId,
        email: data.email,
        prenom: data.prenom,
        nom: data.nom
    }));
}
