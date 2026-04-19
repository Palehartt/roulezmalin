const ip_serv = "localhost";

// ----------------------------------------------------------------
// Retour sur le même lieu de départ
// ----------------------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
    console.log("[FORM] DOM chargé — initialisation same-location");

    const checkbox = document.getElementById("same-location");
    const startInput = document.getElementById("depart");
    const endInput = document.getElementById("arrive");

    function syncArrivalState() {
        if (checkbox.checked) {
            endInput.value = startInput.value;
            endInput.disabled = true;
            console.log("[FORM] same-location activé — arrivée synchronisée avec départ :", startInput.value);
        } else {
            endInput.disabled = false;
            endInput.value = "";
            console.log("[FORM] same-location désactivé — champ arrivée libéré");
        }
    }

    checkbox.addEventListener("change", () => {
        console.log("[FORM] Checkbox same-location changée :", checkbox.checked);
        const depLat = document.getElementById("depart_lat");
        const depLon = document.getElementById("depart_lon");
        const arriveeLat = document.getElementById("arrive_lat");
        const arriveeLon = document.getElementById("arrive_lon");
        arriveeLat.value = depLat.value;
        arriveeLon.value = depLon.value;
        syncArrivalState();
    });

    startInput.addEventListener("input", () => {
        if (checkbox.checked) {
            endInput.value = startInput.value;
            console.log("[FORM] same-location ON — arrivée mise à jour :", endInput.value);
        }
    });

    syncArrivalState();
    console.log("[FORM] same-location initialisé ✅");
});

// ----------------------------------------------------------------
// Gestion du choix de type de véhicule
// ----------------------------------------------------------------
const vehicleButtons = document.querySelectorAll(".vehicle-btn");
let selectedVehicle = "tourisme";
console.log("[FORM] Véhicule par défaut :", selectedVehicle);

vehicleButtons.forEach(btn => {
    btn.addEventListener("click", () => {
        vehicleButtons.forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        selectedVehicle = btn.dataset.value;
        console.log("[FORM] Véhicule sélectionné :", selectedVehicle);
        checkFormValidity();
    });
});

document.querySelectorAll(".vehicle-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".vehicle-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        const vehicleTypeInput = document.getElementById("vehicle-type");
        if (vehicleTypeInput) vehicleTypeInput.value = btn.dataset.value;
        console.log("[FORM] vehicle-type input mis à jour :", btn.dataset.value);
    });
});

// ----------------------------------------------------------------
// Système de choix de Date
// ----------------------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
    console.log("[FORM] Initialisation flatpickr");

    const startInput = document.getElementById("start-date");
    const endInput = document.getElementById("end-date");

    const startPicker = flatpickr(startInput, {
        mode: "range",
        enableTime: true,
        dateFormat: "d/m/Y H:i",
        time_24hr: true,
        locale: "fr",
        position: "below",
        minDate: "today",

        onChange: function(selectedDates) {
            console.log("[FLATPICKR] onChange départ — dates sélectionnées :", selectedDates);
            if (selectedDates.length === 2) {
                const [start, end] = selectedDates;
                startInput.value = startPicker.formatDate(start, "d/m/Y H:i");
                endInput.value   = startPicker.formatDate(end,   "d/m/Y H:i");
                endPicker.setDate(end, false);
                console.log("[FLATPICKR] Plage complète — départ :", startInput.value, "| retour :", endInput.value);
                checkFormValidity();
            }
        },

        onValueUpdate: function(selectedDates) {
            if (selectedDates.length === 2) {
                const [start, end] = selectedDates;
                startInput.value = startPicker.formatDate(start, "d/m/Y H:i");
                endInput.value   = startPicker.formatDate(end,   "d/m/Y H:i");
                console.log("[FLATPICKR] onValueUpdate — départ :", startInput.value, "| retour :", endInput.value);
                checkFormValidity();
            }
        }
    });

    const endPicker = flatpickr(endInput, {
        enableTime: true,
        dateFormat: "d/m/Y H:i",
        time_24hr: true,
        locale: "fr",
        position: "below",
        minDate: "today",

        onOpen: function() {
            if (startPicker.selectedDates.length > 0) {
                this.set("minDate", startPicker.selectedDates[0]);
                console.log("[FLATPICKR] Retour ouvert — minDate fixée à :", startPicker.selectedDates[0]);
            }
        },

        onChange: function(selectedDates) {
            console.log("[FLATPICKR] onChange retour :", selectedDates);
            if (selectedDates.length > 0) {
                const newEnd = selectedDates[0];
                if (startPicker.selectedDates.length > 0) {
                    const start = startPicker.selectedDates[0];
                    startPicker.setDate([start, newEnd], false);
                    startInput.value = startPicker.formatDate(start,  "d/m/Y H:i");
                    endInput.value   = startPicker.formatDate(newEnd, "d/m/Y H:i");
                    console.log("[FLATPICKR] Retour modifié — départ :", startInput.value, "| retour :", endInput.value);
                    checkFormValidity();
                }
            }
        }
    });

    console.log("[FORM] Flatpickr initialisé ✅");
});

