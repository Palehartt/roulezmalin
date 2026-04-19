async function fetchAddresses(query) {
  console.log(`[AUTOCOMPLETE] fetchAddresses() appelé avec : "${query}"`);
  const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}&limit=5&lang=fr&bbox=-5.1,41.3,9.6,51.1`;
  console.log(`[AUTOCOMPLETE] URL construite : ${url}`);

  const response = await fetch(url);
  console.log(`[AUTOCOMPLETE] Réponse HTTP : ${response.status} ${response.statusText}`);

  const data = await response.json();
  console.log(`[AUTOCOMPLETE] ${data.features.length} résultat(s) reçu(s) :`, data.features);

  return data.features;
}

function setupAutocomplete(inputId) {
  console.log(`[AUTOCOMPLETE] Initialisation pour #${inputId}`);

  const input = document.getElementById(inputId);
  if (!input) {
    console.warn(`[AUTOCOMPLETE] ⚠️ Champ #${inputId} introuvable dans le DOM`);
    return;
  }
  console.log(`[AUTOCOMPLETE] Champ #${inputId} trouvé :`, input);

  // Champs cachés pour les coordonnées
  const latInput = document.createElement("input");
  const lonInput = document.createElement("input");
  latInput.type = lonInput.type = "hidden";
  latInput.name = `${inputId}_lat`;
  lonInput.name = `${inputId}_lon`;
  latInput.id = `${inputId}_lat`;
  lonInput.id = `${inputId}_lon`;
  input.parentNode.appendChild(latInput);
  input.parentNode.appendChild(lonInput);
  console.log(`[AUTOCOMPLETE] Champs cachés créés : #${inputId}_lat, #${inputId}_lon`);

  let container = document.createElement("div");
  container.className = "autocomplete-results";
  input.parentNode.appendChild(container);
  console.log(`[AUTOCOMPLETE] Conteneur de suggestions créé pour #${inputId}`);

  let controller, debounceTimer;

  input.addEventListener("input", () => {
    const query = input.value.trim();
    console.log(`[AUTOCOMPLETE] #${inputId} — saisie détectée : "${query}"`);

    container.innerHTML = "";
    latInput.value = lonInput.value = "";
    console.log(`[AUTOCOMPLETE] #${inputId} — coordonnées réinitialisées`);

    if (query.length < 3) {
      console.log(`[AUTOCOMPLETE] #${inputId} — requête trop courte (${query.length} car.), on attend...`);
      return;
    }

    clearTimeout(debounceTimer);
    console.log(`[AUTOCOMPLETE] #${inputId} — debounce lancé (300ms)`);

    debounceTimer = setTimeout(async () => {
      console.log(`[AUTOCOMPLETE] #${inputId} — debounce écoulé, lancement requête pour "${query}"`);

      if (controller) {
        console.log(`[AUTOCOMPLETE] #${inputId} — annulation de la requête précédente`);
        controller.abort();
      }
      controller = new AbortController();

      try {
        const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}&limit=5&lang=fr&bbox=-5.1,41.3,9.6,51.1`;
        console.log(`[AUTOCOMPLETE] #${inputId} — fetch : ${url}`);

        const response = await fetch(url, { signal: controller.signal });
        console.log(`[AUTOCOMPLETE] #${inputId} — réponse HTTP : ${response.status} ${response.statusText}`);

        const data = await response.json();
        console.log(`[AUTOCOMPLETE] #${inputId} — ${data.features.length} résultat(s) :`, data.features);

        if (data.features.length === 0) {
          console.warn(`[AUTOCOMPLETE] #${inputId} — aucun résultat pour "${query}"`);
        }

        data.features.forEach((place, index) => {
          const props = place.properties;
          const [lon, lat] = place.geometry.coordinates;
          const label = [props.name, props.street, props.city, props.postcode]
            .filter(Boolean).join(", ");

          console.log(`[AUTOCOMPLETE] #${inputId} — résultat [${index}] : "${label}" | lat=${lat}, lon=${lon}`);

          const item = document.createElement("div");
          item.className = "autocomplete-item";
          item.textContent = label;

          item.addEventListener("click", () => {
            console.log(`[AUTOCOMPLETE] #${inputId} — sélection : "${label}" | lat=${lat}, lon=${lon}`);
            input.value = label;
            latInput.value = lat;
            lonInput.value = lon;
            console.log(`[AUTOCOMPLETE] #${inputId} — champs cachés mis à jour : lat=${latInput.value}, lon=${lonInput.value}`);
            container.innerHTML = "";
          });

          container.appendChild(item);
        });

      } catch (err) {
        if (err.name === "AbortError") {
          console.log(`[AUTOCOMPLETE] #${inputId} — requête annulée (AbortError), normal`);
        } else {
          console.error(`[AUTOCOMPLETE] #${inputId} — erreur inattendue :`, err);
        }
      }
    }, 300);
  });

  document.addEventListener("click", (e) => {
    if (!container.contains(e.target) && e.target !== input) {
      if (container.innerHTML !== "") {
        console.log(`[AUTOCOMPLETE] #${inputId} — fermeture suggestions (clic extérieur)`);
        container.innerHTML = "";
      }
    }
  });

  console.log(`[AUTOCOMPLETE] #${inputId} — setup terminé ✅`);
}

console.log("[AUTOCOMPLETE] Chargement du script autocomplete.js");
setupAutocomplete("depart");
setupAutocomplete("arrive");
console.log("[AUTOCOMPLETE] Les deux autocompletes sont initialisés");