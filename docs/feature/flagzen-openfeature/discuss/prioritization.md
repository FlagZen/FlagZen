# Prioritization: flagzen-openfeature

## Release Priority

| Priority | Release | Target Outcome | Rationale |
|----------|---------|---------------|-----------|
| 1 | Walking Skeleton (US-OF-01) | String flags resolve end-to-end through OpenFeature | Validates core assumption: adapter pattern works with OpenFeature details API |
| 2 | Typed Methods (US-OF-02) | Typed flags resolve without string round-tripping | Builds on skeleton; correctness improvement for typed features |
| 3 | Context-Aware (US-OF-03) | Per-user targeting works through the adapter | Completes the adapter; requires EvaluationContext mapper |

## Backlog Suggestions

| Story | Release | Priority | Outcome Link | Dependencies |
|-------|---------|----------|-------------|--------------|
| US-OF-01 | WS | P1 | String dispatch works | None (M0 complete) |
| US-OF-02 | R1 | P2 | Typed dispatch works | US-OF-01 |
| US-OF-03 | R1 | P3 | Context-aware dispatch works | US-OF-01 |

> **Note**: All 3 stories ship together in v1.1.0. Priority reflects implementation order, not release slicing.
