const ADMIN_NAV_ITEMS = [
    { href: "index.html", label: "Novo registro" },
    { href: "relatorio.html", label: "Relatório" },
    { href: "importacao.html", label: "Importar Alunos" }
];

function renderizarNavAdmin() {
    const containers = document.querySelectorAll("[data-admin-nav]");
    if (!containers.length) return;

    const paginaAtual = window.location.pathname.split("/").pop() || "index.html";

    containers.forEach((nav) => {
        nav.innerHTML = "";
        ADMIN_NAV_ITEMS.forEach((item) => {
            const link = document.createElement("a");
            const ativo = paginaAtual === item.href;
            link.href = item.href;
            link.textContent = item.label;
            link.className = ativo ? "link-admin link-admin--ativo" : "link-admin";
            if (ativo) {
                link.setAttribute("aria-current", "page");
            }
            nav.appendChild(link);
        });
    });
}

renderizarNavAdmin();
