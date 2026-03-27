<!-- markdownlint-disable MD024 -->

# Typed Variants User Stories

## US-M2-01: FeatureType Enum and @Feature Type Attribute

### Problem

Carlos Mendes is a senior Java developer who uses FlagZen for polymorphic dispatch in a payments service. He currently encodes integer flag values as strings (e.g., `@Variant("3")` for a retry count) because `@Feature` only supports string-typed dispatch. He finds it error-prone to map numeric flag values to string variants and wants the annotation model to express the flag's actual type.

### Who

- Java developer | Backend microservice with integer, boolean, long, and double-valued flags | Wants type-safe dispatch without string encoding

### Solution

Add a `FeatureType` enum (STRING, INT, LONG, BOOLEAN, DOUBLE) and a `type` attribute to the `@Feature` annotation, defaulting to STRING for backward compatibility.

### Domain Examples

#### 1: Retry strategy with integer flag

Carlos annotates his RetryStrategy interface with `@Feature(value = "max-retries", type = FeatureType.INT)`. The annotation compiles. Existing features without `type` continue to default to `FeatureType.STRING`.

#### 2: Dark mode with boolean flag

Mei Chen annotates her DarkMode interface with `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)`. The annotation compiles and signals boolean dispatch.

#### 3: Backward compatibility

Carlos's existing `@Feature("checkout-flow")` interface (no type specified) continues to work identically. The processor treats it as `FeatureType.STRING`.

#### 4: Rate limiter with long flag

Carlos annotates RateLimiter with `@Feature(value = "rate-limit", type = FeatureType.LONG)`. The annotation compiles.

### UAT Scenarios (BDD)

#### Scenario: Feature with explicit INT type compiles

Given Carlos annotates RetryStrategy with `@Feature(value = "max-retries", type = FeatureType.INT)`
When the project compiles
Then the `@Feature` annotation is processed without errors
And the FeatureModel records featureType as INT

#### Scenario: Feature with explicit BOOLEAN type compiles

Given Mei Chen annotates DarkMode with `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)`
When the project compiles
Then the FeatureModel records featureType as BOOLEAN

#### Scenario: Feature without type defaults to STRING

Given Carlos has an existing `@Feature("checkout-flow")` without type attribute
When the project compiles
Then the FeatureModel records featureType as STRING
And existing proxy generation behavior is unchanged

#### Scenario: Feature with LONG and DOUBLE types compile

Given Carlos annotates RateLimiter with `@Feature(value = "rate-limit", type = FeatureType.LONG)`
And Mei Chen annotates SamplingStrategy with `@Feature(value = "sampling-ratio", type = FeatureType.DOUBLE)`
When the project compiles
Then both FeatureModels record the correct featureType

### Acceptance Criteria

- [ ] `FeatureType` enum exists with constants STRING, INT, LONG, BOOLEAN, DOUBLE
- [ ] `@Feature` annotation has `type()` attribute defaulting to `FeatureType.STRING`
- [ ] Annotation processor reads and stores the type in FeatureModel
- [ ] Existing @Feature annotations without type attribute compile and behave identically to before

### Outcome KPIs

- **Who**: Java developers using FlagZen
- **Does what**: Declare feature type at annotation level instead of encoding in string values
- **By how much**: 100% of typed features use explicit FeatureType
- **Measured by**: Annotation processor correctly identifies feature type for all processed @Feature annotations
- **Baseline**: All features are implicitly STRING

### Technical Notes

- FeatureType enum lives in `com.flagzen` package
- `@Feature.type()` has retention CLASS (same as existing attributes)
- FeatureModel gains a `featureType` field populated during processing
- No runtime dependency on FeatureType -- it drives compile-time code generation only

---

## US-M2-02: Typed @Variant Attributes (intValue, booleanValue, longValue, doubleValue)

### Problem

Carlos Mendes has a retry strategy that dispatches based on the integer value of a "max-retries" flag. Currently he must use `@Variant("3")` and rely on string matching, which is misleading -- the flag provider returns an integer, not the string "3". He wants `@Variant(intValue = 3)` to express the variant's match value in its actual type. Similarly, Mei Chen wants `@Variant(doubleValue = @CloseTo(value = 0.3))` for approximate double matching to handle floating-point imprecision from JS backends.

### Who

- Java developer | Has integer, boolean, long, or double-typed feature flags | Wants annotation attributes that match the flag's actual type

### Solution

Add `intValue`, `booleanValue`, `longValue`, and `doubleValue` attributes to the `@Variant` annotation. Add `@CloseTo(value, delta)` annotation for approximate double matching (default delta = 1e-10). Exactly one of `value`, `intValue`, `booleanValue`, `longValue`, or `doubleValue` is used per annotation instance (enforced by processor in US-M2-04).

