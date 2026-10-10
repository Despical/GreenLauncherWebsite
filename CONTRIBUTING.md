# Contributing

Discuss substantial changes in an issue before starting. Follow the
[Code of Conduct](CODE_OF_CONDUCT.md) in every project interaction.

## Development

1. Install JDK 25 and clone the repository.
2. Run `./gradlew bootRun` for the local H2-backed application.
3. Keep changes focused. Use four-space indentation, LF and braces for Java control flow.
4. Run `./gradlew clean test bootJar` before opening a pull request.
5. Describe the problem, resulting behavior and verification. Include screenshots for UI changes.

Edit the six `src/main/resources/messages*.properties` bundles directly when
changing visitor copy. Public pages support Turkish, English, German, French,
Russian and Polish. Imported release notes retain their original language.

Add new Flyway migrations after `V1__releases.sql`; never rewrite an applied migration.
Never commit passwords, tokens, database files, uploaded releases, IDE settings,
local screenshot provenance or authoring and QA helper directories.
