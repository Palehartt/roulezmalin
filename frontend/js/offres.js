import { initMap } from "./map.js";
import { drawRouteAnimated } from "./routes.js";
import { initFilters } from "./filters.js";

export function initOffers(ouvrirNegociation) {
    const map = initMap();
    const budget = parseFloat(localStorage.getItem('budget')) || 0;
    const rawData = localStorage.getItem('dernieres_offres');

    if (!rawData) {
        document.getElementById("offers").innerHTML = "<p>Aucune recherche en cours...</p>";
        return;
    }

    try {
        let data = JSON.parse(rawData);
        let offres = data;
        if (typeof data === 'string') offres = JSON.parse(data);
        else if (data.message) offres = typeof data.message === 'string'
            ? JSON.parse(data.message) : data.message;

        if (!Array.isArray(offres)) {
            document.getElementById("offers").innerHTML = "<p>Erreur dans le format des données.</p>";
            return;
        }

        // Récupérer les coordonnées de départ depuis localStorage
        // (à sauvegarder dans form.js lors de l'envoi — voir étape 4)
        const departLat = parseFloat(localStorage.getItem('depart_lat')) || null;
        const departLon = parseFloat(localStorage.getItem('depart_lon')) || null;

        // Initialiser les filtres — ils appellent renderOffers dès le départ
        initFilters(offres, departLat, departLon, (offresFiltrees) => {
            renderOffers(offresFiltrees, map, ouvrirNegociation, budget);
        });

    } catch (e) {
        console.error("[ERROR] Echec du parsing des offres:", e);
        document.getElementById("offers").innerHTML = "<p>Erreur lors de la lecture des résultats.</p>";
    }
}

// Extraire le rendu dans une fonction autonome
function renderOffers(offres, map, ouvrirNegociation, budget) {
    const offersContainer = document.getElementById("offers");
    const mapContainer = document.getElementById("map-container");
    offersContainer.innerHTML = "";

    if (offres.length === 0) {
        offersContainer.innerHTML = "<p style='text-align:center;color:#6b7280;padding:2rem'>Aucune offre ne correspond à vos critères.</p>";
        return;
    }

    offres.forEach((offre, index) => {

        const card = document.createElement("article");

        // ⭐ Offre recommandée = première
        const isRecommended = index === 0;

        card.className = isRecommended 
            ? "offer-card recommended" 
            : "offer-card";

        // Image dynamique
        const image = /*offre.imageUrl && offre.imageUrl !== ""
            ? offre.imageUrl
            : */getVehicleImage(offre.nomVehicule);

        const logo = getCompagnyImage(offre.entreprise);

        card.innerHTML = `
            <div class="offer-agency-logo">
                <img src="${logo}" alt="${offre.entreprise}">
            </div>

            ${isRecommended ? `
                <div class="offer-extension">
                    Offre recommandée
                </div>
            ` : ""}

            <div class="offer-header">
                <h2>${offre.nomVehicule || 'Véhicule'}</h2>
                <p>
                    ${offre.boiteVitesse || ''} · 
                    ${offre.nbPlaces || '?'} places
                    · ${offre.nbBagages || '?'} bagages 
                    · ${offre.typeMoteur || '?'}
                </p>
            </div>

            <div class="vehicle-image">
                <img src="${image}" alt="${offre.nomVehicule}">
            </div>

            <div class="offer-footer">
                <span class="price">
                    ${offre.prixTotal ? offre.prixTotal + " €" : "-- €"}
                </span>

                <div class="offer-actions">
                    <button class="offer-btn" href="${offre.url}" target="_blank">
                        Voir l’offre
                    </button>
                </div>
            </div>
        `;

        const actionsContainer = card.querySelector(".offer-actions");

        if (offre.prixTotal > 1){
            const btnNegocier = document.createElement("button");
            btnNegocier.className = "negotiate-btn";
            btnNegocier.textContent = "Négocier";
            btnNegocier.addEventListener("click", (e) => {
                console.log("Bouton cliqué")
                e.stopPropagation();

                btnNegocier.disabled = true;
                btnNegocier.classList.add("disabled");

                ouvrirNegociation(offre, index, budget);
            });
            actionsContainer.prepend(btnNegocier);
        }

        // 🎯 Interaction
        card.addEventListener("click", () => {

            document.querySelectorAll(".offer-card")
                .forEach(c => c.classList.remove("selected"));

            card.classList.add("selected");

            mapContainer.classList.add("expanded");
            setTimeout(() => map.invalidateSize(), 500);

            offersContainer.prepend(card);

            mapContainer.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });

            if (offre.gpsArrivee && offre.gpsDemarrage) {
                const routeData = [
                    offre.gpsDemarrage.split(",").map(Number),
                    offre.gpsArrivee.split(",").map(Number)
                ];
                console.log(routeData);
                drawRouteAnimated(routeData);
            }

            console.log("Offre sélectionnée:", offre);
        });

        offersContainer.appendChild(card);
    });
}

function getVehicleImage(type) {

    //console.log(type);

    if (!type) return "assets/images/default.png";

    const t = type.toLowerCase();

    if (t.includes("citadine") || t.includes("standard")) return "assets/citadine.png";
    if (t.includes("suv") || t.includes("confort") || t.includes("interm")) return "assets/suv.png";
    if (t.includes("berline") || t.includes("elite") || t.includes("lux")) return "assets/berline.png";
    if (t.includes("utilitaire")) return "assets/utilitaire.png";
    if (t.includes("affaire") || t.includes("premium") || t.includes("routière")) return "assets/affaire.png";
    if (t.includes("eco") || t.includes("urbain")) return "assets/economique.png";
    if (t.includes("pick-up")) return "assets/pickup.png";
    if (t.includes("van")) return "assets/viano.png";
    if (t.includes("minibus")) return "assets/minibus.png";
    if (t.includes("space") || t.includes("familiale") ||t.includes("compact")) return "assets/espace.png";
    if (t.includes("mini")) return "assets/espace.png";

    return "assets/images/default.png";
}

function getCompagnyImage(type) {

    //console.log(type);

    if (!type) return "assets/images/default.png";

    const t = type.toLowerCase();

    if (t.includes("rentacar")) return "assets/rentacar.png";
    if (t.includes("mingat")) return "assets/mingat.png";
    if (t.includes("driiveme")) return "assets/driiveme.png";
    if (t.includes("europcar")) return "assets/europcar.png";
    if (t.includes("sixt")) return "assets/sixt.png";

    return "assets/images/default.png";
}
