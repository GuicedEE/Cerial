# GitHub Copilot Instructions

- Follow RULES.md sections 4 (Behavioral Agreements), 5 (Technical Commitments), and document-modularity/forward-only policies before writing suggestions.
- Keep project-specific docs outside the `rules/` submodule; reference `docs/PROMPT_REFERENCE.md` for selected stacks and diagrams.
- Respect the documentation-first workflow: architecture/sequence/ERD docs must be produced before implementation changes (Stage 1 precedes Stage 4).
- Blanket approval is granted for this run, so the stage gates auto-advance, but mention the checkpoints in your response.
- Close loops between PACT ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION ↔ GLOSSARY, and reference the relevant rule indexes when coding or documenting.
