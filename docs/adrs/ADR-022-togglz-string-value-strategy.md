# ADR-022: Togglz String Value via Activation Strategy Parameter

## Status

Proposed

## Context

`TogglzFlagProvider` implements `FlagProvider`, which requires `getString(String key)` returning `Optional<String>`. Togglz is fundamentally a boolean toggle library -- its core API is `FeatureManager.isActive(Feature)` returning `boolean`. There is no native string value API.

However, FlagZen's polymorphic dispatch and conditional API rely on `getString` as the foundational method. The Togglz adapter needs a strategy for resolving string values from Togglz's data model.

### Togglz Data Model

A `FeatureState` in Togglz contains:

- `isEnabled()` -- boolean toggle state
- `getStrategyId()` -- the activation strategy name (e.g., `"gradual"`, `"release-date"`)
- `getParameter(String name)` -- named string parameters on the activation strategy

Strategy parameters are arbitrary key-value pairs that activation strategies use for configuration. They are stored in the Togglz state repository alongside the feature state.

### Quality Attributes at Stake

- **Correctness**: `getString` must return meaningful values that FlagZen can use for variant dispatch.
- **Usability**: The convention must be simple to understand and document. Users configure Togglz features; the convention should not require obscure configuration.
- **Compatibility**: The approach must work with existing Togglz state repositories (JDBC, file, in-memory) without modification.

## Decision

Use a **well-known strategy parameter** named `"value"` as the string value source. Fall back to the enabled state as a string (`"true"` / `"false"`) when the parameter is absent.

Resolution logic:

1. Look up the `Feature` by key (case-insensitive match on `Feature.name()`)
2. Get `FeatureState` from `FeatureManager`
3. If `featureState.getParameter("value")` is non-null and non-empty, return that
4. Otherwise, return `String.valueOf(featureState.isEnabled())`

This means:

- Users who want string-typed dispatch configure a `"value"` parameter on their Togglz feature's activation strategy
- Users who only use boolean dispatch (the common case) do not need any special configuration -- the adapter returns `"true"` or `"false"`, which the `FlagProvider` default `getBoolean` method parses correctly
- Users who want numeric dispatch set the `"value"` parameter to a numeric string (e.g., `"42"`), and the `FlagProvider` default `getInt`/`getLong`/`getDouble` methods parse it

## Alternatives Considered

### Alternative A: Only Support Boolean

Override only `getBoolean`. Do not override `getString` at all -- let the `FlagProvider` default implementation call `getString(key)`, which calls `getBoolean(key)` ... wait, that is circular. The `FlagProvider` default `getString` is abstract (no default impl). The adapter must implement `getString`.

Revised: implement `getString` to return `"true"` / `"false"` only.

**Evaluation:**

- (+) Simplest possible implementation
- (-) **No string variant support**: Users cannot use FlagZen's polymorphic dispatch with string variants (e.g., `@Variant("CLASSIC")`, `@Variant("MODERN")`)
- (-) Togglz users who want to migrate to FlagZen would need to switch to a different provider for string features

**Rejected because**: overly limiting. The strategy parameter approach adds minimal complexity while enabling string and numeric dispatch.

### Alternative B: Custom `FeatureState` Subclass

Create a FlagZen-specific `FeatureState` extension that adds a `getValue()` method.

**Evaluation:**

- (-) Requires users to use a custom state class, breaking compatibility with standard Togglz state repositories
- (-) Adds coupling between FlagZen and Togglz internals
- (-) Overengineered for what is essentially a key-value lookup

**Rejected because**: breaks compatibility with existing Togglz infrastructure.

### Alternative C: Use `FeatureState.getStrategyId()` as the Value

Return the activation strategy ID itself as the string value.

**Evaluation:**

- (-) Strategy IDs are strategy names (e.g., `"gradual"`, `"release-date"`), not feature flag values
- (-) Semantic mismatch: the strategy is how the flag is evaluated, not what value it has

**Rejected because**: strategy ID is not semantically a flag value.

## Consequences

### Positive

- **Works with existing Togglz infrastructure**: Strategy parameters are a standard Togglz feature, stored in any state repository.
- **Progressive complexity**: Boolean-only users need zero configuration. String/numeric users add one parameter. No breaking changes.
- **Documented convention**: The parameter name `"value"` is documented in the adapter's Javadoc and the FlagZen docs site.

### Negative

- **Convention-based**: The `"value"` parameter name is a FlagZen convention, not a Togglz standard. Users must know and follow this convention. Misspelling (e.g., `"Value"`, `"val"`) silently falls back to enabled-as-string.
- **Strategy parameter limitations**: Togglz strategy parameters are always strings. There is no type validation. A parameter set to `"hello"` when `getInt` is called results in `OptionalInt.empty()` from the default parsing -- correct but not obvious.
- **No multi-value parameter**: A single `"value"` parameter holds one value. This is sufficient for FlagZen's use case (one flag value per feature per state), but limits more complex scenarios.
