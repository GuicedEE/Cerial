# 🔌 GuicedCerial

[![JDK](https://img.shields.io/badge/JDK-25%2B-0A7?logo=java)](https://openjdk.org/projects/jdk/25/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Vert.x](https://img.shields.io/badge/Vert.x-5-4B9)](https://vertx.io/)

## 📚 Docs & Rules
- Pact: `PACT.md`
- Rules: `RULES.md`
- Guides: `GUIDES.md`
- Architecture: `docs/architecture/README.md`

A GuicedEE serial-port utility that exposes CRTP-fluent `CerialPortConnection` instances for applications that need jSerialComm-level control.

## Overview

GuicedCerial integrates with GuicedInjection, Log4j2, and Vert.x 5 to provide lifecycle-aware serial connectivity. The library exposes a JPMS-friendly module (`com.guicedee.cerial`), a collection of enumerations for baud rate/parity/flow control, and helper bindings that supply `CerialPortConnection` singletons for every COM port number.

## Getting started

Include the module as a Maven dependency within the GuicedEE BOM and inject named connections:

```
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>guiced-cerial</artifactId>
</dependency>
```

```
@Inject
@Named("1")
private CerialPortConnection connection;

connection.setBaudRate(BaudRate.$9600)
          .setDataBits(DataBits.$8)
          .setFlowControl(FlowControl.None)
          .connect();
```

## Documentation

- `PACT.md` — Collaboration agreement for this adoption run.
- `GLOSSARY.md` — Glossary index that links to Java 25, CRTP, GuicedEE, Lombok, and JSpecify topic glossaries.
- `RULES.md` / `GUIDES.md` / `IMPLEMENTATION.md` — Rules, how-to guidance, and implementation notes that reference the selected tech stack.
- `docs/architecture/` — Text-based C4 context/container/component views, two sequence diagrams, and an ERD; indexed in `docs/architecture/README.md`.
- `docs/PROMPT_REFERENCE.md` — Records the stacks, glossary coverage, and diagrams that AI agents must load first.
- `.env.example` — Canonical environment variables aligned with `rules/generative/platform/secrets-config/env-variables.md`.
- `.github/workflows/maven-package.yml` — GitHub Actions job that uses the shared `GuicedEE/Workflows` workflow for packaging.
- `.aiassistant/rules/rules.md` / `.github/copilot-instructions.md` — Workspace rules for the selected AI assistants.

## Rules Repository adoption

The `rules/` directory is the Rules Repository submodule. It must remain read-only for host-specific documentation, and you must link to the relevant indexes within it (e.g., `rules/generative/backend/guicedee/README.md`, `rules/generative/language/java/java-25.rules.md`) when describing architecture, APIs, or policy.

## Contributing & support

- Issues/bugs: open a GitHub issue with a minimal reproduction (serial port, OS, jSerialComm version, and log excerpt). Include relevant `ComPortStatus` transitions and listener output if possible.
- Pull requests: keep changes forward-only, update `PACT.md`, `RULES.md`, `GUIDES.md`, `IMPLEMENTATION.md`, and `docs/architecture/` when altering APIs or behaviors. Follow CRTP (no builders) and JSpecify nullness.
- Tests: prefer JUnit Jupiter; avoid blocking Vert.x event loops in listener/idle-monitor tests.
- Security/coordination: if reporting sensitive issues, avoid filing public details until coordinated; include contact info in the issue description.

## Environment & CI

- `.env.example` documents the required environment variables (oauth, DB, logging/tracing toggles).
- GitHub Actions are defined in `.github/workflows/maven-package.yml`; secrets such as `USERNAME`, `USER_TOKEN`, `SONA_USERNAME`, and `SONA_PASSWORD` are injected from the repository environment and referenced in `GUIDES.md`.

## License

This project is licensed under Apache 2.0 (see `LICENSE`).
