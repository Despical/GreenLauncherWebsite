// Release notes are rendered as inert DOM nodes; remote/admin text never becomes HTML.
document.querySelectorAll("[data-notes]").forEach((article) => {
    const source = article.dataset.notes;
    const fragment = document.createDocumentFragment();
    let list;
    for (const line of source.split(/\r?\n/)) {
        const text = line.trim();
        if (!text) {
            list = null;
            continue;
        }
        const heading = /^(#{1,6})\s+(.+)$/.exec(text);
        const item = /^[-*+]\s+(.+)$/.exec(text);
        if (heading) {
            list = null;
            const node = document.createElement(`h${Math.min(4, Math.max(2, heading[1].length))}`);
            node.textContent = heading[2];
            fragment.append(node);
        } else if (item) {
            if (!list) {
                list = document.createElement("ul");
                fragment.append(list);
            }
            const node = document.createElement("li");
            node.textContent = item[1];
            list.append(node);
        } else {
            list = null;
            const node = document.createElement("p");
            node.textContent = text;
            fragment.append(node);
        }
    }
    article.replaceChildren(fragment);
});
const search = document.querySelector("#release-search");
search?.addEventListener("input", () => {
    const query = search.value.toLocaleLowerCase().trim();
    let count = 0;
    document.querySelectorAll(".timeline-entry").forEach((entry) => {
        const visible = entry.dataset.search.toLocaleLowerCase().includes(query);
        entry.hidden = !visible;
        count += Number(visible);
    });
    document.querySelector("#search-empty").hidden = count !== 0;
});
document.querySelector(".password-toggle")?.addEventListener("click", (event) => {
    const input = document.querySelector("#password");
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    event.currentTarget.setAttribute("aria-pressed", String(show));
    event.currentTarget.setAttribute("aria-label", show ? "Parolayı gizle" : "Parolayı göster");
});
document.querySelectorAll("[data-confirm]").forEach((form) =>
    form.addEventListener("submit", (event) => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    }),
);
document.querySelectorAll(".upload-form").forEach((form) => {
    form.querySelector('input[type="file"]').addEventListener("change", (event) => {
        const file = event.target.files[0];
        form.querySelector(".upload-selected").textContent = file
            ? `${file.name} · ${(file.size / 1048576).toFixed(1)} MB`
            : "Dosya seçilmedi.";
    });
    form.addEventListener("submit", () => {
        form.querySelector('button[type="submit"]').disabled = true;
        form.querySelector(".upload-progress").hidden = false;
    });
});
document.addEventListener("click", (event) => {
    document.querySelectorAll(".language-menu[open], .mobile-menu[open]").forEach((menu) => {
        if (!menu.contains(event.target)) menu.open = false;
    });
});
document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    document.querySelectorAll(".language-menu[open], .mobile-menu[open]").forEach((menu) => {
        const restoreFocus = menu.contains(document.activeElement);
        menu.open = false;
        if (restoreFocus) menu.querySelector("summary").focus();
    });
});
document.querySelectorAll(".language-menu form").forEach((form) =>
    form.addEventListener("submit", () => {
        // A fragment is browser-only; keep the current section when changing language.
        const target = form.querySelector('[name="returnTo"]');
        if (target && location.hash) target.value = target.value.split("#")[0] + location.hash;
    }),
);

// Shared on public, login, and admin pages.
(() => {
    const button = document.querySelector(".back-to-top");
    if (!button) return;
    const motion = matchMedia("(prefers-reduced-motion: reduce)");
    let pending = false;
    const sync = () => {
        const visible = scrollY > 280;
        button.classList.toggle("is-visible", visible);
        button.setAttribute("aria-hidden", String(!visible));
        button.tabIndex = visible ? 0 : -1;
        pending = false;
    };
    sync();
    window.addEventListener(
        "scroll",
        () => {
            if (!pending) {
                pending = true;
                requestAnimationFrame(sync);
            }
        },
        { passive: true },
    );
    button.addEventListener("click", () => {
        document
            .querySelector(".public-header .brand,.admin-header .brand,.login-brand")
            ?.focus({ preventScroll: true });
        scrollTo({ top: 0, behavior: motion.matches ? "instant" : "smooth" });
    });
})();
