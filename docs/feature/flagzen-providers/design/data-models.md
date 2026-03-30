# Data Models -- flagzen-providers

## 1. LaunchDarkly Adapter

### 1.1 EvaluationContext Mapping

#### Field Mapping

| FlagZen `EvaluationContext` | `LDContext` | Notes |
| --- | --- | --- |
| `targetingKey()` (String, nullable) | `LDContext.builder(ContextKind.DEFAULT, key)` | If null, use `"anonymous"` key with `anonymous(true)` |
| `attributes()` (`Map<String, Object>`) | `.set(name, LDValue)` per entry | Custom attributes on the context |

#### Attribute Type Conversion

| Java runtime type | `LDValue` factory | Notes |
| --- | --- | --- |
| `String` | `LDValue.of(String)` | Direct |
| `Boolean` | `LDValue.of(boolean)` | Direct |
| `Integer` | `LDValue.of(int)` | Direct |
| `Long` | `LDValue.of(long)` | Direct -- no narrowing needed (unlike OpenFeature) |
| `Double` | `LDValue.of(double)` | Direct |
| `List<?>` | `LDValue.buildArray()` | Recursive: each element converted to `LDValue` |
| `Map<String, ?>` | `LDValue.buildObject()` | Recursive: each entry converted to `LDValue` |
| Any other type | **Skipped** | Log warning via JUL |

#### Conversion Rules

1. **Null targeting key**: Build with key `"anonymous"` and set `anonymous(true)`. LaunchDarkly requires a non-null context key.
2. **Empty attributes map**: Produce `LDContext` with key only.
3. **Null attribute value**: Skip the entry, log warning.
4. **Nested collections**: `List` and `Map` values converted recursively via `LDValue` builders.
5. **Long values**: `LDValue.of(long)` handles the full long range natively. No narrowing needed.

#### Anonymous Context (No-Context Overloads)

When `getString(key)` (no context) is called, the adapter needs an `LDContext` because LaunchDarkly requires one. The adapter uses a shared anonymous context singleton:

```
LDContext ANONYMOUS = LDContext.builder(ContextKind.DEFAULT, "anonymous")
    .anonymous(true)
    .build();
```

This is allocated once and reused across all no-context calls. It is thread-safe (immutable).

### 1.2 Absence Detection

#### `EvaluationDetail<T>` Fields Used

| Field | Type | Usage |
| --- | --- | --- |
| `getValue()` | `T` | The resolved value (or default if absent) |
| `getReason()` | `EvaluationReason` | Why this value was returned |
| `getVariationIndex()` | `Integer` | Which variation was selected (null if none) |

#### Decision Logic

```
Input: EvaluationDetail<T> detail
Output: Optional<T> (or OptionalInt/OptionalLong/OptionalDouble)

IF detail.getReason().getKind() == ERROR THEN
    return empty
    (log at DEBUG: flag key, error kind, error description)

IF detail.getReason().getKind() == PREREQUISITE_FAILED THEN
    return empty
    (the flag could not be fully evaluated due to prerequisite)

ELSE
    return of(detail.getValue())
```

#### Reason Kind Mapping

| `EvaluationReason.Kind` | Adapter returns | Rationale |
| --- | --- | --- |
| `OFF` | `of(value)` | Off-variation is a deliberate value chosen by the flag administrator |
| `FALLTHROUGH` | `of(value)` | Flag is on, default variation served |
| `TARGET_MATCH` | `of(value)` | User matched a specific target |
| `RULE_MATCH` | `of(value)` | User matched a targeting rule |
| `PREREQUISITE_FAILED` | `empty` | Evaluation incomplete -- prerequisite not met |
| `ERROR` | `empty` | Evaluation failed |

#### Error Kind Details (for logging)

| `EvaluationReason.ErrorKind` | Description |
| --- | --- |
| `FLAG_NOT_FOUND` | Flag key does not exist |
| `MALFORMED_FLAG` | Flag configuration is invalid |
| `USER_NOT_SPECIFIED` | Context required but not provided |
| `CLIENT_NOT_READY` | SDK still initializing |
| `WRONG_TYPE` | Value type mismatch |
| `EXCEPTION` | Unexpected error |

### 1.3 Long Type via JSON Value

For `getLong` and `getLong(key, ctx)`:

```
Input: key, optional context
Output: OptionalLong

1. Call client.jsonValueVariationDetail(key, LDValue.ofNull(), ldContext)
2. IF absent (ERROR or PREREQUISITE_FAILED reason) -> return OptionalLong.empty()
3. IF detail.getValue().isNumber() is false -> return OptionalLong.empty()
4. return OptionalLong.of(detail.getValue().longValue())
```

