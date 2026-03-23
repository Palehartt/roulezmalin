import { initMap } from "./map.js";
import { drawRouteAnimated } from "./routes.js";

export function initOffers() {
    const map = initMap();
    const offersContainer = document.getElementById("offers");
    const mapContainer = document.getElementById("map-container");

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

        offres.forEach(offre => {
            const card = document.createElement("article");
            card.className = "offer-card";
            card.innerHTML = `
                <div class="offer-header">
                    <h2>${offre.nomVehicule || 'Véhicule'}</h2>
                    <p>${offre.boiteVitesse} · ${offre.nbPlaces} places</p>
                </div>
                <div class="vehicle-image">
                    <img src="https://www.rentacar.fr/assets/images/cars/placeholder.png" alt="Car">
                </div>
                <div class="offer-footer">
                    <span class="price">${offre.prixTotal} €</span>
                    <button class="btn-offer">Voir l'offre</button>
                </div>
            `;
            
            card.addEventListener("click", () => {
                // ... (ton code de sélection de carte et animation map)
                console.log("Offre sélectionnée:", offre.nomVehicule);
            });

            offersContainer.appendChild(card);
        });

    } catch (e) {
        console.error("[ERROR] Echec du parsing des offres:", e);
        offersContainer.innerHTML = "<p>Erreur lors de la lecture des résultats.</p>";
    }
}