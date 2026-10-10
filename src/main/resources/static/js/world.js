// Progressive enhancement: native details and anchors still work without JavaScript.
(() => {
    if (!document.querySelector(".world-home")) return;
    const motion = matchMedia("(prefers-reduced-motion: reduce)");

    if (!("IntersectionObserver" in window) || motion.matches) return;
    const reveal = new IntersectionObserver(
        (entries) => {
            for (const entry of entries)
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-revealed");
                    reveal.unobserve(entry.target);
                }
        },
        { threshold: 0.04 },
    );
    document
        .querySelectorAll(
            ".section-intro, .home-showcases .feature-showcase, .feature-page-link, .release-card, .home-faq-intro, .final-cta>div, .final-cta>.button",
        )
        .forEach((element) => {
            element.classList.add("reveal-target");
            reveal.observe(element);
        });
    document.body.classList.add("world-motion-ready");
    motion.addEventListener("change", () => {
        if (motion.matches) {
            document.querySelectorAll(".reveal-target").forEach((element) => element.classList.add("is-revealed"));
            reveal.disconnect();
        }
    });
})();