// ----------------------------------------------------------------
// Validation du formulaire
// ----------------------------------------------------------------
const inputs = document.querySelectorAll("input[type='text'], input[type='datetime-local']");

const searchBtn = document.getElementById("search-btn");

function checkFormValidity() {
    let valid = true;
    inputs.forEach(input => {
        if (!input.value) {
            valid = false;
            console.log("[FORM] Champ vide détecté :", input.id || input.name);
        }
    });
    searchBtn.disabled = !valid;
    console.log("[FORM] Formulaire valide :", valid);
}

inputs.forEach(input => {
    input.addEventListener("input", checkFormValidity);
});

// ----------------------------------------------------------------
// Envoi du formulaire avec countdown 10s
// ----------------------------------------------------------------
const COUNTDOWN_SECONDS = 10;

searchBtn.addEventListener("click", (event) => {
    event.preventDefault();
    console.log("[FORM] Bouton Rechercher cliqué");

    const trajetData = {
        addresseDepart: document.getElementById("depart").value,
        addresseArrivee: document.getElementById("arrive").value,
        departLat: parseFloat(document.getElementById("depart_lat")?.value) || null,
        departLon: parseFloat(document.getElementById("depart_lon")?.value) || null,
        arriveLat: parseFloat(document.getElementById("arrive_lat")?.value) || null,
        arriveLon: parseFloat(document.getElementById("arrive_lon")?.value) || null,
        dateDepart: document.getElementById("start-date").value,
        dateArrivee: document.getElementById("end-date").value,
        typeVehicule: selectedVehicle, // Ta variable globale
        identifiant: crypto.randomUUID(), // Génère un ID unique côté client
        budget: parseFloat(document.getElementById("budget").value)
    };

    console.log("[FORM] Données préparées :", trajetData);
    console.log("[FORM] Coordonnées départ :", trajetData.departLat, trajetData.departLon);
    console.log("[FORM] Coordonnées arrivée :", trajetData.arriveLat, trajetData.arriveLon);

    // Countdown 10 secondes avant envoi
    searchBtn.disabled = true;
    let remaining = COUNTDOWN_SECONDS;
    const originalLabel = searchBtn.textContent;

    searchBtn.textContent = "Recherche en cours...";
    envoyerRequete(trajetData, searchBtn, originalLabel);
});

function envoyerRequete(trajetData, btn, originalLabel) {
    const url = `http://${ip_serv}:8080/trajet`;
    console.log("[FORM] Envoi POST vers :", url);
    console.log("[FORM] Payload :", JSON.stringify(trajetData));

    fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(trajetData)
    })
    .then(response => {
        console.log("[FORM] Réponse HTTP reçue :", response.status, response.statusText);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status} — ${response.statusText}`);
        }
        return response.json();
    })
    .then(data => {
        console.log("[FORM] Réponse JSON :", data);
        console.log("[FORM] Sauvegarde dans localStorage clé 'dernieres_offres'");
        localStorage.setItem('dernieres_offres', JSON.stringify(data.message));
        localStorage.setItem('budget', trajetData.budget);
        console.log("[FORM] Redirection vers trajet.html");
        window.location.href = "trajet.html";
    })
    .catch(err => {
        console.error("[FORM] ❌ Erreur lors de la requête :", err);
        btn.disabled = false;
        btn.textContent = originalLabel;
        console.log("[FORM] Bouton réactivé après erreur");
    });
}