### Domain Examples

#### 1: Integer variants

Carlos annotates ConservativeRetry with `@Variant(intValue = 3)` and AggressiveRetry with `@Variant(intValue = 10)` for his max-retries feature.

#### 2: Long variants

Carlos annotates StandardLimit with `@Variant(longValue = 1000)` and HighVolumeLimit with `@Variant(longValue = 50000)` for his rate-limit feature.

#### 3: Double variants with @CloseTo

Mei Chen annotates LowSampling with `@Variant(doubleValue = @CloseTo(value = 0.1))` using default delta 1e-10, and MediumSampling with `@Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01))` using explicit delta.

#### 4: String variant unchanged

Carlos's existing `@Variant("CLASSIC")` on ClassicCheckout continues to work for string-typed features.

### UAT Scenarios (BDD)

#### Scenario: Variant with intValue compiles

Given Carlos annotates ConservativeRetry with `@Variant(intValue = 3)`
When the project compiles
Then the annotation is processed without errors
And VariantModel records the variant value as integer 3

#### Scenario: Variant with longValue compiles

Given Carlos annotates StandardLimit with `@Variant(longValue = 1000)`
When the project compiles
Then VariantModel records the variant value as long 1000

#### Scenario: Variant with doubleValue and @CloseTo default delta

Given Mei Chen annotates LowSampling with `@Variant(doubleValue = @CloseTo(value = 0.1))`
When the project compiles
Then VariantModel records double 0.1 with delta 1e-10

#### Scenario: Variant with @CloseTo explicit delta

Given Mei Chen annotates MediumSampling with `@Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01))`
When the project compiles
Then VariantModel records double 0.5 with delta 0.01

#### Scenario: Existing string variant unchanged

Given Carlos has `@Variant("CLASSIC")` on ClassicCheckout
When the project compiles
Then the annotation is processed with value "CLASSIC" as before

### Acceptance Criteria

- [ ] `@Variant` annotation has `intValue()` attribute with appropriate default sentinel
- [ ] `@Variant` annotation has `booleanValue()` attribute with appropriate default sentinel
- [ ] `@Variant` annotation has `longValue()` attribute with appropriate default sentinel
- [ ] `@Variant` annotation has `doubleValue()` attribute of type `@CloseTo` with appropriate default sentinel
- [ ] `@CloseTo` annotation exists with `value()` (double) and `delta()` (double, default 1e-10)
- [ ] VariantModel can store typed values (int, boolean, long, double + delta) alongside string value
- [ ] Existing `@Variant(value = "...")` annotations compile and work as before
- [ ] Annotation attribute defaults allow the processor to detect which attribute was explicitly set

### Outcome KPIs

- **Who**: Java developers with typed feature flags
- **Does what**: Express variant match values in their actual type at annotation level
- **By how much**: Eliminates string encoding for all int/boolean/long/double variant values
- **Measured by**: VariantModel correctly stores typed values for all processed @Variant annotations
- **Baseline**: All variant values are strings

### Technical Notes

- Java annotations cannot have null defaults. Use sentinels: `intValue` default `Integer.MIN_VALUE`, `longValue` default `Long.MIN_VALUE`. The `booleanValue` needs a tri-state approach. `doubleValue` default could be a `@CloseTo` with sentinel value. The crafter decides the sentinel strategy.
- `@CloseTo` is a nested annotation type. `@Variant(doubleValue = @CloseTo(value = 0.3))` is the usage syntax.
- `@Variant` remains `@Repeatable` with `@Variants` container
- Retention stays CLASS

---

## US-M2-03: @WhenTrue / @WhenFalse Convenience Annotations

### Problem

Mei Chen has a dark-mode feature with exactly two states: on and off. She finds `@Variant(booleanValue = true)` verbose for such a common pattern. She wants `@WhenTrue` and `@WhenFalse` as readable convenience annotations. For multi-feature classes, she needs `@WhenTrue(of = DarkMode.class)` to specify which feature the boolean applies to.

### Who

- Java developer | Has boolean-typed features | Wants concise, readable boolean variant annotations

### Solution

Add `@WhenTrue` and `@WhenFalse` annotations. The processor treats them identically to `@Variant(booleanValue = true)` and `@Variant(booleanValue = false)` respectively. Both support `of = X.class` for multi-feature classes.

### Domain Examples

#### 1: Simple @WhenTrue/@WhenFalse

