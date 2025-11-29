# Implementation — GuicedEE Guiced Cerial

## Modules and packages
- JPMS module `com.guicedee.cerial` (see `src/main/java/module-info.java`) exports the public API packages `com.guicedee.cerial`, `com.guicedee.cerial.enumerations`, and `com.guicedee.cerial.implementations`. The module `provides IGuiceModule with CerialPortsBindings` so GuicedInjection discovers serial bindings at startup.
- `CerialPortConnection` (CRTP) is the primary API surface, while `CerialIdleMonitor`, `ComPortEvents`, and the `DataSerialPort*Listener` implementations live under `com.guicedee.cerial.implementations` to share wired behavior.
- Enumerations (`BaudRate`, `DataBits`, `FlowControl`, etc.) centralize the serial configuration domain.

## Dependency wiring
- The module requires Transitive GuicedEE artifacts (`com.guicedee.client`, `com.guicedee.jsonrepresentation`) and Vert.x (`io.vertx.core`) for async timers.
- jSerialComm is declared as `requires transitive com.fazecast.jSerialComm` to expose the native port driver to downstream modules.
- Lombok and Jackson annotations are compile-time dependencies (`requires static` for Lombok and runtime for Jackson).

## Configuration & runtime behavior
- `CerialPortsBindings` iterates over port numbers and binds each `CerialPortConnection` through `CerialPortConnectionProvider`, ensuring a single pantry of configurable connections (see `src/main/java/com/guicedee/cerial/implementations/CerialPortsBindings.java`).
- `CerialIdleMonitor` is leveraged to mark unused connections as `ComPortStatus.Silent` via Vert.x timers, while `ComPortEvents` centralizes status/data callbacks.
- Logging uses Log4j2 and follows the existing patterns observed in `CerialPortConnection`, `CerialIdleMonitor`, and the listeners.

## Tests and validation
- The `src/test/java` tree exercises the connection lifecycle, reconnection behavior, and serialization support (e.g., `CerialPortConnectionReconnectTest`, `TestableCerialPortConnection`). Future tests should follow the BDD/TDD orientation dictated in `rules/generative/architecture/tdd.md` and `rules/generative/architecture/bdd.md`.
- Sequence and architecture docs (`docs/architecture/*.md`) provide context for verifying data flow and idle detection before touching code.

## Environment, CI, and secrets
- `.env.example` mirrors the shared env keys from `rules/generative/platform/secrets-config/env-variables.md`; this file documents `ENVIRONMENT`, `PORT`, `SERVICE_NAME`, `JWT_*`, and `OAUTH2_*` placeholders.
- GitHub Actions is configured to run Maven and include the shared `GuicedEE/Workflows` package for publishing (see `.github/workflows/maven-package.yml`).
- Secrets such as `USERNAME`, `USER_TOKEN`, `SONA_USERNAME`, `SONA_PASSWORD` are documented in both the workflow and in `GUIDES.md`.

## Traceability
- Implementation notes reference `RULES.md`, `GLOSSARY.md`, `GUIDES.md`, and `PACT.md` to close the loop; any change to the API must update these documents accordingly.
- Architecture diagrams (context, container, component) live under `docs/architecture/`, and `docs/PROMPT_REFERENCE.md` tracks the selected stacks so future AI prompts load them prior to generating code.
