# Glossary

## Glossary precedence policy
- Topic glossaries referenced below have priority for their domain; this host glossary indexes those documents and adds only cross-topic context when necessary.
- Do not duplicate definitions already covered by the topic-specific files unless the definition is used across multiple topics and no other anchor exists.
- When CRTP is enforced (GuicedEE is in scope), apply the CRTP glossary first and cascade to builder definitions only when describing hypothetical migrations.

## Topic glossaries (linked, canonical)
1. **Java 25 + Tooling** — core language guidance, JPMS, nullness, and LTS policies. See `rules/generative/language/java/GLOSSARY.md` and the linked `java-25.rules.md` plus `build-tooling.md`.
2. **CRTP Fluent API** — strategy enforcement and setter patterns for this repository. See `rules/generative/backend/fluent-api/GLOSSARY.md`.
3. **GuicedEE** — system-level terminology (SPIs, lifecycle phases, service binding). See `rules/generative/backend/guicedee/GLOSSARY.md`.
4. **GuicedEE Client** — client-side SPI and Mutiny/CRTP terms specific to inject clients. See `rules/generative/backend/guicedee/client/GLOSSARY.md`.
5. **Lombok** — annotation usage and integration notes. See `rules/generative/backend/lombok/GLOSSARY.md`.
6. **JSpecify** — nullness annotations and strictness guides. See `rules/generative/backend/jspecify/GLOSSARY.md`.

## Host glossary anchors and interpretation guidance
- **CerialPortConnection** — Fluent serial-port client visible to injectors. `CerialPortConnection` implements CRTP (see its generic signature in `src/main/java/com/guicedee/cerial/CerialPortConnection.java`) and participates in the GuicedInjection lifecycle via `IGuicePreDestroy`.
- **ComPortStatus** — Finite state machine for ports (Offline, Connecting, Online, Silent, etc.). Prefer the shared `ComPortStatus` enumeration (see `src/main/java/com/guicedee/cerial/enumerations/ComPortStatus.java`) and log transitions through `ComPortEvents`.
- **ComPortEvents** — The event aggregator used by the connection to broadcast status and data updates while logging through Log4j2.
- **CerialIdleMonitor** — Idle detector that reads `lastMessageTime` and marks connections `Silent` when the Vert.x timer (via `IGuiceContext.get(Vertx.class)`) exceeds the configured threshold.
- **Environment alignment** — `.env.example` lists the canonical env names; refer to `rules/generative/platform/secrets-config/env-variables.md` for fully scoped secrets.

## Prompt alignment notes
- CRTP is enforced by the GuicedEE stack. Do not reintroduce Builder-style chaining; fluent setters must return the generic self type with `@SuppressWarnings("unchecked")` when needed.
- Nullness defaults to JSpecify's `@NullMarked`; annotate optionality with `@Nullable` only when `null` is part of the contract.
- Logging must follow existing Log4j2 usage (no new frameworks) and align with `rules/generative/backend/logging/README.md` when describing practices.