Mei Chen annotates DarkModeOn with `@WhenTrue` and DarkModeOff with `@WhenFalse`. Both implement DarkMode. Processor treats them as `@Variant(booleanValue = true/false)`.

#### 2: Multi-feature with of=

Carlos annotates DarkOnMaintenanceOff with `@WhenTrue(of = DarkMode.class)` and `@WhenFalse(of = MaintenanceMode.class)`. The class implements both interfaces. Processor registers the correct boolean value per feature.

#### 3: Mixed @WhenTrue and @Variant(booleanValue = false)

DarkModeOn uses `@WhenTrue`, DarkModeOff uses `@Variant(booleanValue = false)`. Both are valid and can coexist on the same feature.

### UAT Scenarios (BDD)

#### Scenario: @WhenTrue equivalent to @Variant(booleanValue = true)

Given Mei Chen annotates DarkModeOn with `@WhenTrue`
When the annotation processor runs
Then the processor treats it identically to `@Variant(booleanValue = true)`

#### Scenario: @WhenFalse equivalent to @Variant(booleanValue = false)

Given Mei Chen annotates DarkModeOff with `@WhenFalse`
When the annotation processor runs
Then the processor treats it identically to `@Variant(booleanValue = false)`

#### Scenario: @WhenTrue with of= for multi-feature class

Given Carlos annotates DarkOnMaintenanceOff with `@WhenTrue(of = DarkMode.class)` and `@WhenFalse(of = MaintenanceMode.class)`
When the annotation processor runs
Then booleanValue true is registered for DarkMode
And booleanValue false is registered for MaintenanceMode

#### Scenario: @WhenTrue infers target on single-feature class

Given Mei Chen annotates DarkModeOn implementing only DarkMode with `@WhenTrue`
When the annotation processor runs
Then the processor infers the target feature as DarkMode

### Acceptance Criteria

- [ ] `@WhenTrue` annotation exists (retention CLASS, target TYPE)
- [ ] `@WhenFalse` annotation exists (retention CLASS, target TYPE)
- [ ] Both have `of()` attribute with `void.class` sentinel (same as `@Variant.of`)
- [ ] Processor normalizes `@WhenTrue` to `@Variant(booleanValue = true)` before validation
- [ ] Processor normalizes `@WhenFalse` to `@Variant(booleanValue = false)` before validation
- [ ] `@WhenTrue(of = X.class)` handled identically to `@Variant(of = X.class, booleanValue = true)`
- [ ] Mixed usage of `@WhenTrue` and `@Variant(booleanValue = ...)` on same feature compiles

### Outcome KPIs

- **Who**: Java developers with boolean features
- **Does what**: Use concise `@WhenTrue`/`@WhenFalse` instead of verbose `@Variant(booleanValue = ...)`
- **By how much**: Boolean variant annotations are 50% shorter (6 chars vs 28 chars)
- **Measured by**: Processor treats both annotation forms identically
- **Baseline**: Only `@Variant(booleanValue = ...)` syntax available

### Technical Notes

- `@WhenTrue` and `@WhenFalse` are separate annotations, not aliases within `@Variant`
- Both need `@Repeatable` if a class implements multiple boolean features (each with `of=`)
- The processor must discover and normalize these annotations in the same processing round as `@Variant`
- Retention and target match `@Variant` (CLASS, TYPE)

---

## US-M2-04: Compile-Time Type Validation (INT, BOOLEAN, LONG, DOUBLE)

### Problem

Carlos Mendes accidentally annotates a variant with `@Variant(value = "3")` on an `@Feature(type = FeatureType.INT)` interface. Without compile-time validation, this mismatch causes a silent dispatch failure at runtime -- the proxy looks up integer 3 but the variant is registered under string "3". He needs the processor to catch this at compile time with a clear error message.

### Who

- Java developer | Annotating typed features | Needs compile-time safety against type mismatches

### Solution

The annotation processor validates that all `@Variant` annotations (including `@WhenTrue`/`@WhenFalse`) for a feature use the attribute matching the `@Feature`'s declared type. Mismatches produce compilation errors with actionable fix suggestions. BOOLEAN features with REQUIRED strategy must have both true and false covered. Duplicate typed values are rejected.

### Domain Examples

#### 1: String variant on INT feature

Carlos has `@Feature(type = FeatureType.INT)` on RetryStrategy and `@Variant(value = "3")` on ConservativeRetry. Processor emits: "Feature 'max-retries' declares type INT but variant ConservativeRetry uses string value. Use @Variant(intValue = 3) instead."

#### 2: intValue on BOOLEAN feature

