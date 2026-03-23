// Fonction pour créer une carte d'offre HTML à partir d'un objet offre
export function createOfferCard(offre) {
    const article = document.createElement('article');
    article.className = 'offer-card';
    
    // On peut ajouter des données pour la carte si besoin
    // article.dataset.route = ... 

    article.innerHTML = `
        <div class="offer-header">
            <h2>${offre.nomVehicule}</h2>
            <p>${offre.boiteVitesse} · ${offre.nbPlaces} places · ${offre.typeMoteur}</p>
            <small>${offre.exempleModele}</small>
        </div>

        <div class="vehicle-image">
            <img src="https://www.rentacar.fr/assets/images/cars/placeholder.png" alt="${offre.nomVehicule}">
        </div>

        <div class="offer-footer">
            <span class="price">${offre.prixTotal.toFixed(2)} €</span>
            <button class="btn-offer">
                <img src="https://www.rentacar.fr/favicon.ico" style="width:16px; margin-right:5px;">
                Voir l’offre
            </button>
        </div>
    `;

    return article;
}

// Fonction pour afficher toutes les offres
export function renderOffers(offres) {
    const container = document.getElementById('offers');
    container.innerHTML = ""; // On vide le message "chargement"

    if (offres.length === 0) {
        container.innerHTML = "<p>Aucune offre trouvée pour ce trajet.</p>";
        return;
    }

    offres.forEach(offre => {
        const card = createOfferCard(offre);
        container.appendChild(card);
    });
}