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
    const consentBanner = document.querySelector("[data-analytics-consent]");
    const footerBottom = document.querySelector(".public-footer .footer-bottom");
    const motion = matchMedia("(prefers-reduced-motion: reduce)");
    let pending = false;
    const sync = () => {
        // Keep the control above the footer's copyright and trademark line.
        const footerClearance = footerBottom
            ? Math.max(0, innerHeight - footerBottom.getBoundingClientRect().top + 12)
            : 0;
        button.style.setProperty("--footer-clearance", `${footerClearance}px`);
        const visible = scrollY > 280 && (!consentBanner || consentBanner.hidden);
        button.classList.toggle("is-visible", visible);
        button.setAttribute("aria-hidden", String(!visible));
        button.tabIndex = visible ? 0 : -1;
        pending = false;
    };
    sync();
    if (consentBanner) {
        new MutationObserver(sync).observe(consentBanner, { attributes: true, attributeFilter: ["hidden"] });
    }
    const scheduleSync = () => {
        if (!pending) {
            pending = true;
            requestAnimationFrame(sync);
        }
    };
    window.addEventListener("scroll", scheduleSync, { passive: true });
    window.addEventListener("resize", scheduleSync);
    button.addEventListener("click", () => {
        document
            .querySelector(".public-header .brand,.admin-header .brand,.login-brand")
            ?.focus({ preventScroll: true });
        scrollTo({ top: 0, behavior: motion.matches ? "instant" : "smooth" });
    });
})();
