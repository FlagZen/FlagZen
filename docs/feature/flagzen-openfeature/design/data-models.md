# Data Models -- flagzen-openfeature

## 1. EvaluationContext Mapping

### Field Mapping

|      FlagZen `EvaluationContext`       | OpenFeature `EvaluationContext` |                             Notes                              |
| -------------------------------------- | ------------------------------- | -------------------------------------------------------------- |
| `targetingKey()` (String, nullable)    | `setTargetingKey(String)`       | 1:1 mapping. Null targeting key means omit the call.           |
| `attributes()` (`Map<String, Object>`) | `add(String, Value)` per entry  | Each attribute value converted to OpenFeature `Value` wrapper. |

### Attribute Type Conversion

FlagZen stores attributes as `Map<String, Object>`. OpenFeature's `Value` type is a tagged union. The mapper converts based on runtime type of each attribute value:

| Java runtime type | OpenFeature `Value` factory |                                              Notes                                               |
| ----------------- | --------------------------- | ------------------------------------------------------------------------------------------------ |
| `String`          | `new Value(String)`         | Direct                                                                                           |
| `Boolean`         | `new Value(Boolean)`        | Direct                                                                                           |
| `Integer`         | `new Value(Integer)`        | Direct                                                                                           |
| `Long`            | `new Value(Integer)`        | Narrowing -- lossy for values outside int range. Log warning for overflow.                       |
| `Double`          | `new Value(Double)`         | Direct                                                                                           |
| `List<?>`         | `new Value(List<Value>)`    | Recursive: each element converted to `Value`.                                                    |
| `Map<String, ?>`  | `new Value(Structure)`      | Recursive: each entry converted to `Value`. Structure is OpenFeature's map type.                 |
| `Instant`         | `new Value(Instant)`        | OpenFeature SDK supports `Instant` natively in `Value`.                                          |
| Any other type    | **Skipped**                 | Log warning: "Unsupported attribute type {type} for key '{key}'". Attribute omitted from output. |

### Conversion Rules

1. **Null targeting key**: Do not call `setTargetingKey` on the OpenFeature builder. The resulting context has no targeting key.
2. **Empty attributes map**: Produce an OpenFeature context with targeting key only (or empty if no targeting key either).
3. **Null attribute value**: Skip the entry, log warning.
4. **Nested collections**: `List` and `Map` values are converted recursively. Elements that are unsupported types are skipped with a warning.
5. **Long narrowing**: OpenFeature `Value` accepts `Integer` but not `Long` directly. Check if the long value fits in int range. If yes, narrow safely. If no, log warning and skip (or convert to `Double` if the implementation prefers -- this is an implementation choice for the crafter).

## 2. Reason-Based Absence Detection

### OpenFeature `FlagEvaluationDetails` Fields Used

|        Field        |        Type        |                       Usage                        |
| ------------------- | ------------------ | -------------------------------------------------- |
| `getValue()`        | `T`                | The resolved value (or default sentinel if absent) |
| `getReason()`       | `String`           | Why this value was returned                        |
| `getErrorCode()`    | `ErrorCode` (enum) | Non-null when evaluation failed                    |
| `getErrorMessage()` | `String`           | Human-readable error description                   |

### Decision Logic

```
Input: FlagEvaluationDetails<T> details
Output: Optional<T> (or OptionalInt/OptionalLong/OptionalDouble)

IF details.getErrorCode() != null THEN
    return empty
    (log at DEBUG: "Flag '{}' evaluation error: {} - {}", key, errorCode, errorMessage)

IF details.getReason() == "DEFAULT" THEN
    return empty
    (this means the SDK returned the client-supplied default without real resolution)

ELSE
    return of(details.getValue())
```

### Reason Values and Their Meaning

|      Reason       |                              Meaning                              | Adapter returns |
| ----------------- | ----------------------------------------------------------------- | --------------- |
| `TARGETING_MATCH` | Flag resolved via targeting rules                                 | `of(value)`     |
| `SPLIT`           | Flag resolved via percentage rollout                              | `of(value)`     |
| `DISABLED`        | Flag exists but is disabled; provider returned its disabled value | `of(value)`     |
| `STATIC`          | Flag resolved with a static/fixed value                           | `of(value)`     |
| `CACHED`          | Flag resolved from cache                                          | `of(value)`     |
| `DEFAULT`         | No real resolution; SDK returned client-supplied default          | `empty`         |
| `ERROR`           | Evaluation error (typically also has errorCode set)               | `empty`         |
| `UNKNOWN`         | Provider did not supply a reason but did resolve                  | `of(value)`     |
| null              | Provider did not set reason field                                 | `of(value)`     |
| Any custom string | Provider-specific reason                                          | `of(value)`     |

The key insight: only `DEFAULT` reason (without a real resolution) and error conditions produce `empty`. Everything else -- including `DISABLED`, `UNKNOWN`, and null reason -- is treated as a real resolution. This is the most permissive and correct interpretation, since a provider that returns a value with any non-DEFAULT reason has genuinely resolved the flag.

### Default Sentinel Values

The adapter must pass a default value to OpenFeature's detail methods. These sentinels are never returned to the FlagZen caller (the reason-based check catches them):

|  Type   | Sentinel |                          Rationale                           |
| ------- | -------- | ------------------------------------------------------------ |
| String  | `""`     | Empty string -- any real flag value is non-empty in practice |
| Boolean | `false`  | Arbitrary; irrelevant because reason check supersedes        |
| Integer | `0`      | Arbitrary; irrelevant because reason check supersedes        |
| Double  | `0.0`    | Arbitrary; irrelevant because reason check supersedes        |

The sentinel value choice does not matter because the adapter never returns it directly. The `reason` field determines whether the resolution was real.

## 3. Thread Safety

|         Component         | Thread Safety |                                             Rationale                                             |
| ------------------------- | ------------- | ------------------------------------------------------------------------------------------------- |
| `OpenFeatureFlagProvider` | Thread-safe   | Delegates to `Client` which is thread-safe per OpenFeature spec. No mutable state in the adapter. |
| `EvaluationContextMapper` | Thread-safe   | Stateless, pure function.                                                                         |
| `Client` (OpenFeature)    | Thread-safe   | Per OpenFeature specification, clients are safe for concurrent use.                               |
