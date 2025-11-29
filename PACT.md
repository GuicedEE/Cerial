---
version: 2.0
date: 2025-10-20
title: GuicedEE Guiced Cerial Pact
project: GuicedEE / Guiced Cerial
authors: [GuicedEE maintainers, Junie, Codex, AI Assistant]
---

# 🤝 GuicedEE Guiced Cerial Pact

## 1. Purpose

This pact documents how humans and AI collaborate to adopt the Rules Repository into an existing GuicedEE serial-port library. The focus is on documentation-first, specification-driven adoption: architecture inventories, glossary grounding, RULES ↔ GUIDES ↔ IMPLEMENTATION traceability, and forward-only changes. Blanket approval has been granted for this run, so stage gates proceed without additional STOPs, but their checkpoints remain recorded.

## 2. Principles

- **Contextual fidelity** — Every description traces back to the actual `com.guicedee.cerial` sources, GuicedInjection bindings, and the jSerialComm dependency already present.
- **Spec-first** — Architecture/sequence/ERD docs precede implementation notes to respect the documentation-first workflow required by the repo.
- **Forward-only actions** — Legacy monoliths are replaced with modular docs; no compatibility shims are introduced.
- **Traceability loops** — PACT ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION ↔ GLOSSARY ↔ architecture assets remain cross-linked so AI and humans can follow the lineage.
- **CRTP compliance** — GuicedEE implies CRTP. No @Builder surfaces are introduced; fluent setters return the generic self type as seen in `CerialPortConnection`.

## 3. Structure of Work

| Layer | Artifact | Status |
| ----- | -------- | ------ |
| **Pact** | `PACT.md` (this file) | done |
| **Rules** | `RULES.md` linking to topic indexes | pending |
| **Guides** | `GUIDES.md` mapping rules to workflows | pending |
| **Implementation** | `IMPLEMENTATION.md` describing modules, wiring, and plan | pending |

## 4. Behavioral Agreements

- Language stays technical yet conversational, avoiding generic AI tone and referencing actual packages and diagrams.
- AI outputs chunked documentation with modular headings; large sections are never merged into a single monolith.
- The collaboration acknowledges when knowledge gaps exist and calls out questions instead of inventing details.

## 5. Technical Commitments

- Use Markdown and Mermaid for diagrams; all architecture sources live under `docs/architecture/`.
- Refer to `rules/generative/backend/guicedee` (Core + Client) and the Java 25/Maven guidelines when describing APIs.
- Keep JPMS module descriptors accurate; rely on existing `module-info.java` as the definitive wiring documentation.
- Align logging with the existing Log4j2 usage already spotted in the sources.
- Document environment, CI, and AI workspace alignments before touching code.

## 6. Shared Goals

1. Inventory the current module, produce architecture docs, and link them from `docs/PROMPT_REFERENCE.md`.
2. Author RULES, GUIDES, IMPLEMENTATION, and GLOSSARY that reference the identified stacks (Java 25, CRTP, GuicedEE, Vert.x 5, JSpecify, Lombok, Logging).
3. Sync all docs with the Rules Repository (submodule `rules/`) while leaving project docs outside that directory.
4. Document environment variables and CI secrets consistent with `rules/generative/platform/secrets-config/env-variables.md` and the GitHub Actions reference.
5. Keep the documentation-first workflow intact until stage 4, limiting code touches to `README.md`, `.env.example`, CI workflow, AI workspace files, and the new docs themselves.
