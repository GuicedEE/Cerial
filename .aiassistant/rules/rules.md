# AI Assistant workspace rules

1. **Behavioral commitments** (from RULES.md sections 4 & 5): keep language precise, respectful, documentation-first, and transparent about limitations. No fictional details; cite actual code/data.
2. **Document Modularity policy**: split large docs, remove monoliths, replace them with topic-focused files, and link back to the index. See `RULES.md` Document Modularity sections for guidance.
3. **Forward-only policy**: do not preserve backward-compatible shims or anchors; every doc/implementation update must be in a single forward-only change set referencing the new artifacts (see `RULES.md` section 6).
4. **Stage gating**: Architecture/sequence/ERD docs precede implementation notes. Blanket approval is recorded (per PROMPT_ADOPT_EXISTING_PROJECT inputs), so stage gates auto-advance, but the checkpoints remain documented.
5. **Traceability**: Close loops across PACT ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION ↔ GLOSSARY ↔ docs/architecture; include link references accordingly.
6. **Prompt reference**: Always load `docs/PROMPT_REFERENCE.md` before generating code to ensure the selected stacks, diagrams, and glossary topics are in scope.
