// Native details remain usable without scripts, including keyboard navigation.
(() => {
    const motion = matchMedia("(prefers-reduced-motion: reduce)");
    document.querySelectorAll("[data-faq]").forEach((list) => {
        const questions = [...list.querySelectorAll("details")];
        const animations = new WeakMap();
        const expand = (details, expanded) => {
            const start = details.getBoundingClientRect().height;
            animations.get(details)?.cancel();
            details.dataset.expanded = String(expanded);
            if (expanded) details.open = true;
            if (motion.matches || !details.animate) {
                details.open = expanded;
                details.style.height = "";
                return;
            }
            details.style.height = "";
            const style = getComputedStyle(details);
            const borders = parseFloat(style.borderTopWidth) + parseFloat(style.borderBottomWidth);
            const end = expanded
                ? details.getBoundingClientRect().height
                : details.querySelector("summary").getBoundingClientRect().height + borders;
            const animation = details.animate([{ height: `${start}px` }, { height: `${end}px` }], {
                duration: 300,
                easing: "cubic-bezier(.22,1,.36,1)",
            });
            animations.set(details, animation);
            animation.onfinish = () => {
                details.open = expanded;
                details.style.height = "";
                animations.delete(details);
            };
        };
        questions.forEach((details) => {
            details.dataset.expanded = String(details.open);
            details.querySelector("summary").addEventListener("click", (event) => {
                event.preventDefault();
                const expanded = details.dataset.expanded !== "true";
                if (expanded)
                    questions
                        .filter((other) => other !== details && other.dataset.expanded === "true")
                        .forEach((other) => expand(other, false));
                expand(details, expanded);
            });
        });
    });
})();
