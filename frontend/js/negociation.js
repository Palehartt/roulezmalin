const PHRASES = {

    clientOuverture: [
        "Bonjour, je suis intéressé par ce véhicule. Je vous propose {prix} €, est-ce que cela vous convient ?",
        "Bonjour, votre offre m'intéresse. Seriez-vous prêt à me la céder pour {prix} € ?",
        "Bonjour, je souhaiterais louer ce véhicule. Mon budget est de {prix} €, on peut s'arranger ?",
        "Bonjour, je vous fais une offre directe : {prix} €. Qu'en pensez-vous ?",
        "Bonjour, ce véhicule m'intéresse beaucoup. Je peux vous proposer {prix} € pour cette location.",
        "Bonjour, j'ai consulté plusieurs offres et je suis prêt à mettre {prix} € pour ce véhicule.",
    ],

    clientContreoffre: [
        "Je comprends, mais je ne peux pas aller au-delà de {prix} €. C'est mon maximum.",
        "Votre prix reste trop élevé pour moi. Je maintiens mon offre à {prix} €.",
        "Je fais un effort, mais {prix} € reste ma limite. Pouvez-vous faire un geste ?",
        "Je comprends votre position, mais {prix} € est vraiment ce que je peux mettre.",
        "Je suis sérieux dans ma démarche. {prix} €, c'est une offre honnête.",
        "Permettez-moi d'insister : {prix} €. Je pense que c'est un prix juste.",
    ],

    clientAgressif: [
        "Écoutez, je vais être franc : {prix} €, c'est mon dernier mot.",
        "Je n'irai pas plus haut que {prix} €. Prenez-le ou laissez-le.",
        "J'ai d'autres offres sur la table. {prix} €, c'est tout ce que je peux faire.",
        "Soyons directs : {prix} €. Je ne bougerai pas de là.",
        "{prix} €, c'est mon offre finale. Je n'ai pas de marge de manœuvre.",
        "Je vais aller voir ailleurs si vous ne pouvez pas faire {prix} €.",
    ],

    vendeurContreoffre: [
        "Je comprends votre budget, mais je ne peux pas descendre sous {prix} €. C'est déjà un effort de ma part.",
        "Votre offre est intéressante, mais le mieux que je puisse faire c'est {prix} €.",
        "Je vais faire un effort pour vous : {prix} €. C'est vraiment mon meilleur prix.",
        "Difficile de descendre aussi bas, mais je peux vous proposer {prix} €.",
        "Pour vous aider, je peux aller jusqu'à {prix} €, mais pas en dessous.",
        "Je comprends vos contraintes. Voici ma meilleure offre : {prix} €.",
        "Le véhicule vaut son prix, mais je consens à {prix} € pour conclure.",
        "Je fais un geste commercial : {prix} €. C'est ma limite basse.",
    ],

    vendeurFerme: [
        "Je suis désolé, {prix} € est vraiment mon prix plancher. Je ne peux pas faire mieux.",
        "J'entends votre proposition, mais en dessous de {prix} € ce n'est pas rentable pour moi.",
        "Mon prix minimum est {prix} €. Je ne peux vraiment pas descendre davantage.",
        "Je suis au bout de ce que je peux faire : {prix} €. C'est impossible d'aller plus bas.",
    ],

    vendeurRefusNet: [
        "Je suis désolé, cette offre est trop éloignée de mes attentes. Je ne peux pas accepter.",
        "Votre proposition est bien trop basse, je préfère décliner. Bonne continuation.",
        "Je ne peux pas donner suite à cette offre. L'écart est trop important.",
        "Cette négociation ne peut pas aboutir à ce niveau de prix. Je dois refuser.",
    ],

    vendeurAccord: [
        "C'est une affaire conclue ! Je vous cède le véhicule pour {prix} €.",
        "D'accord, {prix} €. Marché conclu, vous avez bien négocié !",
        "Soit, {prix} €. C'est un prix juste pour les deux parties.",
        "Entendu pour {prix} €. Je suis ravi de conclure avec vous.",
        "Va pour {prix} € ! C'est avec plaisir que je vous fais ce tarif.",
    ],

    clientAccord: [
        "Très bien, {prix} € c'est acceptable, je prends !",
        "D'accord pour {prix} €, marché conclu !",
        "Je suis prêt à aller jusqu'à {prix} €, c'est bon pour moi.",
        "Entendu, {prix} € ça me convient, on fait affaire.",
        "Va pour {prix} €, je ne vais pas faire la fine bouche.",
        "C'est un peu au-dessus de ce que je voulais mais {prix} €, d'accord.",
    ],

    refusFinal: [
        "Nous n'avons malheureusement pas réussi à trouver un terrain d'entente. Bonne continuation.",
        "Après réflexion, nos positions sont trop éloignées pour conclure. Désolé.",
        "Je regrette, mais nous ne pouvons pas nous mettre d'accord sur ce prix.",
        "La négociation n'a pas abouti. N'hésitez pas à revenir si vous changez d'avis.",
    ],
};

