# GUIDES — Applying the Rules

## Serial port configuration (CRTP + jSerialComm)
- Use the `CerialPortConnection` fluent setters (`setBaudRate`, `setDataBits`, etc.) that return the generic self type; the CRTP pattern is enforced by GuicedEE and detailed in `rules/generative/backend/fluent-api/crtp.rules.md` and `rules/generative/backend/guicedee/GLOSSARY.md`.
- Register status/data handlers through `ComPortEvents` so logging and event propagation stay centralized.
- Avoid mixing Builder-style patterns with this CRTP surface; any new helper should extend `CerialPortConnection` with (J extends …) signatures and `return (J) this` when overriding setters.

## Lifecycle and injection (GuicedInjection)
- `CerialPortsBindings` implements `IGuiceModule` (`rules/generative/backend/guicedee/functions/guiced-injection-rules.md`) and registers every port provider via `Names.named(i + "")` so the application can inject `@Named("1")` style `CerialPortConnection` instances.
- `CerialPortConnectionProvider` guarantees that each connection is a singleton and configured consistently before being injected into downstream modules.
- Any SPI providers or lifecycle hooks must be declared in `module-info.java` (`rules/generative/language/java/GLOSSARY.md`, `module-info.java`).

## Monitoring, idle detection, and Vert.x integration
- `CerialIdleMonitor` uses `IGuiceContext.get(Vertx.class)` to schedule periodic checks; the module must not block Vert.x event loops and should rely on `setPeriodic`/`cancelTimer` as shown in `rules/generative/backend/guicedee/vertx/README.md`.
- Idle events update `ComPortStatus`, which flows through `ComPortEvents` to listeners and logs (see `rules/generative/platform/observability/README.md` for telemetry guidance).
- Align status logging with the `rules/generative/backend/logging/README.md` posture; prefer contextual diagnostics rather than scattershot println statements.

## Nullness, annotations, and Lombok
- Annotate classes/packages with `@org.jspecify.annotations.NullMarked` and annotate optional fields/methods with `@Nullable` per `rules/generative/backend/jspecify/README.md`.
- Lombok is used for getters, setters, and `@Log`; keep Lombok usage limited (`rules/generative/backend/lombok/README.md`) and do not rely on Lombok to generate CRTP-style setters.

## Testing, security, and CI readiness
- Existing tests under `src/test/java` follow JUnit Jupiter; align future tests with the architecture’s TDD/BBD stance by referencing `rules/generative/architecture/tdd.md` and `rules/generative/architecture/bdd.md` before adding new specs.
- Environment secrets follow `rules/generative/platform/secrets-config/env-variables.md`; `.env.example` mirrors the required keys and couples with GitHub Actions secrets documented in `rules/generative/platform/ci-cd/providers/github-actions.md`.
- Keep CI in Maven (build/test) mode and rely on the `GuicedEE/Workflows/.github/workflows/projects.yml` inclusion for packaging jobs (`rules/generative/platform/ci-cd/README.md`).

## Implementation guidance
- Always link back to `RULES.md` when writing new guides so the traceability loop (PACT ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION ↔ GLOSSARY) remains intact.
- For host-specific modifications, prefer linking to the most applicable topic guide and avoid writing new monolithic instructions.
