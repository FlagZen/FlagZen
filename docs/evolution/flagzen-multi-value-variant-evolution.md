# Feature Evolution — flagzen-multi-value-variant (M13: Multi-Value Variant Mapping)

## Summary

Extended `@Variant` annotation to support multi-value mapping: multiple flag values can map to the same variant implementation via array syntax. All changes within flagzen-core.

## Timeline

- **DISCUSS**: 2026-03-28 — 7 user stories, 2 releases
- **DESIGN**: 2026-03-28 — 1 ADR (array migration strategy)
- **DISTILL**: 2026-03-28 — 31 acceptance scenarios across 5 milestones
- **DELIVER**: 2026-03-28 — 33 TDD steps, all passing

## Architecture Decisions

- **ADR-018**: In-place array migration — change annotation elements from scalar+sentinel to arrays with empty defaults

## Key Changes

1. `@Variant.value()`: `String` → `String[]` (source-compatible)
2. `@Variant.intValue()`: `int` with sentinel → `int[]` with default `{}`
3. `@Variant.longValue()`: `long` with sentinel → `long[]` with default `{}`
4. `@Variant.doubleValue()`: already `CloseTo[]` — no change
5. `@Variant.booleanValue()`: unchanged (only true/false, multi-value not needed)
6. Compile-time duplicate detection across multi-value arrays
7. `@CloseTo` overlap detection: `|v1 - v2| < delta1 + delta2` → compile error

## Quality Gates

| Gate | Result |
|------|--------|
| Acceptance tests (31 scenarios) | PASS |
| PITest mutation testing | 84% kill rate (gate: ≥80%) |
| DES integrity verification (33 steps) | PASS |
