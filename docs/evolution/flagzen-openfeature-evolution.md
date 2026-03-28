# Feature Evolution — flagzen-openfeature (M5 partial: OpenFeature SDK Adapter)

## Summary

New Gradle submodule bridging the OpenFeature SDK to FlagZen's FlagProvider SPI. Teams using any OpenFeature-compatible provider (Flagd, CloudBees, Split, etc.) get FlagZen's polymorphic dispatch.

## Timeline

- **DISCUSS**: 2026-03-28 — 3 user stories
- **DESIGN**: 2026-03-28 — 1 ADR (absent flag detection)
- **DISTILL**: 2026-03-28 — 16 acceptance scenarios
- **DELIVER**: 2026-03-28 — 16 TDD steps, all passing

## Architecture Decisions

- **ADR-020**: Reason-based absence detection — check `errorCode` and `reason` from OpenFeature details API instead of sentinel values

## Key Design Choices

1. **Reason-based absence detection**: `errorCode != null` OR `reason == "DEFAULT"` → `Optional.empty()`
2. **Native typed delegation**: `getBoolean`/`getInt`/`getDouble` use OpenFeature's typed methods directly (no string parsing)
3. **Long via int widening**: `getLong` uses `getIntegerDetails` with int→long cast (OpenFeature SDK limitation)
4. **EvaluationContext mapping**: FlagZen context → OpenFeature MutableContext (targetingKey + typed attributes)
5. **Dual constructors**: no-arg (global client) + parameterized (injected client)

## Quality Gates

|                  Gate                  |           Result           |
| -------------------------------------- | -------------------------- |
| Acceptance tests (16 scenarios)        | PASS                       |
| PITest mutation testing (flagzen-core) | 84% kill rate (gate: ≥80%) |
| DES integrity verification (16 steps)  | PASS                       |
