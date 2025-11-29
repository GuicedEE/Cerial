# Project RULES — GuicedEE Guiced Cerial

## Scope and intent
- This project is a GuicedEE library that exposes serial port connectivity APIs (`com.guicedee.cerial`) while integrating with GuicedInjection’s lifecycle and Vert.x 5 for timers and async event handling.
- Forward-only change policy is enforced; legacy monolith docs must be replaced with modular entries and everything must trace back to PACT, RULES, GUIDES, GLOSSARY, and IMPLEMENTATION.
- Documentation-first workflow: architecture/sequence/ERD docs precede any code changes; see `docs/architecture/README.md` and `docs/PROMPT_REFERENCE.md` for the discovery record.

## Chosen technology stack and rules
1. **Language & toolchain** — Java 25 LTS with Maven (`rules/generative/language/java/java-25.rules.md`, `rules/generative/language/java/build-tooling.md`, `module-info.java`).
2. **Fluent API** — CRTP enforced by GuicedEE; use `rules/generative/backend/fluent-api/crtp.rules.md` and avoid Lombok @Builder on CRTP types.
3. **Frameworks & runtime** — GuicedEE Core & Client (`rules/generative/backend/guicedee/README.md`, `rules/generative/backend/guicedee/client/README.md`, `rules/generative/backend/guicedee/vertx/README.md`) with Vert.x 5 (`rules/generative/backend/vertx/README.md`).
4. **Nullness and annotations** — JSpecify-first (`rules/generative/backend/jspecify/README.md`); annotate optional contracts with `@Nullable` only when null is expected, keep `@NullMarked` defaults.
5. **Lombok & logging** — Follow Lombok guidelines (`rules/generative/backend/lombok/README.md`) and Log4j2 logging patterns (`rules/generative/backend/logging/README.md`).
6. **CI / Observability / Security** — GitHub Actions (`rules/generative/platform/ci-cd/README.md`, `rules/generative/platform/ci-cd/providers/github-actions.md`), observability (`rules/generative/platform/observability/README.md`), and security/auth (`rules/generative/platform/security-auth/README.md`).
7. **Architecture paradigms** — Document and align with the architecture indexes: `rules/generative/architecture/README.md`, plus `tdd.md` and `bdd.md` for verification artifacts that guide future test-writing.

## Linking and traceability
- Cross-link to the architecture artifacts (`docs/architecture/README.md`, `docs/PROMPT_REFERENCE.md`).
- Reference glossary terms via `GLOSSARY.md`; topic glossaries take precedence and are linked there.
- Implementation details are described in `IMPLEMENTATION.md`; every API change must trace to the relevant guide in `GUIDES.md`.

## Environment and deployment rules
- `.env.example` mirrors `rules/generative/platform/secrets-config/env-variables.md` values (see `ENVIRONMENT`, `PORT`, `SERVICE_NAME`, `OAUTH2_*` keys, etc.).
- GitHub Actions workflows pull secrets via repository `secrets.*` names described under the CI provider guide.

## Compliance requirements
- Keep `rules/` as a submodule; do not author project docs inside it.
- If exceptions are needed, document them explicitly in `MIGRATION.md` or `IMPLEMENTATION.md`.
