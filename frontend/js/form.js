import { saveSearch, isLoggedIn } from "./auth.js";

const ip_serv = "localhost";

// ----------------------------------------------------------------
// Retour sur le même lieu de départ
// ----------------------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
    const checkbox = document.getElementById("same-location");
    const startInput = document.getElementById("depart");
    const endInput = document.getElementById("arrive");

    function syncArrivalState() {
        if (checkbox.checked) {
            endInput.value = startInput.value;
            endInput.disabled = true;
        } else {
            endInput.disabled = false;
            endInput.value = "";
        }
    }

    checkbox.addEventListener("change", () => {
        const depLat = document.getElementById("depart_lat");
        const depLon = document.getElementById("depart_lon");
        const arriveeLat = document.getElementById("arrive_lat");
        const arriveeLon = document.getElementById("arrive_lon");
        if (arriveeLat && arriveeLon) {
            arriveeLat.value = depLat.value;
            arriveeLon.value = depLon.value;
        }
        syncArrivalState();
    });

    startInput.addEventListener("input", () => {
        if (checkbox.checked) endInput.value = startInput.value;
    });

    syncArrivalState();
});

// ----------------------------------------------------------------
// Gestion du choix de type de véhicule
// ----------------------------------------------------------------
const vehicleButtons = document.querySelectorAll(".vehicle-btn");
let selectedVehicle = "tourisme";

vehicleButtons.forEach(btn => {
    btn.addEventListener("click", () => {
        vehicleButtons.forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        selectedVehicle = btn.dataset.value;
        const vehicleTypeInput = document.getElementById("vehicle-type");
        if (vehicleTypeInput) vehicleTypeInput.value = btn.dataset.value;
        checkFormValidity();
    });
});

// ----------------------------------------------------------------
// Système de choix de Date (flatpickr)
// ----------------------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
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
            if (selectedDates.length === 2) {
                const [start, end] = selectedDates;
                startInput.value = startPicker.formatDate(start, "d/m/Y H:i");
                endInput.value   = startPicker.formatDate(end,   "d/m/Y H:i");
                endPicker.setDate(end, false);
                checkFormValidity();
            }
        },
        onValueUpdate: function(selectedDates) {
            if (selectedDates.length === 2) {
                const [start, end] = selectedDates;
                startInput.value = startPicker.formatDate(start, "d/m/Y H:i");
                endInput.value   = startPicker.formatDate(end,   "d/m/Y H:i");
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
            }
        },
        onChange: function(selectedDates) {
            if (selectedDates.length > 0) {
                const newEnd = selectedDates[0];
                if (startPicker.selectedDates.length > 0) {
                    const start = startPicker.selectedDates[0];
                    startPicker.setDate([start, newEnd], false);
                    startInput.value = startPicker.formatDate(start,  "d/m/Y H:i");
                    endInput.value   = startPicker.formatDate(newEnd, "d/m/Y H:i");
                    checkFormValidity();
                }
            }
        }
    });
});

// ----------------------------------------------------------------
// Validation du formulaire
// ----------------------------------------------------------------
const inputs = document.querySelectorAll("input[type='text']");
const searchBtn = document.getElementById("search-btn");

function checkFormValidity() {
    let valid = true;
    inputs.forEach(input => { if (!input.value) valid = false; });
    searchBtn.disabled = !valid;
}

inputs.forEach(input => input.addEventListener("input", checkFormValidity));

// ----------------------------------------------------------------
// Envoi du formulaire
// ----------------------------------------------------------------
const COUNTDOWN_SECONDS = 10;

searchBtn.addEventListener("click", (event) => {
    event.preventDefault();

    const trajetData = {
        addresseDepart: document.getElementById("depart").value,
        addresseArrivee: document.getElementById("arrive").value,
        departLat: parseFloat(document.getElementById("depart_lat")?.value) || null,
        departLon: parseFloat(document.getElementById("depart_lon")?.value) || null,
        arriveLat: parseFloat(document.getElementById("arrive_lat")?.value) || null,
        arriveLon: parseFloat(document.getElementById("arrive_lon")?.value) || null,
        dateDepart: document.getElementById("start-date").value,
        dateArrivee: document.getElementById("end-date").value,
        typeVehicule: selectedVehicle,
        identifiant: crypto.randomUUID(),
        budget: parseFloat(document.getElementById("budget").value)
    };

    searchBtn.disabled = true;
    searchBtn.textContent = "Recherche...";
    envoyerRequete(trajetData, searchBtn);
});

function envoyerRequete(trajetData, btn) {
    const url = `http://${ip_serv}:8080/trajet`;

    fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(trajetData)
    })
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    })
    .then(data => {
        const offres = JSON.parse(data.message);
        localStorage.setItem('dernieres_offres', JSON.stringify(data.message));
        localStorage.setItem('budget', trajetData.budget);
        localStorage.setItem('depart_lat', trajetData.departLat);
        localStorage.setItem('depart_lon', trajetData.departLon);

        // ── Sauvegarde dans l'historique si connecté ──
        if (isLoggedIn()) {
            saveSearch({
                addresseDepart: trajetData.addresseDepart,
                addresseArrivee: trajetData.addresseArrivee,
                dateDepart: trajetData.dateDepart,
                dateArrivee: trajetData.dateArrivee,
                nbOffres: Array.isArray(offres) ? offres.length : 0
            });
        }

        window.location.href = "trajet.html";
    })
    .catch(err => {
        console.error("[FORM] Erreur :", err);
        btn.disabled = false;
        btn.textContent = "Rechercher";
    });
}