// ============================================================
// ÉTAT GLOBAL
// ============================================================

let numeroNegociationGlobal = 0;
let numeroNegociationEchoue = 0;
let negociationsEnCours = [];

// ============================================================
// UTILITAIRES
// ============================================================

function phraseAleatoire(categorie, prix) {
    const liste = PHRASES[categorie];
    const phrase = liste[Math.floor(Math.random() * liste.length)];
    return phrase.replace("{prix}", prix);
}

function delaiAleatoire(min = 1000, max = 3000) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function arrondir(prix) {
    return Math.round(prix);
}

function attendre(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function idFenetre(etat) {
    return "fenetre-nego-" + etat.indexCarte;
}

// ============================================================
// GESTION DES FENÊTRES
// ============================================================

function creerFenetre(etat) {
    const zone = document.getElementById("negociations-reduites");
    const fenetre = document.createElement("div");
    fenetre.className = "nego-fenetre";
    fenetre.id = idFenetre(etat);

    fenetre.innerHTML = `
        <div class="nego-header">
            <span class="nego-titre">${etat.offre.nomVehicule}</span>
            <div class="nego-header-btns">
                <button class="nego-btn-reduire">─</button>
                <button class="nego-btn-fermer">✕</button>
            </div>
        </div>
        <div class="nego-body">
            <div class="nego-messages"></div>
            <div class="nego-typing" style="display:none;">
                <span></span><span></span><span></span>
            </div>
            <div class="nego-footer"></div>
        </div>
    `;

    // Bouton réduire
    fenetre.querySelector(".nego-btn-reduire").addEventListener("click", () => {
        toggleReduire(etat);
    });

    // Bouton fermer
    fenetre.querySelector(".nego-btn-fermer").addEventListener("click", () => {
        fermerFenetre(etat);
    });

    // Clic sur le header pour rouvrir si réduit
    fenetre.querySelector(".nego-header").addEventListener("click", (e) => {
        if (e.target.closest("button")) return;
        if (etat.reduite) toggleReduire(etat);
    });

    zone.appendChild(fenetre);
}

function toggleReduire(etat) {
    const fenetre = document.getElementById(idFenetre(etat));
    if (!fenetre) return;
    etat.reduite = !etat.reduite;
    fenetre.querySelector(".nego-body").style.display = etat.reduite ? "none" : "flex";
    fenetre.querySelector(".nego-btn-reduire").textContent = etat.reduite ? "▲" : "─";
}

function fermerFenetre(etat) {
    const fenetre = document.getElementById(idFenetre(etat));
    if (fenetre) fenetre.remove();
    negociationsEnCours = negociationsEnCours.filter(n => n !== etat);
}

// ============================================================
// AFFICHAGE DANS UNE FENÊTRE
// ============================================================

function afficherBulle(etat, role, texte) {
    const fenetre = document.getElementById(idFenetre(etat));
    if (!fenetre) return;
    const messages = fenetre.querySelector(".nego-messages");
    const bulle = document.createElement("div");
    bulle.className = "bulle bulle-" + role;
    bulle.textContent = texte;
    messages.appendChild(bulle);
    messages.scrollTop = messages.scrollHeight;
}

function afficherTyping(etat, role) {
    const fenetre = document.getElementById(idFenetre(etat));
    if (!fenetre) return;
    const typing = fenetre.querySelector(".nego-typing");
    typing.setAttribute("data-role", role);
    typing.style.display = "flex";
}

function cacherTyping(etat) {
    const fenetre = document.getElementById(idFenetre(etat));
    if (!fenetre) return;
    fenetre.querySelector(".nego-typing").style.display = "none";
}

function afficherFooter(etat, texte, classeTexte) {
    const fenetre = document.getElementById(idFenetre(etat));
    if (!fenetre) return;
    const footer = fenetre.querySelector(".nego-footer");
    footer.innerHTML = "";
    const msg = document.createElement("p");
    msg.className = classeTexte;
    msg.textContent = texte;
    footer.appendChild(msg);
}

function mettreAJourHeader(etat) {
    const fenetre = document.getElementById(idFenetre(etat));
    if (!fenetre) return;
    const titre = fenetre.querySelector(".nego-titre");
    if (etat.resultat === "accord") {
        titre.textContent = "✓ " + etat.offre.nomVehicule;
        fenetre.classList.add("nego-accord");
    } else if (etat.resultat === "echec") {
        titre.textContent = "✗ " + etat.offre.nomVehicule;
        fenetre.classList.add("nego-echec");
    }
}

// ============================================================
// CONCLURE
// ============================================================

function conclureAccord(etat) {
    afficherFooter(etat, "Accord conclu à " + etat.prixVendeur + " € !", "footer-accord");

    etat.terminee = true;
    etat.resultat = "accord";
    mettreAJourHeader(etat);

    const carte = document.querySelectorAll(".offer-card")[etat.indexCarte];
    if (carte) {
        const prix = carte.querySelector(".price");
        if (prix) prix.textContent = etat.prixVendeur + " € — Offre négocié";
    }
}

function conclureEchec(etat, typeRefus) {
    const texte = typeRefus === "refusNet"
        ? "Le vendeur a mis fin à la négociation."
        : "Aucun accord n'a pu être trouvé.";

    afficherFooter(etat, texte, "footer-echec");

    etat.terminee = true;
    etat.resultat = "echec";
    numeroNegociationEchoue++;
    mettreAJourHeader(etat);
}

// ============================================================
// STRATÉGIE
// ============================================================

function getMiroir(numeroNegociation) {
    const centrale = Math.max(1.0 - (numeroNegociation * 0.15), 0.3);
    const bruit = (Math.random() * 0.30) - 0.15;
    return Math.min(Math.max(centrale + bruit, 0.3), 1.1);
}

function getImpatience(roundActuel, roundMax) {
    const progression = roundActuel / roundMax;
    const base = 0.8 + Math.pow(progression, 1.5) * 0.6;
    const bruit = (Math.random() * 0.20) - 0.10;
    return Math.min(Math.max(base + bruit, 0.8), 1);
}

function refusNet(ecartNormalise, roundActuel) {
    if (roundActuel == 1) return 0;
    const facteurTemps = 1 - roundActuel * 0.1;
    const proba = Math.pow(ecartNormalise, 1.2) * facteurTemps;
    const tirage = Math.random();
    return tirage < proba ? 1 : 0;
}

function proposerPrixClient(budget, prixOffre, numeroNegociationEchoue, roundActuel, roundMax, dernierPrixClient, dernierPrixVendeur, avantDernierPrixVendeur) {
    if (roundActuel == 0) {
        const ratio = prixOffre / budget;
        const base = 0.6 + (1 - ratio) * 0.3;
        const coef = base + Math.random() * 0.2 - 0.1;
        return Math.min(arrondir(coef * prixOffre), budget);
    }
    const concession = Math.max(avantDernierPrixVendeur - dernierPrixVendeur, 0);
    let concessionEffective = concession;
    if (concession == 0) concessionEffective = Math.random() * 2;
    const miroir = getMiroir(numeroNegociationEchoue);
    const impatience = getImpatience(roundActuel, roundMax);
    const nouvelle_proposition = dernierPrixClient + concessionEffective * miroir * impatience;
    return Math.min(arrondir(nouvelle_proposition), budget);
}

function proposerPrixVendeur(prixActuelVendeur, prixClient, plancher, roundActuel) {
    const ecartNormalise = (prixActuelVendeur - prixClient) / prixActuelVendeur;
    const refus = refusNet(ecartNormalise, roundActuel);
    if (refus == 1) return null;
    const fraction = Math.random() * (0.3 - 0.1) + 0.1;
    const nouvelleProposition = prixActuelVendeur - (prixActuelVendeur - prixClient) * fraction;
    const nouveauPrix = Math.max(arrondir(nouvelleProposition), plancher);
    const seuil = prixActuelVendeur * 0.05;
    if (Math.abs(nouveauPrix - prixClient) <= seuil) return prixClient;
    return nouveauPrix;
}

// ============================================================
// ORCHESTRATION
// ============================================================

function lancerNegociation(offre, indexCarte, budget) {
    numeroNegociationGlobal++;

    const roundMax = Math.floor(Math.random() * 3) + 4;
    const plancher = arrondir((Math.random() * (0.85 - 0.70) + 0.70) * offre.prixTotal);

    const etat = {
        prixVendeur: offre.prixTotal,
        prixClient: null,
        avantDernierPrixVendeur: null,
        roundActuel: 0,
        roundMax: roundMax,
        plancher: plancher,
        offre: offre,
        indexCarte: indexCarte,
        budget: budget,
        reduite: false,
        terminee: false,
        resultat: null
    };

    negociationsEnCours.push(etat);
    creerFenetre(etat);
    jouerRound(etat);
}

async function jouerRound(etat) {
    etat.roundActuel++;

    // === TOUR CLIENT ===
    afficherTyping(etat, "client");
    await attendre(delaiAleatoire());

    etat.prixClient = proposerPrixClient(
        etat.budget,
        etat.offre.prixTotal,
        numeroNegociationEchoue,
        etat.roundActuel === 1 ? 0 : etat.roundActuel,
        etat.roundMax,
        etat.prixClient,
        etat.prixVendeur,
        etat.avantDernierPrixVendeur
    );

    cacherTyping(etat);

    const categorieClient = etat.roundActuel === 1
        ? "clientOuverture"
        : numeroNegociationEchoue >= 2
            ? "clientAgressif"
            : "clientContreoffre";

    afficherBulle(etat, "client", phraseAleatoire(categorieClient, etat.prixClient));

    // === TOUR VENDEUR ===
    afficherTyping(etat, "vendeur");
    await attendre(delaiAleatoire());

    const nouveauPrixVendeur = proposerPrixVendeur(
        etat.prixVendeur,
        etat.prixClient,
        etat.plancher,
        etat.roundActuel
    );

    cacherTyping(etat);

    if (nouveauPrixVendeur === null) {
        afficherBulle(etat, "vendeur", phraseAleatoire("vendeurRefusNet", etat.prixVendeur));
        conclureEchec(etat, "refusNet");
        return;
    }

    etat.avantDernierPrixVendeur = etat.prixVendeur;
    etat.prixVendeur = nouveauPrixVendeur;

    // === VERIF ACCORD ===
    if (etat.prixClient >= etat.prixVendeur) {
        afficherBulle(etat, "vendeur", phraseAleatoire("vendeurAccord", etat.prixVendeur));
        conclureAccord(etat);
        return;
    }

    // === VERIF ROUNDS EPUISES ===
    if (etat.roundActuel === etat.roundMax) {
        afficherBulle(etat, "vendeur", phraseAleatoire("refusFinal", etat.prixVendeur));
        conclureEchec(etat, "final");
        return;
    }

    const categorieVendeur = etat.prixVendeur <= etat.plancher * 1.05
        ? "vendeurFerme"
        : "vendeurContreoffre";

    afficherBulle(etat, "vendeur", phraseAleatoire(categorieVendeur, etat.prixVendeur));

    // === ROUND SUIVANT ===
    jouerRound(etat);
}

// ============================================================
// EXPORT
// ============================================================

export function ouvrirNegociation(offre, index, budget) {
    lancerNegociation(offre, index, budget);
}