// Adapted from docs/src/components/ImageLightbox.astro: source-to-center motion.
(() => {
    const template = document.querySelector("#image-lightbox-template");
    let active;
    const open = (source, trigger) => {
        if (!template || active || !source.naturalWidth || !source.naturalHeight) return;
        const origin = source.getBoundingClientRect();
        const gutter = innerWidth < 600 ? 24 : 48;
        const scale = Math.min(
            1,
            960 / source.naturalWidth,
            (innerWidth - gutter) / source.naturalWidth,
            (innerHeight * 0.75) / source.naturalHeight,
        );
        const width = source.naturalWidth * scale,
            height = source.naturalHeight * scale;
        const left = (innerWidth - width) / 2,
            top = (innerHeight - height) / 2;
        const dialog = template.content.firstElementChild.cloneNode(true);
        const closeButton = dialog.querySelector("button"),
            caption = dialog.querySelector(".image-lightbox-caption");
        const image = source.cloneNode(true);
        image.removeAttribute("id");
        image.classList.add("image-lightbox-media");
        image.src = source.currentSrc || source.src;
        image.loading = "eager";
        Object.assign(image.style, { width: `${width}px`, height: `${height}px`, left: `${left}px`, top: `${top}px` });
        Object.assign(caption.style, { width: `${width}px`, left: `${left}px`, top: `${top + height + 12}px` });
        caption.textContent = source.alt || "";
        dialog.append(image);
        const previous = {
            overflow: document.body.style.overflow,
            width: document.body.style.width,
            visibility: source.style.visibility,
        };
        document.body.style.width = `${document.body.clientWidth}px`;
        document.body.style.overflow = "hidden";
        source.style.visibility = "hidden";
        document.body.append(dialog);
        dialog.showModal();
        closeButton.focus({ preventScroll: true });
        const transformFor = (bounds) =>
            `translate(${bounds.left - left}px,${bounds.top - top}px) scale(${bounds.width / width},${bounds.height / height})`;
        const reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;
        let animation,
            closing = false,
            finished = false;
        if (!reduced)
            animation = image.animate([{ transform: transformFor(origin) }, { transform: "none" }], {
                duration: 260,
                easing: "cubic-bezier(0.2,0.7,0.2,1)",
            });
        const finish = () => {
            if (finished) return;
            finished = true;
            animation?.cancel();
            dialog.close();
            dialog.remove();
            source.style.visibility = previous.visibility;
            document.body.style.overflow = previous.overflow;
            document.body.style.width = previous.width;
            active = undefined;
            if (trigger.isConnected) trigger.focus({ preventScroll: true });
        };
        const close = (immediate = false) => {
            if (closing && !immediate) return;
            closing = true;
            if (immediate || reduced) return finish();
            const current = image.getBoundingClientRect();
            animation?.cancel();
            animation = image.animate(
                [{ transform: transformFor(current) }, { transform: transformFor(source.getBoundingClientRect()) }],
                { duration: 220, easing: "cubic-bezier(0.4,0,0.2,1)", fill: "forwards" },
            );
            animation.finished.then(finish, finish);
        };
        active = { close };
        closeButton.addEventListener("click", () => close());
        image.addEventListener("click", () => close());
        dialog.addEventListener("click", (event) => {
            if (event.target === dialog) close();
        });
        dialog.addEventListener("cancel", (event) => {
            event.preventDefault();
            close();
        });
    };
    document.addEventListener("click", (event) => {
        if (!(event.target instanceof Element)) return;
        const trigger = event.target.closest("[data-lightbox]");
        const source = trigger?.querySelector("img");
        if (!source) return;
        event.preventDefault();
        open(source, trigger);
    });
    window.addEventListener("resize", () => active?.close(true));
    window.addEventListener("pagehide", () => active?.close(true));
})();
