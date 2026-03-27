# Wave Decisions -- flagzen-typed-variants

## Decisions Log

| Decision | Value | Rationale |
| -------- | ----- | --------- |
| Feature type | Backend (Java library) | No UI, no CLI -- annotation processor, SPI, and code generation |
| Walking skeleton | None | Library feature, not end-to-end user journey; all stories deliver independently testable compilation/runtime behavior |
| UX research depth | Lightweight | Developer-facing API design; persona is Java developer writing annotations and consuming typed flag values |
| JTBD analysis | Skipped | Scope is well-understood from project brief; typed variants extend existing M0 dispatch with type-safe alternatives |

## Implications

- **No journey emotional arc**: replaced with developer experience arc (annotation authoring, compilation feedback, runtime dispatch)
- **No TUI/CLI mockups**: replaced with API surface examples showing annotation usage and compiler output
- **Persona**: Java developer using FlagZen annotations; characteristics drawn from project brief context
- **Story map**: organized by developer activity (define, annotate, compile, dispatch, test) rather than user journey steps
- **Outcome KPIs**: framed as library adoption and developer experience metrics, not end-user behavior change