Mei Chen has `@Feature(type = FeatureType.BOOLEAN)` on DarkMode and `@Variant(intValue = 1)` on DarkModeOn. Processor emits: "Feature 'dark-mode' declares type BOOLEAN but variant DarkModeOn uses intValue. Use @Variant(booleanValue = true) instead."

#### 3: Duplicate intValue

Two variants both declare `intValue = 3` for the same INT feature. Processor rejects with duplicate variant value error.

#### 4: Boolean REQUIRED incomplete

`@Feature(type = BOOLEAN, fallback = REQUIRED)` with only a `true` variant and no `@DefaultVariant`. Processor emits: "REQUIRED BOOLEAN feature requires variants for both true and false."

#### 5: intValue on DOUBLE feature

A variant uses `@Variant(intValue = 1)` on a DOUBLE feature. Processor emits suggestion to use `@Variant(doubleValue = @CloseTo(...))`.

### UAT Scenarios (BDD)

#### Scenario: Type mismatch -- string variant on INT feature

Given Carlos has `@Feature(value = "max-retries", type = FeatureType.INT)`
And ConservativeRetry annotated with `@Variant(value = "3")`
When the annotation processor runs
Then compilation fails with error on ConservativeRetry
And the error message includes "type INT" and "string value" and "Use @Variant(intValue = 3)"

#### Scenario: Type mismatch -- intValue on BOOLEAN feature

Given Mei Chen has `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)`
And DarkModeOn annotated with `@Variant(intValue = 1)`
When the annotation processor runs
Then compilation fails suggesting `@Variant(booleanValue = true)`

#### Scenario: Duplicate intValue rejected

Given Carlos has two variants both declaring `@Variant(intValue = 3)` for the same INT feature
When the annotation processor runs
Then compilation fails with duplicate variant value error for intValue 3

#### Scenario: Boolean REQUIRED incomplete

Given `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)`
And only DarkModeOn with `@WhenTrue`
And no variant for false and no `@DefaultVariant`
When the annotation processor runs
Then compilation fails requiring both true and false variants

#### Scenario: Boolean REQUIRED satisfied by DefaultVariant

Given `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)`
And DarkModeOn with `@WhenTrue`
And FallbackDarkMode with `@DefaultVariant`
When the annotation processor runs
Then compilation succeeds

### Acceptance Criteria

- [ ] Processor rejects `@Variant` with wrong attribute for the `@Feature` type, with actionable error message
- [ ] Processor rejects mixed variant attributes within same feature
- [ ] Processor rejects duplicate typed values (intValue, booleanValue, longValue, doubleValue)
- [ ] Processor enforces BOOLEAN REQUIRED completeness (both true + false, or `@DefaultVariant` covers gap)
- [ ] Error messages include feature name, variant class name, and suggested fix
- [ ] All existing validation rules continue to work for STRING features
- [ ] `@WhenTrue`/`@WhenFalse` validated the same as `@Variant(booleanValue = ...)`

### Outcome KPIs

- **Who**: Java developers annotating typed features
- **Does what**: Receive compile-time errors for type mismatches instead of runtime dispatch failures
- **By how much**: 100% of type mismatches caught at compile time (zero runtime surprises)
- **Measured by**: Processor emits ERROR diagnostic for every type-inconsistent @Variant annotation
- **Baseline**: No type validation exists (string-only)

### Technical Notes

- Validation runs in the same processing round as existing validations
- Error messages use `Messager.printMessage(Kind.ERROR, ...)` on the offending element
- Boolean REQUIRED check extends existing REQUIRED/enum completeness logic
- Sentinel detection logic must be robust -- see US-M2-02 technical notes
- LONG and DOUBLE validation follows the same pattern as INT and BOOLEAN

---

## US-M2-05: INT and BOOLEAN Proxy Dispatch + getInt/getBoolean

### Problem

Carlos Mendes's RetryStrategy feature dispatches on an integer "max-retries" flag. Currently, the generated proxy calls `getString()` and matches against string variant values, requiring his flag provider to return "3" instead of the integer 3. He wants the proxy to call `getInt()` and match against integer variant values directly. Mei Chen wants the same for boolean dispatch with `getBoolean()`.

### Who

- Java developer | Has INT or BOOLEAN-typed feature compiling successfully | Wants runtime dispatch on typed flag values

### Solution

The proxy generator produces dispatch logic that calls `FlagProvider.getInt(key)` for INT features (using `Map<Integer, Supplier<T>>`) and `FlagProvider.getBoolean(key)` for BOOLEAN features (using `Map<Boolean, Supplier<T>>`). FlagProvider gains `getInt()` returning `OptionalInt` and `getBoolean()` returning `Optional<Boolean>` as default methods parsing from `getString()`. Both have context-aware overloads.

