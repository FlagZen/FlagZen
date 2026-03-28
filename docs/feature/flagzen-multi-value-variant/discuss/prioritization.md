# Prioritization: flagzen-multi-value-variant

## Release Priority

| Priority | Release | Target Outcome | Rationale |
|----------|---------|---------------|-----------|
| 1 | R1: String + Int Multi-Value | Developer uses array syntax for string and int features with compile-time duplicate safety | Walking skeleton + most common types. Validates annotation schema change is non-breaking. |
| 2 | R2: Long + Composability | Full type coverage and composability with @Repeatable | Extends proven pattern to remaining type. Composability validates no regression with existing mechanism. |

## Backlog Suggestions

| Story | Release | Priority | Value | Urgency | Effort | Score | Dependencies |
|-------|---------|----------|-------|---------|--------|-------|--------------|
| US-01: String array multi-value | R1 | P1 | 5 | 5 | 2 | 12.5 | None |
| US-02: Int array multi-value | R1 | P2 | 4 | 4 | 2 | 8.0 | US-01 (annotation schema established) |
| US-03: Cross-array duplicate detection | R1 | P3 | 5 | 5 | 2 | 12.5 | US-01 |
| US-04: Long array multi-value | R2 | P4 | 3 | 3 | 1 | 9.0 | US-01 pattern established |
| US-05: Array + repeated composability | R2 | P5 | 4 | 3 | 2 | 6.0 | US-01, US-03 |
| US-06: Enum validation + REQUIRED fallback | R2 | P6 | 4 | 3 | 2 | 6.0 | US-01, US-03 |

> **Note**: US-03 (duplicate detection) scores high on value/urgency because without it, the feature is unsafe. It ships with R1, not as a separate release.

> **Riskiest Assumption**: Changing `String value()` to `String[] value()` is source-compatible for all existing user code. Validated by: existing single-value tests still compile after annotation change.
