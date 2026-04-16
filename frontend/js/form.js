const ip_serv = "localhost";

//Retour sur le même lieu de départ
document.addEventListener("DOMContentLoaded", () => {
    const checkbox = document.getElementById("same-location");
    const startInput = document.getElementById("depart");
    const endInput = document.getElementById("arrive");
    const autoCompletion = document.querySelectorAll("#autocomplete-results")

    function syncArrivalState() {
        if (checkbox.checked) {
            endInput.value = startInput.value;
            endInput.disabled = true;
        } else {
            endInput.disabled = false;
            endInput.value = "";
        }
    }

    checkbox.addEventListener("change", syncArrivalState);

    startInput.addEventListener("input", () => {
        if (checkbox.checked) {
            endInput.value = startInput.value;
        }
    });

    syncArrivalState();
});

//Gestion du choix de type de véhicule (Tourisme par défaut)
const vehicleButtons = document.querySelectorAll(".vehicle-btn");
let selectedVehicle = "tourisme";

vehicleButtons.forEach(btn => {
    btn.addEventListener("click", () => {
        vehicleButtons.forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        selectedVehicle = btn.dataset.value;
        checkFormValidity();
    });
});

document.querySelectorAll(".vehicle-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".vehicle-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        document.getElementById("vehicle-type").value = btn.dataset.value;
    });
});

//Système de choix de Date de Départ et Retour
document.addEventListener("DOMContentLoaded", () => {

    const startInput = document.getElementById("start-date");
    const endInput = document.getElementById("end-date");

    // Instance Départ (avec range)
    const startPicker = flatpickr(startInput, {
        mode: "range",
        enableTime: true,
        dateFormat: "d/m/Y H:i",
        time_24hr: true,
        locale: "fr",
        position: "below",
        minDate: "today",

        onChange: function(selectedDates) {

            // Quand les deux dates sont choisies
            if (selectedDates.length === 2) {

                const [start, end] = selectedDates;

                // On remplit manuellement les deux champs
                startInput.value = startPicker.formatDate(start, "d/m/Y H:i");
                endInput.value = startPicker.formatDate(end, "d/m/Y H:i");

                // Met à jour le picker retour
                endPicker.setDate(end, false);
            }
        },
        onValueUpdate: function(selectedDates) {

            if (selectedDates.length === 2) {

                const [start, end] = selectedDates;

                startInput.value = startPicker.formatDate(start, "d/m/Y H:i");
                endInput.value = startPicker.formatDate(end, "d/m/Y H:i");
            }
        }
    });

    // Instance Retour (simple date)
    const endPicker = flatpickr(endInput, {
        enableTime: true,
        dateFormat: "d/m/Y H:i",
        time_24hr: true,
        locale: "fr",
        minDate: "today",

        onOpen: function() {
            // Empêche retour < départ
            if (startPicker.selectedDates.length > 0) {
                this.set("minDate", startPicker.selectedDates[0]);
            }
        },

        onChange: function(selectedDates) {

            if (selectedDates.length > 0) {

                const newEnd = selectedDates[0];

                // Si départ existe → on met à jour la plage du startPicker
                if (startPicker.selectedDates.length > 0) {
                    const start = startPicker.selectedDates[0];
                    startPicker.setDate([start, newEnd], false);

                    startInput.value = startPicker.formatDate(start, "d/m/Y H:i");
                    endInput.value = startPicker.formatDate(newEnd, "d/m/Y H:i");
                }
            }
        }
    });

});




//Bloquage du bouton chercher tant que toute les informations ne sont pas insérer.
const inputs = document.querySelectorAll(
    "input[type='text'], input[type='datetime-local']"
);
const searchBtn = document.getElementById("search-btn");

function checkFormValidity() {
    let valid = true;

    inputs.forEach(input => {
        if (!input.value) valid = false;
    });

    searchBtn.disabled = !valid;
}

inputs.forEach(input => {
    input.addEventListener("input", checkFormValidity);
});

searchBtn.addEventListener("click", (event) => {
    event.preventDefault(); // On empêche le rechargement de la page

    // 1. Préparation des données
    const trajetData = {
        addresseDepart: document.getElementById("depart").value,
        addresseArrivee: document.getElementById("arrive").value,
        dateDepart: document.getElementById("start-date").value,
        dateArrivee: document.getElementById("end-date").value,
        typeVehicule: selectedVehicle, // Ta variable globale
        identifiant: crypto.randomUUID() // Génère un ID unique côté client
    };

    const payload = JSON.stringify(trajetData);

    console.log("Envoi de la recherche :", payload);

    fetch("http://" + ip_serv + ":8080/trajet", { // Remplace 8080 par ton port API
        method: "POST",
        headers: {
            "Content-Type": "application/json" // On prévient qu'on envoie du JSON
        },
        body: JSON.stringify(trajetData) // On transforme l'objet en texte
    })
    .then(response => response.json())
    .then(data => {
        localStorage.setItem('dernieres_offres', JSON.stringify(data.message));
        window.location.href = "trajet.html"; // Redirection
    })});