### Domain Examples

#### 1: Integer dispatch

Carlos's flag provider returns `OptionalInt.of(3)` for `getInt("max-retries")`. The proxy selects ConservativeRetry.

#### 2: Boolean dispatch

Mei Chen's flag provider returns `Optional.of(true)` for `getBoolean("dark-mode")`. The proxy selects DarkModeOn.

#### 3: Unmatched integer triggers fallback

Flag provider returns 99, which matches no variant. EXCEPTION strategy throws `UnmatchedVariantException` listing known values [3, 10].

#### 4: Dynamic dispatch on flag change

Flag provider changes from 3 to 10 at runtime. Next proxy method call dispatches to AggressiveRetry.

### UAT Scenarios (BDD)

#### Scenario: INT proxy dispatches on integer value

Given Carlos has RetryStrategy with FeatureType.INT and variants for intValue 3 and 10
And the FlagProvider returns `OptionalInt.of(3)` for `getInt("max-retries")`
When Carlos resolves RetryStrategy via FeatureDispatcher
Then ConservativeRetry handles the method call

#### Scenario: BOOLEAN proxy dispatches on true

Given Mei Chen has DarkMode with FeatureType.BOOLEAN
And DarkModeOn for booleanValue true, DarkModeOff for booleanValue false
And the FlagProvider returns `Optional.of(true)` for `getBoolean("dark-mode")`
When Mei Chen resolves DarkMode
Then DarkModeOn handles the method call

#### Scenario: Proxy follows runtime flag changes

Given the FlagProvider initially returns 3 for "max-retries"
When the flag value changes to 10
And Carlos calls a method on the resolved proxy
Then AggressiveRetry handles the call

#### Scenario: Unmatched integer with EXCEPTION fallback

Given RetryStrategy has fallback EXCEPTION
And the FlagProvider returns `OptionalInt.of(99)` for `getInt("max-retries")`
When Carlos resolves RetryStrategy
Then `UnmatchedVariantException` is thrown listing known values [3, 10]

#### Scenario: FlagProvider.getInt parses from getString

Given a FlagProvider that implements only `getString()` returning "42"
When `getInt("some-key")` is called via the default method
Then `OptionalInt.of(42)` is returned

#### Scenario: FlagProvider.getBoolean returns empty for non-boolean string

Given a FlagProvider returning "maybe" for `getString("flag")`
When `getBoolean("flag")` is called via the default method
Then `Optional.empty()` is returned

### Acceptance Criteria

- [ ] Generated proxy for INT features calls `FlagProvider.getInt()` returning `OptionalInt`
- [ ] Generated proxy for BOOLEAN features calls `FlagProvider.getBoolean()` returning `Optional<Boolean>`
- [ ] Proxy variant map uses `Map<Integer, Supplier<T>>` for INT, `Map<Boolean, Supplier<T>>` for BOOLEAN
- [ ] `FlagProvider.getInt(String key)` is a default method parsing from `getString()`
- [ ] `FlagProvider.getBoolean(String key)` is a default method parsing from `getString()`
- [ ] Both have context-aware overloads: `getInt(String, EvaluationContext)`, `getBoolean(String, EvaluationContext)`
- [ ] Parse failure in default methods returns empty optional, not exception
- [ ] Unmatched typed values trigger fallback strategy identically to unmatched string values
- [ ] Dynamic dispatch works -- proxy re-evaluates on each method call

### Outcome KPIs

- **Who**: Java developers with INT/BOOLEAN feature flags
- **Does what**: Dispatch on actual typed values without string encoding
- **By how much**: Zero string-to-type encoding workarounds for INT/BOOLEAN features
- **Measured by**: Generated proxy calls getInt()/getBoolean() and correctly dispatches
- **Baseline**: All dispatch goes through getString() with string matching

### Technical Notes

- ProxyGenerator needs INT and BOOLEAN code paths: call typed method, match against typed keys
- FlagProvider typed methods are default methods -- no breaking change to existing implementations
- Context-aware overloads follow same pattern as existing `getString(key, context)`
- `InMemoryFlagProvider` stores string values; typed proxies parse via default getInt/getBoolean methods
- `@PinFlag` continues to specify variant values as strings; typed proxies parse via FlagProvider default methods
- getBoolean parsing: only "true"/"false" (case-insensitive) are valid; other strings return empty. Stricter than `Boolean.parseBoolean`.

---

## US-M2-06: Typed Dispatch with Evaluation Context

### Problem

