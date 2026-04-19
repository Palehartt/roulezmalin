import { initMap } from "./map.js";
import { drawRouteAnimated } from "./routes.js";

function getVehicleImage(type) {

    //console.log(type);

    if (!type) return "assets/images/default.png";

    const t = type.toLowerCase();

    if (t.includes("citadine")) return "assets/citadine.png";
    if (t.includes("suv") || t.includes("confort")) return "assets/suv.png";
    if (t.includes("berline")) return "assets/berline.png";
    if (t.includes("utilitaire")) return "assets/utilitaire.png";
    if (t.includes("affaire") || t.includes("premium") || t.includes("routière")) return "assets/affaire.png";
    if (t.includes("economique") || t.includes("urbain")) return "assets/economique.png";
    if (t.includes("pick-up")) return "assets/pickup.png";
    if (t.includes("van")) return "assets/viano.png";
    if (t.includes("minibus")) return "assets/minibus.png";
    if (t.includes("space") || t.includes("familiale")) return "assets/espace.png";

    return "assets/images/default.png";
}

function getCompagnyImage(type) {

    //console.log(type);

    if (!type) return "assets/images/default.png";

    const t = type.toLowerCase();

    if (t.includes("rentacar")) return "assets/rentacar.png";
    if (t.includes("mingat")) return "assets/mingat.png";
    if (t.includes("driiveme")) return "assets/driiveme.png";
    if (t.includes("utilitaire")) return "assets/utilitaire.png";

    return "assets/images/default.png";
}

export function initOffers(ouvrirNegociation) {
    const map = initMap();
    const offersContainer = document.getElementById("offers");
    const mapContainer = document.getElementById("map-container");

    const budget = parseFloat(localStorage.getItem('budget')) || 0;
    const rawData = localStorage.getItem('dernieres_offres');
    console.log("[DEBUG] Données brutes localStorage:", rawData);

    if (!rawData) {
        offersContainer.innerHTML = "<p>Aucune recherche en cours...</p>";
        return;
    }

    try {
        // 1. On parse le premier niveau (l'objet MessageReponse ou le tableau)
        let data = JSON.parse(rawData);

        // 2. Si ton Writer a envoyé le JSON des offres dans le champ "message" 
        // de MessageReponse, il faut parser ce champ spécifique.
        let offres = data;
        if (typeof data === 'string') {
            offres = JSON.parse(data);
        } else if (data.message) {
            // Si c'est l'objet MessageReponse, on prend le contenu du message
            offres = typeof data.message === 'string' ? JSON.parse(data.message) : data.message;
        }

        // 3. Sécurité : on vérifie que c'est bien un tableau avant le forEach
        if (!Array.isArray(offres)) {
            console.error("[ERROR] Les offres ne sont pas un tableau:", offres);
            offersContainer.innerHTML = "<p>Erreur dans le format des données reçues.</p>";
            return;
        }

        offersContainer.innerHTML = ""; 

        console.log(offres);

        offres.sort((a, b) => {
            const priceA = a.prixTotal ?? Infinity;
            const priceB = b.prixTotal ?? Infinity;
            return priceA - priceB;
        });

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

                    <button class="offer-btn">
                        Voir l’offre
                    </button>
                </div>
            `;

            if (offre.prixTotal > 1){
                const btnNegocier = document.createElement("button");
                btnNegocier.className = "btn-negocier";
                btnNegocier.textContent = "Négocier";
                btnNegocier.addEventListener("click", (e) => {
                    console.log("Bouton cliqué")
                    e.stopPropagation();
                    ouvrirNegociation(offre, index, budget);
                });
                card.appendChild(btnNegocier);
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

        } catch (e) {
            console.error("[ERROR] Echec du parsing des offres:", e);
            offersContainer.innerHTML = "<p>Erreur lors de la lecture des résultats.</p>";
        }
}