async function fetchAddresses(query) {
    const url = `https://nominatim.openstreetmap.org/search?` +
        `q=${encodeURIComponent(query)}` +
        `&format=json&addressdetails=1&limit=5&countrycodes=fr`;

    const response = await fetch(url, {
        headers: {
            "Accept": "application/json"
        }
    });

    return await response.json();
}

function setupAutocomplete(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;

    let container = document.createElement("div");
    container.className = "autocomplete-results";
    input.parentNode.appendChild(container);

    let controller;

    input.addEventListener("input", async () => {
        const query = input.value.trim();
        container.innerHTML = "";

        if (query.length < 3) return;

        // Annule la requête précédente
        if (controller) controller.abort();
        controller = new AbortController();

        try {
            const response = await fetch(
                `https://nominatim.openstreetmap.org/search?format=json&limit=5&addressdetails=1&countrycodes=fr&q=${encodeURIComponent(query)}`,
                {
                    signal: controller.signal,
                    headers: {
                        "Accept": "application/json"
                    }
                }
            );

            const results = await response.json();

            results.forEach(place => {
                const item = document.createElement("div");
                item.className = "autocomplete-item";
                item.textContent = place.display_name;

                item.addEventListener("click", () => {
                    input.value = place.display_name;
                    container.innerHTML = "";
                });

                container.appendChild(item);
            });

        } catch (err) {
            if (err.name !== "AbortError") {
                console.error(err);
            }
        }
    });

    document.addEventListener("click", (e) => {
        if (!container.contains(e.target) && e.target !== input) {
            container.innerHTML = "";
        }
    });
}

// Initialisation
setupAutocomplete("depart");
setupAutocomplete("arrive");