Carlos Mendes uses evaluation context (M1) for tenant-scoped flag resolution. He expects typed dispatch to work seamlessly with explicit contexts, block-scoped contexts (`FlagContext.run`), and context accessors. Without this, typed dispatch would ignore contextual targeting -- a regression from string-typed features that already support context.

### Who

- Java developer | Uses typed dispatch AND evaluation context | Expects context to work identically for typed and string features

### Solution

Generated typed proxies pass evaluation context to typed FlagProvider methods (`getInt(key, ctx)`, `getBoolean(key, ctx)`, etc.) following the same resolution chain as string dispatch: explicit > accessor > scoped > default.

### Domain Examples

#### 1: INT dispatch with explicit context

Carlos resolves RetryStrategy with explicit context targeting plan "enterprise". Flag provider returns 10 for enterprise, 3 for free. AggressiveRetry is selected.

#### 2: BOOLEAN dispatch with block-scoped context

Mei Chen runs `FlagContext.run(ctx, () -> ...)` with preference "dark". Inside the block, DarkMode resolves to DarkModeOn.

#### 3: Explicit context overrides scoped context

Inside a `FlagContext.run` block with plan "free", Carlos resolves RetryStrategy with explicit context plan "enterprise". Explicit wins -- AggressiveRetry selected.

### UAT Scenarios (BDD)

#### Scenario: INT dispatch with explicit EvaluationContext

Given a context-aware FlagProvider for "max-retries" returning 3 for plan "free" and 10 for plan "enterprise"
When Carlos resolves RetryStrategy with explicit context targeting plan "enterprise"
Then AggressiveRetry is selected

#### Scenario: BOOLEAN dispatch with explicit context

Given a context-aware FlagProvider returning true for "dark-mode" when preference is "dark"
When Mei Chen resolves DarkMode with context attribute preference "dark"
Then DarkModeOn is selected

#### Scenario: Typed dispatch with block-scoped context

Given a context-aware FlagProvider for "max-retries" (3 for free, 10 for enterprise)
When Carlos runs `FlagContext.run` with context targeting plan "enterprise"
And resolves RetryStrategy inside the block
Then AggressiveRetry is selected
And after the block exits the scoped context is cleared

#### Scenario: Explicit context overrides scoped context

Given a scoped context block with plan "free"
When Carlos resolves RetryStrategy with explicit context targeting plan "enterprise" inside the block
Then the explicit context wins and AggressiveRetry is selected

#### Scenario: Typed dispatch with context accessor

Given a ContextAccessor providing context with plan "enterprise"
And a context-aware FlagProvider for "max-retries" (3 for free, 10 for enterprise)
When Carlos resolves RetryStrategy without explicit context
Then the accessor-provided context is used and AggressiveRetry is selected

### Acceptance Criteria

- [ ] Generated typed proxies pass context to typed FlagProvider methods (getInt/getBoolean with context)
- [ ] Explicit context parameter takes precedence over scoped and accessor contexts
- [ ] Block-scoped context (`FlagContext.run`) works with typed dispatch
- [ ] Context accessor (ContextAccessor SPI) works with typed dispatch
- [ ] Resolution chain (explicit > accessor > scoped > default) is identical to string dispatch
- [ ] No context (null/absent) falls back to contextless typed method call

### Outcome KPIs

- **Who**: Java developers using typed dispatch with evaluation context
- **Does what**: Get context-aware typed dispatch without special handling
- **By how much**: Typed dispatch follows identical context resolution as string dispatch (100% parity)
- **Measured by**: All context resolution scenarios pass for typed features
- **Baseline**: Context integration not yet implemented for typed dispatch

### Technical Notes

- Typed FlagProvider methods already have context-aware overloads (from US-M2-05)
- Generated proxy reads context from `FlagContext.current()` or explicit parameter (same as M1 proxy logic)
- No new context infrastructure -- this story verifies the typed proxy code path integrates with existing M1 context resolution
- Depends on M1 (EvaluationContext) being complete

---

## US-M2-07: LONG/DOUBLE Dispatch, @CloseTo, and getLong/getDouble

### Problem

Carlos Mendes has a rate-limiting service with a "rate-limit" flag holding a long value (e.g., 5000000000) and Mei Chen has a "sampling-ratio" flag with a double value (e.g., 0.1). They want `@Variant(longValue = ...)` and `@Variant(doubleValue = @CloseTo(...))` for typed polymorphic dispatch, and `getLong()`/`getDouble()` on FlagProvider for the conditional API. The `@CloseTo` annotation handles floating-point imprecision -- e.g., a JS-based flag backend where 0.1 + 0.2 != 0.3.

### Who

- Java developer | Uses FlagProvider with long/double flag values | Wants typed dispatch and complete typed accessor set

