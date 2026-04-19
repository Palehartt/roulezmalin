// frontend/js/filters.js

const DEFAULT_STATE = {
    sort: "asc",        // "asc" | "desc"
    distMax: 100,       // km — distance max entre agence départ et point départ user
    company: "Tous",    // nom de l'entreprise ou "Tous"
    gearbox: "Toutes",  // "Toutes" | "Manuelle" | "Automatique"
};

let state = { ...DEFAULT_STATE };
let allOffres = [];
let userLat = null;
let userLon = null;
let onFilterChange = null; // callback(offresFiltrees)

// ------------------------------------------------------------------
// Initialisation
// ------------------------------------------------------------------
export function initFilters(offres, departLat, departLon, callback) {
    allOffres = offres;
    userLat = departLat;
    userLon = departLon;
    onFilterChange = callback;

    bindEvents();
    applyFilters(); // premier rendu
}

// ------------------------------------------------------------------
// Liaison des événements DOM
// ------------------------------------------------------------------
function bindEvents() {
    // Tri prix
    document.querySelectorAll(".filter-sort").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".filter-sort")
                .forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            state.sort = btn.dataset.sort;
            applyFilters();
        });
    });

    // Slider distance
    const distRange = document.getElementById("dist-range");
    const distLabel = document.getElementById("dist-label");
    distRange.addEventListener("input", () => {
        state.distMax = parseInt(distRange.value);
        distLabel.textContent = state.distMax + " km";
        applyFilters();
    });

    // Chips loueur
    document.querySelectorAll("#company-chips .filter-chip").forEach(chip => {
        chip.addEventListener("click", () => {
            document.querySelectorAll("#company-chips .filter-chip")
                .forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            state.company = chip.dataset.value;
            applyFilters();
        });
    });

    // Chips boîte
    document.querySelectorAll("#gearbox-chips .filter-chip").forEach(chip => {
        chip.addEventListener("click", () => {
            document.querySelectorAll("#gearbox-chips .filter-chip")
                .forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            state.gearbox = chip.dataset.value;
            applyFilters();
        });
    });

    // Réinitialiser
    document.getElementById("reset-filters").addEventListener("click", resetFilters);
}

// ------------------------------------------------------------------
// Application des filtres + tri
// ------------------------------------------------------------------
export function applyFilters() {
    let result = [...allOffres];

    // 1. Filtre loueur
    if (state.company !== "Tous") {
        result = result.filter(o =>
            o.entreprise?.toLowerCase() === state.company.toLowerCase()
        );
    }

    // 2. Filtre boîte de vitesses
    if (state.gearbox !== "Toutes") {
        result = result.filter(o =>
            o.boiteVitesse?.toLowerCase().includes(state.gearbox.toLowerCase())
        );
    }

    // 3. Filtre distance agence de départ
    if (userLat !== null && userLon !== null) {
        result = result.filter(o => {
            const dist = distanceAgenceDepart(o);
            // Si on ne peut pas calculer la distance, on garde l'offre
            return dist === null || dist <= state.distMax;
        });
    }

    // 4. Tri par prix
    result.sort((a, b) => {
        const pa = parseFloat(a.prixTotal) ?? Infinity;
        const pb = parseFloat(b.prixTotal) ?? Infinity;
        return state.sort === "asc" ? pa - pb : pb - pa;
    });

    // Mise à jour du compteur
    const badge = document.getElementById("offers-count");
    if (badge) badge.textContent = result.length + " offre" + (result.length > 1 ? "s" : "");

    // Appel du callback de rendu
    if (onFilterChange) onFilterChange(result);
}

// ------------------------------------------------------------------
// Calcul de distance (Haversine)
// ------------------------------------------------------------------
function distanceAgenceDepart(offre) {
    if (!offre.gpsDemarrage) return null;

    const parts = offre.gpsDemarrage.split(",").map(Number);
    if (parts.length < 2 || isNaN(parts[0]) || isNaN(parts[1])) return null;

    const [lat2, lon2] = parts;
    return haversineKm(userLat, userLon, lat2, lon2);
}

function haversineKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function toRad(deg) { return deg * Math.PI / 180; }

// ------------------------------------------------------------------
// Réinitialisation
// ------------------------------------------------------------------
function resetFilters() {
    state = { ...DEFAULT_STATE };

    document.querySelectorAll(".filter-sort")
        .forEach((b, i) => b.classList.toggle("active", i === 0));

    document.getElementById("dist-range").value = 100;
    document.getElementById("dist-label").textContent = "100 km";

    document.querySelectorAll("#company-chips .filter-chip")
        .forEach((c, i) => c.classList.toggle("active", i === 0));

    document.querySelectorAll("#gearbox-chips .filter-chip")
        .forEach((c, i) => c.classList.toggle("active", i === 0));

    applyFilters();
}