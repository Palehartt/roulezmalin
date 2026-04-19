// assets/js/routes.js

import { getMap, setRouteLayer, getRouteLayer } from "./map.js";

export function drawRouteAnimated(latLngs) {
    const map = getMap();
    if (!map || !latLngs?.length) return;

    // Supprime l'ancien tracé
    if (getRouteLayer()) {
        map.removeLayer(getRouteLayer());
    }

    // Crée une polyline animée avec Leaflet.Motion
    const motionPolyline = L.motion.polyline(latLngs, {
        color: "#2563eb",
        weight: 5,
        opacity: 0.8
    }, {
        duration: latLngs.length * 100, // 100ms par point
        easing: L.Motion.Ease.linear
    }).addTo(map);

    setRouteLayer(motionPolyline);

    // Lance l'animation
    motionPolyline.motionStart();

    // Fit bounds
    map.fitBounds(latLngs);
}

/*export function drawRouteAnimated(latLngs) {

    const polyline = L.polyline([], {
        color: "#2563eb",
        weight: 5
    }).addTo(map);

    setRouteLayer(polyline);

    let i = 0;

    function animate() {
        if (i < latLngs.length) {
            polyline.addLatLng(latLngs[i]);
            i++;
            requestAnimationFrame(animate);
        }
    }

    animate();

    map.fitBounds(latLngs);
}*/