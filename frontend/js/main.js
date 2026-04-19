import { initOffers } from "./offres.js";
import { ouvrirNegociation } from "./negociation.js";

document.addEventListener("DOMContentLoaded", () => {
    initOffers(ouvrirNegociation);
});