### Solution

Proxy generator produces LONG dispatch (exact match via `Map<Long, Supplier<T>>`) and DOUBLE dispatch (iterate variants, `Math.abs(flagValue - variantValue) <= delta`). FlagProvider gains `getLong()` returning `OptionalLong` and `getDouble()` returning `OptionalDouble` as default methods parsing from `getString()`.

### Domain Examples

#### 1: Long dispatch

Carlos's flag provider returns `OptionalLong.of(50000)` for `getLong("rate-limit")`. Proxy selects HighVolumeLimit.

#### 2: Double dispatch with default delta

Mei Chen's flag provider returns `OptionalDouble.of(0.0999999999)` for `getDouble("sampling-ratio")`. Proxy iterates variants: `Math.abs(0.0999999999 - 0.1)` = 9.99e-11 which is <= 1e-10 (default delta). LowSampling selected.

#### 3: Double dispatch with explicit delta

Flag provider returns `OptionalDouble.of(0.505)`. Variant MediumSampling has `@CloseTo(value = 0.5, delta = 0.01)`. `Math.abs(0.505 - 0.5)` = 0.005 <= 0.01. MediumSampling selected.

#### 4: getLong overflow returns empty

Flag "big-number" has value "999999999999999999999" (exceeds `Long.MAX_VALUE`). `flags.getLong("big-number")` returns `OptionalLong.empty()`.

### UAT Scenarios (BDD)

#### Scenario: LONG proxy dispatches via map lookup

Given Carlos has RateLimiter with FeatureType.LONG and variants longValue 1000 and 50000
And the FlagProvider returns `OptionalLong.of(50000)` for `getLong("rate-limit")`
When Carlos resolves RateLimiter via FeatureDispatcher
Then HighVolumeLimit handles the method call

#### Scenario: DOUBLE proxy dispatches via approximate matching

Given Mei Chen has SamplingStrategy with FeatureType.DOUBLE
And LowSampling for `@CloseTo(value = 0.1)` with default delta 1e-10
And the FlagProvider returns `OptionalDouble.of(0.0999999999)`
When Mei Chen resolves SamplingStrategy
Then LowSampling is selected

#### Scenario: DOUBLE proxy matches explicit delta

Given MediumSampling for `@CloseTo(value = 0.5, delta = 0.01)`
And the FlagProvider returns `OptionalDouble.of(0.505)`
When Mei Chen resolves SamplingStrategy
Then MediumSampling is selected

#### Scenario: getLong parses valid long

Given the FlagProvider has flag "rate-limit" with string value "5000000000"
When Carlos calls `flags.getLong("rate-limit")`
Then `OptionalLong.of(5000000000L)` is returned

#### Scenario: getLong returns empty for overflow

Given the FlagProvider has flag "rate-limit" with string value "999999999999999999999"
When Carlos calls `flags.getLong("rate-limit")`
Then `OptionalLong.empty()` is returned

#### Scenario: getDouble parses valid double

Given the FlagProvider has flag "sampling-ratio" with string value "0.75"
When Carlos calls `flags.getDouble("sampling-ratio")`
Then `OptionalDouble.of(0.75)` is returned

### Acceptance Criteria

- [ ] Generated proxy for LONG features calls `FlagProvider.getLong()` with `Map<Long, Supplier<T>>`
- [ ] Generated proxy for DOUBLE features calls `FlagProvider.getDouble()` and iterates variants with `Math.abs(flagValue - variantValue) <= delta`
- [ ] `FlagProvider.getLong(String key)` default method returning `OptionalLong`, parses from `getString()`
- [ ] `FlagProvider.getDouble(String key)` default method returning `OptionalDouble`, parses from `getString()`
- [ ] Both have context-aware overloads
- [ ] Parse failures and absent flags return empty optionals
- [ ] Numeric overflow returns empty, not exception
- [ ] Compile-time validation: `longValue`/`doubleValue` matches `@Feature(type = LONG/DOUBLE)`

### Outcome KPIs

- **Who**: Java developers with long/double-valued feature flags
- **Does what**: Dispatch on and access long/double flag values with type safety
- **By how much**: Complete typed dispatch for all 5 types (STRING, INT, LONG, BOOLEAN, DOUBLE)
- **Measured by**: getLong/getDouble return correct values; proxy dispatches correctly
- **Baseline**: Manual getString + parsing; no long/double polymorphic dispatch

### Technical Notes

