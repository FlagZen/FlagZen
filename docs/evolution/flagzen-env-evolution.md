# Feature Evolution — flagzen-env (M3: Environment Variable Provider)

## Summary

Delivered two new Gradle modules providing environment-variable-based flag resolution with reusable key-mapping infrastructure.

|        Module         |         Package          |                                          Types                                           |
| --------------------- | ------------------------ | ---------------------------------------------------------------------------------------- |
| `flagzen-key-mapping` | `com.flagzen.keymapping` | `FlagKeyParser`, `FlagKeyParsers`, `FlagKeyFormat`, `FlagKeyFormats`, `ConflictStrategy` |
| `flagzen-env`         | `com.flagzen.env`        | `EnvironmentVariableFlagProvider` (with nested `Builder`)                                |

## Timeline

- **DISCUSS**: 2026-03-28 — 10 user stories, 3 releases planned
- **DESIGN**: 2026-03-28 — 3 ADRs (module split, eager loading, conflict strategy)
- **DISTILL**: 2026-03-28 — 33 acceptance scenarios across 4 milestones
- **DELIVER**: 2026-03-28 — 40 TDD steps, all passing

## Architecture Decisions

- **ADR-015**: Key mapping module split — `flagzen-key-mapping` has zero dependencies
- **ADR-016**: Eager loading — single `System.getenv()` at construction, immutable map
- **ADR-017**: Conflict strategy — `ConflictStrategy` enum (WARN/ERROR) with cardinality-based defaults

## Key Design Choices

1. **Parse/format separation**: `FlagKeyParser` (source → segments) and `FlagKeyFormat` (segments → flag key) are independent SAM interfaces
2. **Prefix per parser**: different parsers can have different prefixes
3. **Eager loading**: `getString()` is O(1) map lookup, no runtime I/O
4. **`Supplier<Map<String, String>>`** injection for testability
5. **Cardinality defaults**: 1×1/N×1/1×N = WARN, N×N = ERROR (overridable)
6. **First-access warning**: conflicted keys warn once on first `getString()` call

## Quality Gates

|                  Gate                  |               Result                |
| -------------------------------------- | ----------------------------------- |
| Acceptance tests (33 scenarios)        | PASS                                |
| PITest mutation testing (flagzen-core) | 86% kill rate (gate: ≥80%)          |
| DES integrity verification (40 steps)  | PASS                                |
| Adversarial review                     | PASS (2 HIGH fixed, 3 MEDIUM fixed) |

## Acceptance Test Coverage

|                    Milestone                    | Scenarios | Status |
| ----------------------------------------------- | --------- | ------ |
| Walking skeleton                                | 2         | PASS   |
| Key mapping (parsers + formatters)              | 11        | PASS   |
| Env provider (defaults, builder, ServiceLoader) | 15        | PASS   |
| Conflict strategy (WARN/ERROR, first-access)    | 15        | PASS   |
