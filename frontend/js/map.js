/*document.addEventListener("DOMContentLoaded", () => {

    const map = L.map("map").setView([48.8566, 2.3522], 6);

    // L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    //     attribution: "© OpenStreetMap"
    // }).addTo(map);

    L.tileLayer("https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}{r}.png", {
    attribution: "© Stadia Maps"
    }).addTo(map);


    window.map = map;
    window.routeLayer = null;

});*/

// assets/js/map.js

//export const map = L.map("map").setView([48.8566, 2.3522], 6);

// L.tileLayer(
//     "https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}{r}.png",
//     {
//         attribution: "© Stadia Maps"
//     }
// ).addTo(map);

// let currentRouteLayer = null;

let map = null;
let routeLayer = null;

export function initMap() {
    if (!map) {
        map = L.map("map").setView([48.8566, 2.3522], 6);

        L.tileLayer("https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}{r}.png", {
            attribution: "© Stadia Maps"
        }).addTo(map);
    }
    return map;
}

export function getMap() {
    return map;
}

export function getRouteLayer() {
    return routeLayer;
}

export function clearRoute() {
    if (currentRouteLayer) {
        map.removeLayer(currentRouteLayer);
        currentRouteLayer = null;
    }
}

/* export function setRouteLayer(layer) {
    clearRoute();
    currentRouteLayer = layer;
} */

export function setRouteLayer(layer) {
    if (routeLayer) map.removeLayer(routeLayer);
    routeLayer = layer;
}