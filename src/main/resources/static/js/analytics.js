(() => {
    const measurementId = document.querySelector('meta[name="google-analytics-id"]')?.content;
    const banner = document.querySelector("[data-analytics-consent]");
    if (!measurementId || !banner) return;

    const cookieName = "greenweb-analytics-v1";
    const readConsent = () =>
        document.cookie
            .split("; ")
            .find((cookie) => cookie.startsWith(`${cookieName}=`))
            ?.split("=")[1];
    let loaded = false;
    let settingsButton = null;

    function enableAnalytics() {
        if (loaded) return;
        loaded = true;
        window[`ga-disable-${measurementId}`] = false;
        window.dataLayer = window.dataLayer || [];
        window.gtag = function () {
            window.dataLayer.push(arguments);
        };
        window.gtag("consent", "default", {
            analytics_storage: "granted",
            ad_storage: "denied",
            ad_user_data: "denied",
            ad_personalization: "denied",
        });
        window.gtag("js", new Date());
        let referrer = "";
        try {
            const url = new URL(document.referrer);
            referrer = `${url.origin}${url.pathname}`;
        } catch {
            /* An empty or invalid referrer has no attribution data. */
        }
        window.gtag("config", measurementId, {
            page_location: `${location.origin}${location.pathname}`,
            page_referrer: referrer,
            allow_google_signals: false,
            allow_ad_personalization_signals: false,
        });
        const tag = document.createElement("script");
        tag.async = true;
        tag.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(measurementId)}`;
        document.head.append(tag);
    }

    function clearAnalyticsCookies() {
        const domains = ["", location.hostname, `.${location.hostname}`];
        const parent = location.hostname.split(".").slice(1).join(".");
        if (parent.includes(".")) domains.push(parent, `.${parent}`);
        for (const cookie of document.cookie.split("; ")) {
            const name = cookie.split("=")[0];
            if (!/^_ga(?:_|$)|^_gid$|^_gat(?:_|$)/.test(name)) continue;
            for (const domain of domains) {
                document.cookie = `${name}=; Max-Age=0; Path=/; Secure; SameSite=Lax${domain ? `; Domain=${domain}` : ""}`;
            }
        }
    }

    function chooseConsent(choice) {
        document.cookie = `${cookieName}=${choice}; Max-Age=15552000; Path=/; Secure; SameSite=Lax`;
        banner.hidden = true;
        if (choice === "granted") enableAnalytics();
        else {
            window[`ga-disable-${measurementId}`] = true;
            if (loaded) window.gtag("consent", "update", { analytics_storage: "denied" });
            clearAnalyticsCookies();
        }
        settingsButton?.focus();
        // Discard any queued measurement and unload the tag after revoking consent.
        if (choice === "denied" && loaded) location.reload();
    }

    document.querySelector("[data-analytics-accept]")?.addEventListener("click", () => chooseConsent("granted"));
    document.querySelector("[data-analytics-reject]")?.addEventListener("click", () => chooseConsent("denied"));
    document.querySelector("[data-analytics-settings]")?.addEventListener("click", (event) => {
        settingsButton = event.currentTarget;
        banner.hidden = false;
        document.querySelector("[data-analytics-reject]")?.focus();
    });
    if (readConsent() === "granted") enableAnalytics();
    else if (readConsent() !== "denied") banner.hidden = false;
})();
