# Prompt Reference

This artifact records the stacks, topic glossaries, and architecture sources that every subsequent stage must load before proposing code or doc changes.

## Selected stacks
- **Language:** Java 25 LTS (`rules/generative/language/java/java-25.rules.md`).
- **Build:** Maven (`rules/generative/language/java/build-tooling.md`).
- **Fluent API:** CRTP (`rules/generative/backend/fluent-api/crtp.rules.md`).
- **Runtime:** GuicedEE Core + Client (`rules/generative/backend/guicedee/README.md`, `rules/generative/backend/guicedee/client/README.md`).
- **Reactive engine:** Vert.x 5 (`rules/generative/backend/vertx/README.md`).
- **Nullness:** JSpecify (`rules/generative/backend/jspecify/README.md`).
- **Lombok & Logging:** `rules/generative/backend/lombok/README.md`, `rules/generative/backend/logging/README.md`.
- **CI/CD:** GitHub Actions (`rules/generative/platform/ci-cd/README.md`, `rules/generative/platform/ci-cd/providers/github-actions.md`).

## Glossary composition
Topic glossaries (Java, CRTP, GuicedEE, GuicedEE Client, Lombok, JSpecify) take precedence; our host `GLOSSARY.md` links to:
- `rules/generative/language/java/GLOSSARY.md`
- `rules/generative/backend/fluent-api/GLOSSARY.md`
- `rules/generative/backend/guicedee/GLOSSARY.md`
- `rules/generative/backend/guicedee/client/GLOSSARY.md`
- `rules/generative/backend/lombok/GLOSSARY.md`
- `rules/generative/backend/jspecify/GLOSSARY.md`

## Architecture artifacts
- Context, container, and serial-I/O component views: `docs/architecture/c4-context.md`, `docs/architecture/c4-container.md`, `docs/architecture/c4-component-serial-io.md`.
- Sequences: `docs/architecture/sequence-connection.md`, `docs/architecture/sequence-idle-monitor.md`.
- ERD: `docs/architecture/erd-serial-domain.md`.
- Index: `docs/architecture/README.md`.

Every future stage must load this reference before authoring related docs or code so that the artifacts remain traceable (PACT ↔ GLOSSARY ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION ↔ architecture).  
