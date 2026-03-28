# Wave Decisions -- flagzen-openfeature

## Decision 1: Feature Type

**Backend** -- adapter/bridge between two SPIs (OpenFeature SDK and FlagZen FlagProvider).

## Decision 2: Walking Skeleton

**Yes** -- thinnest slice: `OpenFeatureFlagProvider.getString(key)` delegates to OpenFeature `Client.getStringDetails(key, defaultValue)` and returns `Optional<String>`.

## Decision 3: UX Research Depth

**Lightweight** -- standard adapter pattern, no user-facing UI. The "user" is a Java developer adding a Gradle dependency and optionally configuring the provider.

## Decision 4: JTBD

**Skipped** -- motivations are clear and singular: teams already on OpenFeature want FlagZen's polymorphic dispatch without replacing their flag infrastructure.

## Key Design Decisions (from briefing)

### D5: getString Optional mismatch

**Option B selected**: Use `client.getStringDetails(key, defaultValue)` and inspect the `reason` field. If reason indicates no real value was resolved (e.g., `DEFAULT` with no provider configured, or `ERROR`), return `Optional.empty()`. This avoids sentinel-value fragility.

### D6: Typed method delegation

**Option A selected**: Override `getBoolean`/`getInt`/`getLong`/`getDouble` to delegate to OpenFeature's native typed detail methods (`client.getBooleanDetails`, `client.getIntegerDetails`, etc.). Avoids string-to-type-to-string round-tripping. More correct when the upstream provider stores native types.

### D7: Constructor strategy

**Both**: No-arg constructor calls `OpenFeatureAPI.getInstance().getClient()` (for ServiceLoader). Parameterized constructor accepts a `Client` instance (for DI / testing). Builder optional -- may be overkill for 1-2 config options.

### D8: EvaluationContext mapping

Bidirectional mapper: `com.flagzen.EvaluationContext` to `dev.openfeature.sdk.EvaluationContext`. Targeting key maps 1:1. Attributes map iterates entries. FlagZen stores `Map<String, Object>`; OpenFeature accepts `Value` wrappers -- the mapper handles conversion.
