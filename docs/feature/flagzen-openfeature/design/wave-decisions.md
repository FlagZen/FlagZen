# Wave Decisions -- flagzen-openfeature DESIGN

## Decision Summary

| # | Decision | Choice | Rationale |
|---|----------|--------|-----------|
| D1 | Architectural style | Driven adapter in existing ports-and-adapters | Follows project architecture; this is a textbook adapter |
| D2 | Absent flag detection | Reason-based via `get*Details` | Correct for all types, no sentinel fragility (ADR-020) |
| D3 | Typed method delegation | Override all 10 FlagProvider methods | Native typed calls avoid string round-tripping |
| D4 | Context mapper visibility | Package-private | Internal detail; one consumer (`OpenFeatureFlagProvider`) |
| D5 | Constructor strategy | No-arg + static factory `create(Client)` | No builder -- insufficient configuration dimensions |
| D6 | Logging framework | java.util.logging (JUL) | Consistent with flagzen-env; zero added dependencies |
| D7 | Long type handling | `getIntegerDetails` + widen to long | OpenFeature SDK has no `getLongDetails`; documented limitation |
| D8 | `DISABLED` reason handling | Treat as resolved (return value) | A disabled flag returning a value is still a real resolution |

## Decisions Carried Forward from DISCUSS

The following decisions from the DISCUSS wave are confirmed without change:

- **D5 (DISCUSS)**: Reason-based absence detection (Option B) -- confirmed, now formalized as ADR-020
- **D6 (DISCUSS)**: Override typed methods with native delegation (Option A) -- confirmed
- **D7 (DISCUSS)**: Dual constructor strategy -- confirmed, simplified to no-arg + factory method (no builder)
- **D8 (DISCUSS)**: Bidirectional EvaluationContext mapping -- confirmed as unidirectional (FlagZen to OpenFeature only; there is no use case for the reverse direction)

## Open Questions Resolved

### Q: Should `EvaluationContextMapper` be public?

**No.** It is an implementation detail. If consumers need to map contexts for other purposes, that is their own concern. Exposing internal mappers creates API surface we must maintain.

### Q: Should we support `Value.asInstant()` in context mapping?

**Yes.** OpenFeature SDK's `Value` supports `Instant` natively. Since `EvaluationContext.attributes()` stores `Object`, and `Instant` is a common Java type, the mapper should handle it. This was flagged as uncertain in the DISCUSS wave (US-OF-03 technical notes). Resolved: support it.

### Q: What about `null` reason from non-compliant providers?

**Treat as resolved.** A provider that returns a value without setting a reason has still resolved the flag. Only explicit `DEFAULT` reason (or error) means absent. This is the most permissive and correct interpretation.
