function includeHTML(id, file) {
    fetch(file)
        .then(response => response.text())
        .then(data => {
            document.getElementById(id).innerHTML = data;

            if (id === "menu" && typeof currentPage !== "undefined") {
                const links = document.querySelectorAll(".nav-links a");
                links.forEach(link => {
                    if (link.dataset.page === currentPage) {
                        link.classList.add("active");
                    }
                });
            }
        });
}

document.addEventListener("DOMContentLoaded", () => {
    includeHTML("menu", "components/menu.html");
    includeHTML("footer", "components/footer.html");
});
