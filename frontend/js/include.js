// Import des outils d'authentification
import { isLoggedIn, getUser } from "./auth.js";

function includeHTML(id, file) {
    fetch(file)
        .then(response => response.text())
        .then(data => {
            document.getElementById(id).innerHTML = data;

            // Logique spécifique au MENU
            if (id === "menu") {
                // 1. Gérer l'état actif des liens
                if (typeof currentPage !== "undefined") {
                    const links = document.querySelectorAll(".nav-links a");
                    links.forEach(link => {
                        if (link.dataset.page === currentPage) {
                            link.classList.add("active");
                        }
                    });
                }
                
                // 2. Gérer l'affichage Connexion / Mon Compte
                updateAuthDisplay();
            }
        });
}

function updateAuthDisplay() {
    const navAuth = document.getElementById("nav-auth");
    if (!navAuth) return;

    if (isLoggedIn()) {
        const user = getUser();
        navAuth.innerHTML = `
            <a href="compte.html" class="nav-auth-btn nav-btn-compte">Mon compte</a>
        `;
    } else {
        navAuth.innerHTML = `
            <a href="login.html" class="nav-auth-btn nav-btn-login">Se connecter</a>
        `;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    includeHTML("menu", "components/menu.html");
    includeHTML("footer", "components/footer.html");
});