`LDValue.longValue()` returns the numeric value as a Java `long`. For integers and longs stored in JSON, this is lossless. For floating-point JSON numbers, it truncates (standard JSON number behavior).

### 1.4 Default Sentinel Values

| Type | Method | Default sentinel | Notes |
| --- | --- | --- | --- |
| String | `stringVariationDetail` | `""` | Irrelevant -- reason-based check |
| Boolean | `boolVariationDetail` | `false` | Irrelevant -- reason-based check |
| Integer | `intVariationDetail` | `0` | Irrelevant -- reason-based check |
| Long | `jsonValueVariationDetail` | `LDValue.ofNull()` | Null sentinel for JSON value |
| Double | `doubleVariationDetail` | `0.0` | Irrelevant -- reason-based check |

---

## 2. Togglz Adapter

### 2.1 Feature Lookup (String Key to Togglz Feature)

Togglz features are enum instances implementing `org.togglz.core.Feature`. FlagZen uses string keys. The `FeatureLookup` class builds a case-insensitive mapping:

```
Input: FeatureManager.getFeatures() -> Set<Feature>
Mapping: Feature.name().toUpperCase(Locale.ROOT) -> Feature
Lookup: key.toUpperCase(Locale.ROOT) -> cached Feature or empty

Examples:
  "DARK_MODE"      -> DarkModeFeature.DARK_MODE
  "dark_mode"      -> DarkModeFeature.DARK_MODE (case-insensitive)
  "dark-mode"      -> empty (hyphens do not match underscores)
  "checkout.beta"  -> empty (dots do not match)
```

The mapping is built lazily on first `resolve()` call and cached in a `ConcurrentHashMap`. Thread-safe via `computeIfAbsent` pattern.

### 2.2 Absence Detection

| Condition | Check | Adapter returns |
| --- | --- | --- |
| Feature not found | `FeatureLookup.resolve(key)` returns `empty` | `empty` (for all methods) |
| Feature found, state null | `featureManager.getFeatureState(feature)` returns `null` | `empty` |
| Feature found, state exists | State is present | `of(value)` |

### 2.3 String Value Strategy

Togglz has no native string value mechanism. The adapter uses activation strategy parameters:

```
Input: key
Output: Optional<String>

1. Resolve Feature from key via FeatureLookup
2. IF feature not found -> return empty
3. Get FeatureState from FeatureManager
4. IF state is null -> return empty
5. IF state.getParameter("value") is non-null and non-empty -> return of(parameter)
6. ELSE -> return of(String.valueOf(state.isEnabled()))
```

Step 5 uses the convention that a strategy parameter named `"value"` holds the string value. This is documented in the adapter's Javadoc and the FlagZen docs site. See ADR-022.

Step 6 falls back to the enabled state as a string (`"true"` / `"false"`), ensuring that `getString` always returns a value when the feature exists. This enables the `FlagProvider` default numeric parsing to work for features configured with a numeric `"value"` parameter.

### 2.4 Boolean Value Strategy

```
Input: key
Output: Optional<Boolean>

1. Resolve Feature from key via FeatureLookup
2. IF feature not found -> return empty
3. Get FeatureState from FeatureManager
4. IF state is null -> return empty
5. return of(state.isEnabled())
```

Native boolean resolution. No string parsing needed.

### 2.5 Context Handling

All context-aware overloads delegate to their non-context counterparts:

```
getString(key, ctx)   -> getString(key)   // ctx ignored
getBoolean(key, ctx)  -> getBoolean(key)  // ctx ignored
```

A one-time INFO log is emitted on the first context-aware call:

```
"TogglzFlagProvider does not support explicit EvaluationContext.
Configure a Togglz UserProvider for user targeting."
```

This is logged via JUL at INFO level, using `Logger.getLogger(TogglzFlagProvider.class.getName())`.

---

## 3. Thread Safety

| Component | Thread Safety | Rationale |
| --- | --- | --- |
| `LaunchDarklyFlagProvider` | Thread-safe | `LDClient` is thread-safe per SDK docs. No mutable state in adapter. |
| `EvaluationContextMapper` (LD) | Thread-safe | Stateless, pure function. |
| `TogglzFlagProvider` | Thread-safe | `FeatureManager` is thread-safe per Togglz docs. |
| `FeatureLookup` | Thread-safe | `ConcurrentHashMap` with lazy population via `computeIfAbsent`. |
| Anonymous `LDContext` singleton | Thread-safe | Immutable object, allocated once. |
