# ADR-020: Absent Flag Detection Strategy

## Status

Proposed

## Context

`OpenFeatureFlagProvider` implements `FlagProvider`, whose contract returns `Optional<String>` (and typed optionals) -- empty means "flag not set or unavailable." OpenFeature's `Client` API does not return optionals. Its `getStringValue(key, default)` always returns a value (either resolved or the caller-supplied default). There is no way to distinguish "flag resolved to this value" from "flag not found, here is your default back" using the simple `get*Value` methods alone.

We need a strategy for the adapter to detect when a flag was genuinely resolved vs. when OpenFeature fell back to the client-supplied default.

### Quality Attributes at Stake

- **Correctness**: False positives (returning a value when the flag is absent) break FlagZen's fallback/default-variant logic. False negatives (returning empty when the flag exists) break dispatch.
- **Reliability**: The strategy must handle provider errors, uninitialized clients, and missing flags consistently.
- **Maintainability**: The strategy must not depend on fragile assumptions about flag values (e.g., "no real flag will ever equal this sentinel").

## Decision

Use **reason-based detection** via `Client.get*Details()` methods.

Call `client.getStringDetails(key, "")` (and typed equivalents). Inspect the `FlagEvaluationDetails` response:

1. If `errorCode` is non-null, return empty (evaluation failed).
2. If `reason` equals `"DEFAULT"`, return empty (no real resolution occurred).
3. Otherwise, return the resolved value.

## Alternatives Considered

### Alternative A: Sentinel Value Detection

Pass a unique sentinel default (e.g., `"__FLAGZEN_UNSET__"`) to `client.getStringValue(key, sentinel)`. If the returned value equals the sentinel, the flag is absent.

**Evaluation:**

- (+) Simpler -- uses the basic `get*Value` API, no need for `get*Details`
- (-) **Fragile**: If a flag is genuinely configured with the sentinel value, the adapter incorrectly reports it as absent. This is a correctness bug that is impossible to prevent.
- (-) **Type-specific sentinels**: For boolean (only two values), there is no safe sentinel. `false` is a valid flag value. For integer/double, any number could be a real value.
- (-) Violates the "no magic values" principle

**Rejected because**: correctness failure for boolean flags is guaranteed (no safe sentinel exists), and the sentinel collision risk for string flags is non-zero.

### Alternative B: Two-Call Strategy

First call `client.getStringValue(key, sentinelA)`, then call again with `sentinelB`. If both return their respective sentinels, the flag is absent. If both return the same non-sentinel value, the flag is present.

**Evaluation:**

- (+) Eliminates sentinel collision (astronomically unlikely both sentinels match a real value)
- (-) **Doubles evaluation calls** -- performance penalty on every flag resolution
- (-) **Race condition**: Flag value could change between the two calls
- (-) Still fails for boolean type (only two possible values)
- (-) Over-engineered for a problem the `Details` API already solves

**Rejected because**: performance overhead and race condition risk, when the `Details` API provides a clean solution.

### Alternative C: Exception-Based Detection

Configure OpenFeature to throw on missing flags, catch the exception, return empty.

**Evaluation:**

- (-) OpenFeature SDK does not throw for missing flags -- it returns defaults. This alternative does not exist in the API.

**Rejected because**: not supported by the OpenFeature SDK API.

## Consequences

### Positive

- **Correct for all types**: Works identically for string, boolean, integer, and double. No type-specific sentinel problems.
- **No magic values**: The sentinel passed to `get*Details` is irrelevant -- the `reason` field determines the result, not the value comparison.
- **Handles all error cases**: `errorCode` non-null covers provider errors, unreachable backends, and type mismatches uniformly.
- **Forward-compatible**: If OpenFeature adds new reason values, the adapter treats them as "resolved" by default (safe fallback).

### Negative

- **Dependency on `reason` semantics**: The adapter assumes `DEFAULT` reason means "no real resolution." If a provider uses `DEFAULT` reason for a genuinely resolved value, the adapter would incorrectly return empty. Per the OpenFeature specification, `DEFAULT` means the default value was used, so this interpretation is correct -- but non-compliant providers could violate it.
- **Slightly more complex API surface**: `get*Details` returns a richer object than `get*Value`. The adapter must extract and inspect multiple fields. This is minor complexity.