- `FlagProvider.getLong()` returns `OptionalLong`, `getDouble()` returns `OptionalDouble` -- JDK primitive optional types avoid boxing
- `@CloseTo` is necessary because flag backends (especially JS-based) may return imprecise doubles
- DOUBLE proxy dispatch: iterate variants in declaration order, first match wins
- LONG proxy dispatch: exact match via `Map<Long, Supplier<T>>`
- `@CloseTo.delta()` defaults to 1e-10, overridable per variant

---

## US-M2-08: Conditional API (Non-Polymorphic Typed Accessors)

### Problem

Carlos Mendes sometimes needs a simple boolean flag check without polymorphic dispatch -- just `if (flags.getBoolean("feature-x"))`. Currently FlagProvider only exposes `getString()`, forcing him to parse manually: `Boolean.parseBoolean(flags.getString("feature-x").orElse("false"))`. This is verbose and error-prone. He wants all four typed accessors (`getBoolean`, `getInt`, `getLong`, `getDouble`) available as a clean conditional API for non-polymorphic use.

### Who

- Java developer | Uses FlagProvider for simple conditional flag checks | Wants typed accessor methods without manual parsing

### Solution

Document and test `getBoolean()`, `getInt()`, `getLong()`, and `getDouble()` as the public conditional API on FlagProvider. These are the same default methods introduced by US-M2-05 and US-M2-07 for proxy use, but this story validates and documents their use as a standalone non-polymorphic API. All have context-aware overloads.

### Domain Examples

#### 1: Boolean conditional check

Carlos writes `if (flags.getBoolean("maintenance-mode").orElse(false)) { showMaintenancePage(); }`. Clean, no manual parsing.

#### 2: Integer conditional check

Carlos writes `int maxItems = flags.getInt("max-items").orElse(100);`. Provider returns "200" as string, getInt parses it to 200.

#### 3: Long conditional check

Carlos writes `long rateLimit = flags.getLong("rate-limit").orElse(1000L);`. Provider returns "5000000000", getLong parses to 5000000000L.

#### 4: Double conditional check

Mei Chen writes `double ratio = flags.getDouble("sampling-ratio").orElse(0.1);`. Provider returns "0.75", getDouble parses to 0.75.

### UAT Scenarios (BDD)

#### Scenario: getBoolean for conditional check

Given the FlagProvider has flag "maintenance-mode" with string value "true"
When Carlos calls `flags.getBoolean("maintenance-mode")`
Then `Optional.of(true)` is returned

#### Scenario: getInt for conditional check

Given the FlagProvider has flag "max-items" with string value "200"
When Carlos calls `flags.getInt("max-items")`
Then `OptionalInt.of(200)` is returned

#### Scenario: getLong for conditional check

Given the FlagProvider has flag "rate-limit" with string value "5000000000"
When Carlos calls `flags.getLong("rate-limit")`
Then `OptionalLong.of(5000000000L)` is returned

#### Scenario: getDouble for conditional check

Given the FlagProvider has flag "sampling-ratio" with string value "0.75"
When Mei Chen calls `flags.getDouble("sampling-ratio")`
Then `OptionalDouble.of(0.75)` is returned

#### Scenario: Absent flag returns empty for all types

Given the FlagProvider has no flag "unknown"
When Carlos calls `flags.getBoolean("unknown")`, `flags.getInt("unknown")`, `flags.getLong("unknown")`, `flags.getDouble("unknown")`
Then all return empty optionals

### Acceptance Criteria

- [ ] `getBoolean()`, `getInt()`, `getLong()`, `getDouble()` documented as public conditional API
- [ ] All four methods have context-aware overloads accepting `EvaluationContext`
- [ ] Parse failures return empty optionals, not exceptions
- [ ] Absent flags result in empty optionals for all typed methods
- [ ] These are the same methods used by generated typed proxies (no duplication)
- [ ] Return types: `Optional<Boolean>`, `OptionalInt`, `OptionalLong`, `OptionalDouble`

### Outcome KPIs

- **Who**: Java developers using FlagProvider for conditional checks
- **Does what**: Access typed flag values without manual parsing boilerplate
- **By how much**: Zero manual parsing calls for boolean/int/long/double flags
- **Measured by**: All four typed accessors available and return correct values
- **Baseline**: Manual getString + parse for every typed flag check

### Technical Notes

- Methods already exist from US-M2-05 and US-M2-07. This story is primarily about testing the conditional API use case, documenting the public API surface, and ensuring Javadoc coverage.
- `FlagProvider.getBoolean()` returns `Optional<Boolean>` (no `OptionalBoolean` in the JDK)
- `FlagProvider.getInt()` returns `OptionalInt`
- `FlagProvider.getLong()` returns `OptionalLong`
- `FlagProvider.getDouble()` returns `OptionalDouble`
