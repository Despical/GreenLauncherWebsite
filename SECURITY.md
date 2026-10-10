# Security Policy

Only the latest revision of the default branch is supported.

## Reporting a vulnerability

Report vulnerabilities privately to **contact@despical.dev** or use GitHub's
private vulnerability reporting. Do not disclose security issues in public issues.

Include the affected commit, impact, reproduction steps and a minimal proof of
concept. Remove credentials, cookies, repository tokens, uploaded binaries and
personal information from attachments. Allow time for a fix before disclosure.

Security-sensitive areas include administrator authentication, CSRF, sessions,
release publication, upload validation, path handling, download authorization,
update manifests and reverse proxy configuration.

Production requires a BCrypt administrator password hash. Local passwords,
databases, release uploads and deployment environment files must never be committed.
