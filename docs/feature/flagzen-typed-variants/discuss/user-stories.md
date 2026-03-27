<!-- markdownlint-disable MD024 -->

## US-M2-01: FeatureType Enum and @Feature Type Attribute

### Problem

Kenji Tanaka is a Java backend developer who uses FlagZen for polymorphic dispatch in a payments service. He currently encodes integer flag values as strings (e.g., `@Variant("3")` for a retry count) because @Feature only supports string-typed dispatch. He finds it error-prone to map numeric flag values to string variants and wants the annotation model to express the flag's actual type.

### Who

- Java developer | Backend microservice with integer-valued flags | Wants type-safe dispatch without string encoding

### Solution

Add a `FeatureType` enum (STRING, INT, BOOLEAN) and a `type` attribute to the `@Feature` annotation, defaulting to STRING for backward compatibility.

### Domain Examples

#### 1: Retry strategy with integer flag -- Kenji annotates his RetryStrategy interface with `@Feature(value = "max-retries", type = FeatureType.INT)`. The annotation compiles. Existing features without `type` continue to default to `FeatureType.STRING`.

#### 2: Dark mode with boolean flag -- Mei Chen annotates her DarkMode interface with `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)`. The annotation compiles and signals boolean dispatch.

#### 3: Backward compatibility -- Kenji's existing `@Feature("checkout-flow")` interface (no type specified) continues to work identically. The processor treats it as `FeatureType.STRING`.

### UAT Scenarios (BDD)

#### Scenario: Feature with explicit INT type compiles

Given Kenji annotates RetryStrategy with `@Feature(value = "max-retries", type = FeatureType.INT)`
When the project compiles
Then the `@Feature` annotation is processed without errors
And the FeatureModel records featureType as INT

#### Scenario: Feature with explicit BOOLEAN type compiles

Given Mei Chen annotates DarkMode with `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)`
When the project compiles
Then the `@Feature` annotation is processed without errors
And the FeatureModel records featureType as BOOLEAN

#### Scenario: Feature without type defaults to STRING

Given Kenji has an existing `@Feature("checkout-flow")` without type attribute
When the project compiles
Then the FeatureModel records featureType as STRING
And existing proxy generation behavior is unchanged

### Acceptance Criteria

- [ ] `FeatureType` enum exists with constants STRING, INT, BOOLEAN
- [ ] `@Feature` annotation has `type()` attribute defaulting to `FeatureType.STRING`
- [ ] Annotation processor reads and stores the type in FeatureModel
- [ ] Existing @Feature annotations without type attribute compile and behave identically to before

### Outcome KPIs

- **Who**: Java developers using FlagZen
- **Does what**: Declare feature type at annotation level instead of encoding in string values
- **By how much**: 100% of typed features use explicit FeatureType (no more string-encoded integers)
- **Measured by**: Annotation processor correctly identifies feature type for all processed @Feature annotations
- **Baseline**: All features are implicitly STRING

### Technical Notes

- FeatureType enum lives in `com.flagzen` package
- @Feature.type() has retention CLASS (same as existing attributes)
- FeatureModel gains a `featureType` field populated during processing
- No runtime dependency on FeatureType -- it drives compile-time code generation only

---

## US-M2-02: @Variant intValue and booleanValue Attributes

### Problem

Kenji Tanaka has a retry strategy that dispatches based on the integer value of a "max-retries" flag. Currently he must use `@Variant("3")` and rely on string matching, which is misleading -- the flag provider returns an integer, not the string "3". He wants `@Variant(intValue = 3)` to express the variant's match value in its actual type.

### Who

- Java developer | Has integer-typed or boolean-typed feature flags | Wants annotation attributes that match the flag's actual type

### Solution

Add `intValue` and `booleanValue` attributes to the `@Variant` annotation. Exactly one of `value`, `intValue`, or `booleanValue` is used per annotation instance (enforced by processor in US-M2-03).

### Domain Examples

#### 1: Integer variant -- Kenji annotates ConservativeRetry with `@Variant(intValue = 3)` and AggressiveRetry with `@Variant(intValue = 10)` for his max-retries feature.

#### 2: Boolean variant -- Mei Chen annotates DarkModeOn with `@Variant(booleanValue = true)` and DarkModeOff with `@Variant(booleanValue = false)` for her dark-mode feature. Alternatively, she uses the convenience annotations `@WhenTrue` and `@WhenFalse`.

#### 3: String variant unchanged -- Kenji's existing `@Variant("CLASSIC")` on ClassicCheckout continues to work for string-typed features.

### UAT Scenarios (BDD)

#### Scenario: Variant with intValue compiles

Given Kenji annotates ConservativeRetry with `@Variant(intValue = 3)`
When the project compiles
Then the annotation is processed without errors
And VariantModel records the variant value as integer 3

#### Scenario: Variant with booleanValue compiles

Given Mei Chen annotates DarkModeOn with `@Variant(booleanValue = true)`
When the project compiles
Then the annotation is processed without errors
And VariantModel records the variant value as boolean true

#### Scenario: Existing string variant unchanged

Given Kenji has `@Variant("CLASSIC")` on ClassicCheckout
When the project compiles
Then the annotation is processed with value "CLASSIC" as before

#### Scenario: Default values for unused attributes

Given Kenji annotates with `@Variant(intValue = 3)` without setting value or booleanValue
When the processor reads the annotation
Then value has its default sentinel, booleanValue has its default sentinel
And only intValue is treated as the active attribute

### Acceptance Criteria

- [ ] @Variant annotation has `intValue()` attribute with appropriate default sentinel
- [ ] @Variant annotation has `booleanValue()` attribute with appropriate default sentinel
- [ ] `@WhenTrue` convenience annotation (retention CLASS, target TYPE) — equivalent to `@Variant(booleanValue = true)`
- [ ] `@WhenFalse` convenience annotation (retention CLASS, target TYPE) — equivalent to `@Variant(booleanValue = false)`
- [ ] `@WhenTrue(of = ...)` and `@WhenFalse(of = ...)` for multi-feature classes
- [ ] VariantModel can store typed values (integer, boolean) alongside string value
- [ ] Existing @Variant(value = "...") annotations compile and work as before
- [ ] Annotation attribute defaults allow the processor to detect which attribute was explicitly set

### Outcome KPIs

- **Who**: Java developers with typed feature flags
- **Does what**: Express variant match values in their actual type at annotation level
- **By how much**: Eliminates string encoding for all int/boolean variant values
- **Measured by**: VariantModel correctly stores typed values for all processed @Variant annotations
- **Baseline**: All variant values are strings

### Technical Notes

- Java annotations cannot have null defaults. Use sentinels: `intValue` default `Integer.MIN_VALUE`, `booleanValue` would need a tri-state approach. Alternative: the processor detects which attribute was set by comparing against sentinels. The crafter decides the sentinel strategy.
- @Variant remains @Repeatable with @Variants container
- Retention stays CLASS
- The `of` attribute is unrelated and unchanged

---

## US-M2-03: Compile-Time Type Consistency Validation

### Problem

Kenji Tanaka accidentally annotates a variant with `@Variant(value = "3")` on an `@Feature(type = FeatureType.INT)` interface. Without compile-time validation, this mismatch would cause a silent dispatch failure at runtime -- the proxy would look up integer 3 but the variant is registered under string "3". He needs the processor to catch this at compile time with a clear error message.

### Who

- Java developer | Annotating typed features | Needs compile-time safety against type mismatches

### Solution

The annotation processor validates that all @Variant annotations for a feature use the attribute matching the @Feature's declared type. Mismatches produce compilation errors with actionable fix suggestions.

### Domain Examples

#### 1: String variant on INT feature -- Kenji has `@Feature(type = FeatureType.INT)` on RetryStrategy and `@Variant(value = "3")` on ConservativeRetry. Processor emits: "Feature 'max-retries' declares type INT but variant ConservativeRetry uses string value. Use @Variant(intValue = 3) instead."

#### 2: intValue on BOOLEAN feature -- Mei Chen has `@Feature(type = FeatureType.BOOLEAN)` on DarkMode and `@Variant(intValue = 1)` on DarkModeOn. Processor emits: "Feature 'dark-mode' declares type BOOLEAN but variant DarkModeOn uses intValue. Use @Variant(booleanValue = true) instead."

#### 3: Mixed types across variants -- Kenji has two variants for an INT feature: one uses `intValue = 3`, the other uses `value = "fast"`. Processor emits errors on the mismatched variant identifying it by class name.

#### 4: Boolean REQUIRED incomplete -- Kenji has `@Feature(type = BOOLEAN, fallback = REQUIRED)` with only a `true` variant. Processor emits: "Boolean feature 'dark-mode' with REQUIRED fallback must have variants for both true and false."

#### 5: Duplicate typed values -- Two variants both declare `intValue = 3`. Processor rejects with the existing duplicate variant value error, adapted for integer values.

### UAT Scenarios (BDD)

#### Scenario: Type mismatch -- string variant on INT feature

Given Kenji has `@Feature(value = "max-retries", type = FeatureType.INT)`
And ConservativeRetry is annotated with `@Variant(value = "3")`
When the annotation processor runs
Then compilation fails with an error on ConservativeRetry
And the error message includes "type INT" and "string value" and "Use @Variant(intValue = 3)"

#### Scenario: Type mismatch -- intValue on BOOLEAN feature

Given Mei Chen has `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)`
And DarkModeOn is annotated with `@Variant(intValue = 1)`
When the annotation processor runs
Then compilation fails with an error on DarkModeOn
And the error message includes "type BOOLEAN" and "intValue" and "Use @Variant(booleanValue = true)"

#### Scenario: Duplicate intValue rejected

Given Kenji has `@Feature(value = "max-retries", type = FeatureType.INT)`
And ConservativeRetry with `@Variant(intValue = 3)` and CautiousRetry with `@Variant(intValue = 3)`
When the annotation processor runs
Then compilation fails with duplicate variant value error for value 3

#### Scenario: Boolean REQUIRED incomplete

Given Kenji has `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)`
And only DarkModeOn with `@Variant(booleanValue = true)`
And no variant for false and no @DefaultVariant
When the annotation processor runs
Then compilation fails requiring both true and false variants

#### Scenario: Boolean REQUIRED satisfied by DefaultVariant

Given Kenji has `@Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)`
And DarkModeOn with `@Variant(booleanValue = true)`
And FallbackDarkMode with `@DefaultVariant`
When the annotation processor runs
Then compilation succeeds

### Acceptance Criteria

- [ ] Processor rejects @Variant with wrong attribute for the @Feature type, with actionable error message
- [ ] Processor rejects mixed variant attributes within same feature
- [ ] Processor rejects duplicate typed values (intValue or booleanValue)
- [ ] Processor enforces boolean REQUIRED completeness (both true + false, or DefaultVariant covers gap)
- [ ] Error messages include feature name, variant class name, and suggested fix
- [ ] All existing validation rules continue to work for STRING features

### Outcome KPIs

- **Who**: Java developers annotating typed features
- **Does what**: Receive compile-time errors for type mismatches instead of runtime dispatch failures
- **By how much**: 100% of type mismatches caught at compile time (zero runtime surprises)
- **Measured by**: Processor emits diagnostic for every type-inconsistent @Variant annotation
- **Baseline**: No type validation exists (string-only)

### Technical Notes

- Validation runs in the same processing round as existing validations
- Error messages use `Messager.printMessage(Kind.ERROR, ...)` on the offending element
- Boolean REQUIRED check extends existing REQUIRED/enum completeness logic
- Sentinel detection logic must be robust -- see US-M2-02 technical notes

---

## US-M2-04: Integer Proxy Dispatch and FlagProvider.getInt

### Problem

Kenji Tanaka's RetryStrategy feature dispatches on an integer "max-retries" flag. Currently, the generated proxy calls `getString()` and matches against string variant values, requiring his flag provider to return "3" instead of the integer 3. He wants the proxy to call `getInt()` and match against integer variant values directly.

### Who

- Java developer | Has INT-typed feature compiling successfully | Wants runtime dispatch on integer flag values

### Solution

The proxy generator produces dispatch logic that calls `FlagProvider.getInt(key)` for INT features and uses `Map<Integer, Supplier<T>>` for variant lookup. FlagProvider gains a `getInt()` default method that parses from `getString()`.

### Domain Examples

#### 1: Integer dispatch works -- Kenji's flag provider returns `Optional.of(3)` for `getInt("max-retries")`. The proxy selects ConservativeRetry.

#### 2: Flag value changes at runtime -- Flag provider starts returning 10 instead of 3. Next method call on the proxy dispatches to AggressiveRetry.

#### 3: Unmatched integer triggers fallback -- Flag provider returns 99, which matches no variant. EXCEPTION strategy throws `UnmatchedVariantException` listing known values [3, 10].

### UAT Scenarios (BDD)

#### Scenario: Proxy dispatches on integer value

Given Kenji has RetryStrategy with FeatureType.INT and variants for intValue 3 and 10
And the FlagProvider returns `Optional.of(3)` for `getInt("max-retries")`
When Kenji resolves RetryStrategy via FeatureDispatcher
Then ConservativeRetry handles the method call

#### Scenario: Proxy follows runtime flag changes

Given the FlagProvider initially returns 3 for "max-retries"
When the flag value changes to 10
And Kenji calls a method on the resolved RetryStrategy proxy
Then AggressiveRetry handles the call

#### Scenario: Unmatched integer with EXCEPTION fallback

Given RetryStrategy has fallback EXCEPTION
And the FlagProvider returns `Optional.of(99)` for `getInt("max-retries")`
When Kenji resolves RetryStrategy
Then `UnmatchedVariantException` is thrown
And the exception message includes "99" and known values [3, 10]

#### Scenario: FlagProvider.getInt parses from getString

Given a FlagProvider that implements only `getString()` returning "42"
When `getInt("some-key")` is called via the default method
Then `Optional.of(42)` is returned

#### Scenario: FlagProvider.getInt returns empty for non-integer

Given a FlagProvider that returns "abc" for `getString("some-key")`
When `getInt("some-key")` is called via the default method
Then `Optional.empty()` is returned

### Acceptance Criteria

- [ ] Generated proxy for INT features calls `FlagProvider.getInt()` instead of `getString()`
- [ ] Proxy variant map uses `Map<Integer, Supplier<T>>` keyed by intValue
- [ ] `FlagProvider.getInt(String key)` is a default method parsing from `getString()`
- [ ] `FlagProvider.getInt(String key, EvaluationContext ctx)` default method also exists (context-aware)
- [ ] Parse failure in default getInt returns `Optional.empty()`, not exception
- [ ] Unmatched integer values trigger fallback strategy identically to unmatched string values
- [ ] Dynamic dispatch works -- proxy re-evaluates on each method call
- [ ] `@PinFlag` supports pinning typed features (variant value specified as string, parsed by proxy via FlagProvider default methods)
- [ ] `TestFlagContext.pin()` works with typed features (string values parsed by proxy)
- [ ] `InMemoryFlagProvider` stores string values; typed proxies parse via default getInt/getBoolean methods

### Outcome KPIs

- **Who**: Java developers with integer-valued feature flags
- **Does what**: Dispatch on actual integer values without string encoding
- **By how much**: Zero string-to-int encoding workarounds needed for INT features
- **Measured by**: Generated proxy calls getInt() and correctly dispatches for all integer variant values
- **Baseline**: All dispatch goes through getString() with string matching

### Technical Notes

- ProxyGenerator needs a code path for INT features: call getInt, match against Integer keys
- FlagProvider.getInt is a default method -- no breaking change to existing implementations
- Context-aware overload follows same pattern as existing getString(key, context)
- InMemoryFlagProvider and TestFlagContext may need updates to support typed storage or rely on string parsing
- FlagProvider implementations with native typed support (e.g., LaunchDarkly SDK returns integers directly) can override getInt/getBoolean for efficiency, bypassing string parsing
- For @Repeatable @Variant on multi-feature classes, each @Variant instance's attribute (value/intValue/booleanValue) must match its target feature's type. Processor validates per-feature.

---

## US-M2-05: Boolean Dispatch with REQUIRED Completeness

### Problem

Mei Chen has a dark-mode feature with exactly two states: on and off. She currently uses `@Variant("true")` and `@Variant("false")` as strings, which feels wrong -- the flag provider returns a boolean, and the string "true"/"false" encoding is fragile. She wants `@Variant(booleanValue = true)` with the compile-time guarantee that both values are covered.

### Who

- Java developer | Has boolean-typed feature flags | Wants boolean dispatch with completeness checking

### Solution

The proxy generator produces dispatch logic that calls `FlagProvider.getBoolean(key)` for BOOLEAN features. The REQUIRED fallback strategy validates both true and false are covered (or a DefaultVariant fills the gap).

### Domain Examples

#### 1: Boolean dispatch -- Mei Chen's flag provider returns `true` for "dark-mode". Proxy selects DarkModeOn.

#### 2: Boolean with convenience annotations -- Mei Chen uses `@WhenTrue` on DarkModeOn and `@WhenFalse` on DarkModeOff instead of `@Variant(booleanValue = ...)`. Both are syntactic sugar processed identically.

#### 3: Boolean REQUIRED satisfied -- Both `@Variant(booleanValue = true)` and `@Variant(booleanValue = false)` (or `@WhenTrue`/`@WhenFalse`) exist. REQUIRED strategy is satisfied. Compiles.

#### 4: DefaultVariant covers boolean gap -- Only `@WhenTrue` exists, but `@DefaultVariant` covers the false case. REQUIRED is satisfied.

### UAT Scenarios (BDD)

#### Scenario: Boolean proxy dispatches correctly

Given Mei Chen has DarkMode with FeatureType.BOOLEAN
And DarkModeOn for booleanValue true, DarkModeOff for booleanValue false
And the FlagProvider returns `Optional.of(true)` for `getBoolean("dark-mode")`
When she resolves DarkMode via FeatureDispatcher
Then DarkModeOn handles the method call

#### Scenario: Boolean proxy dispatches on false

Given the FlagProvider returns `Optional.of(false)` for `getBoolean("dark-mode")`
When Mei Chen resolves DarkMode
Then DarkModeOff handles the method call

#### Scenario: Boolean with NOOP fallback handles absent flag

Given DarkMode has fallback NOOP
And the FlagProvider returns `Optional.empty()` for `getBoolean("dark-mode")`
When Mei Chen resolves DarkMode
Then the NOOP proxy returns safe defaults

#### Scenario: FlagProvider.getBoolean parses from getString

Given a FlagProvider returning "true" for `getString("flag")`
When `getBoolean("flag")` is called via default method
Then `Optional.of(true)` is returned

#### Scenario: FlagProvider.getBoolean returns empty for non-boolean string

Given a FlagProvider returning "maybe" for `getString("flag")`
When `getBoolean("flag")` is called via default method
Then `Optional.empty()` is returned

### Acceptance Criteria

- [ ] Generated proxy for BOOLEAN features calls `FlagProvider.getBoolean()` instead of `getString()`
- [ ] Proxy variant map uses `Map<Boolean, Supplier<T>>` keyed by booleanValue
- [ ] `FlagProvider.getBoolean(String key)` is a default method parsing from `getString()`
- [ ] `FlagProvider.getBoolean(String key, EvaluationContext ctx)` default method also exists
- [ ] getBoolean parses only exact "true"/"false" (case-insensitive) as valid; other strings return empty
- [ ] Dynamic dispatch works for boolean features

### Outcome KPIs

- **Who**: Java developers with boolean feature flags
- **Does what**: Dispatch on boolean values with compile-time completeness guarantee
- **By how much**: Boolean features have provably complete variant coverage at compile time
- **Measured by**: Processor validates boolean REQUIRED completeness; proxy dispatches on getBoolean()
- **Baseline**: Boolean dispatch encoded as string "true"/"false"

### Technical Notes

- Boolean getBoolean parsing: only "true" (case-insensitive) maps to true, only "false" maps to false, anything else returns empty. This is stricter than Boolean.parseBoolean which treats everything non-"true" as false.
- REQUIRED completeness for boolean is validated in US-M2-03 (compile-time validation). This story focuses on the runtime dispatch side.
- ProxyGenerator needs a BOOLEAN code path parallel to the INT code path

---

## US-M2-06: Conditional API -- getBoolean and getInt on FlagProvider

### Problem

Kenji Tanaka sometimes needs a simple boolean flag check without polymorphic dispatch -- just `if (flags.getBoolean("feature-x"))`. Currently FlagProvider only exposes `getString()`, forcing him to parse manually every time: `Boolean.parseBoolean(flags.getString("feature-x").orElse("false"))`. This is verbose and error-prone.

### Who

- Java developer | Uses FlagProvider for simple conditional checks | Wants typed accessor methods without manual parsing

### Solution

Add `getBoolean(String key)` and `getInt(String key)` as default methods on `FlagProvider`, delegating to `getString()` with parsing. These are the same methods used by typed proxies but also available for direct non-polymorphic use. Context-aware overloads included.

### Domain Examples

#### 1: Boolean conditional check -- Kenji writes `if (flags.getBoolean("feature-x").orElse(false)) { enableFeatureX(); }`. Clean, no manual parsing.

#### 2: Integer conditional check -- Kenji writes `int maxItems = flags.getInt("max-items").orElse(100);`. Provider returns "200" as string, getInt parses it to 200.

#### 3: Unparseable value is absent -- Provider returns "abc" for "max-items". `flags.getInt("max-items")` returns `Optional.empty()`. Kenji's `.orElse(100)` kicks in.

### UAT Scenarios (BDD)

#### Scenario: getBoolean returns true for "true" string

Given the FlagProvider has flag "feature-x" with string value "true"
When Kenji calls `flags.getBoolean("feature-x")`
Then `Optional.of(true)` is returned

#### Scenario: getBoolean returns false for "false" string

Given the FlagProvider has flag "feature-x" with string value "false"
When Kenji calls `flags.getBoolean("feature-x")`
Then `Optional.of(false)` is returned

#### Scenario: getBoolean returns empty for absent flag

Given the FlagProvider has no flag "feature-x"
When Kenji calls `flags.getBoolean("feature-x")`
Then `Optional.empty()` is returned

#### Scenario: getInt returns parsed integer

Given the FlagProvider has flag "max-items" with string value "200"
When Kenji calls `flags.getInt("max-items")`
Then `Optional.of(200)` is returned

#### Scenario: getInt returns empty for non-integer string

Given the FlagProvider has flag "max-items" with string value "many"
When Kenji calls `flags.getInt("max-items")`
Then `Optional.empty()` is returned

### Acceptance Criteria

- [ ] `FlagProvider.getBoolean(String key)` default method parses from getString
- [ ] `FlagProvider.getInt(String key)` default method parses from getString
- [ ] Both methods have context-aware overloads: `getBoolean(String key, EvaluationContext ctx)`, `getInt(String key, EvaluationContext ctx)`
- [ ] Parse failures return Optional.empty(), not exceptions
- [ ] Absent flags (getString returns empty) result in Optional.empty() for typed methods
- [ ] These are the same methods used by generated typed proxies (no duplication)

### Outcome KPIs

- **Who**: Java developers using FlagProvider for conditional checks
- **Does what**: Access typed flag values without manual parsing boilerplate
- **By how much**: Zero manual parsing calls for boolean/int flags
- **Measured by**: getBoolean/getInt available as default methods and return correct values for all input types
- **Baseline**: Manual getString + parse for every typed flag check

### Technical Notes

- getBoolean and getInt are already introduced by US-M2-04/US-M2-05 for proxy use. This story ensures they are documented and tested as a public conditional API, not just internal proxy infrastructure.
- If US-M2-04/US-M2-05 deliver these methods, this story is primarily about testing the conditional API use case and adding context-aware overloads if missing.

---

## US-M2-07: Conditional API -- getLong and getDouble on FlagProvider

### Problem

Kenji Tanaka has a rate-limiting service with a "rate-limit" flag that holds a long value (1000000000) and a "sampling-ratio" flag with a double value (0.75). He currently parses these manually from `getString()`. He wants `getLong()` and `getDouble()` for consistency with the existing `getBoolean()` and `getInt()` methods.

### Who

- Java developer | Uses FlagProvider with long/double flag values | Wants complete typed accessor set

### Solution

Add `getLong(String key)` and `getDouble(String key)` as default methods on `FlagProvider`, following the same pattern as `getBoolean()` and `getInt()`.

### Domain Examples

#### 1: Long value -- Kenji's rate-limit flag has value "1000000000". `flags.getLong("rate-limit")` returns `Optional.of(1000000000L)`.

#### 2: Double value -- Kenji's sampling-ratio flag has value "0.75". `flags.getDouble("sampling-ratio")` returns `Optional.of(0.75)`.

#### 3: Overflow handling -- Flag "big-number" has value "999999999999999999999". `flags.getLong("big-number")` returns `Optional.empty()` (exceeds Long.MAX_VALUE).

### UAT Scenarios (BDD)

#### Scenario: getLong parses valid long

Given the FlagProvider has flag "rate-limit" with string value "1000000000"
When Kenji calls `flags.getLong("rate-limit")`
Then `Optional.of(1000000000L)` is returned

#### Scenario: getLong returns empty for non-long string

Given the FlagProvider has flag "rate-limit" with string value "fast"
When Kenji calls `flags.getLong("rate-limit")`
Then `Optional.empty()` is returned

#### Scenario: getDouble parses valid double

Given the FlagProvider has flag "ratio" with string value "0.75"
When Kenji calls `flags.getDouble("ratio")`
Then `Optional.of(0.75)` is returned

#### Scenario: getDouble returns empty for non-numeric string

Given the FlagProvider has flag "ratio" with string value "high"
When Kenji calls `flags.getDouble("ratio")`
Then `Optional.empty()` is returned

#### Scenario: Context-aware getLong

Given the FlagProvider has context-dependent flag "rate-limit"
And for targeting key "premium-tier" the value is "5000000000"
When Kenji calls `flags.getLong("rate-limit", context)` with targeting key "premium-tier"
Then `Optional.of(5000000000L)` is returned

### Acceptance Criteria

- [ ] `FlagProvider.getLong(String key)` default method parses from getString
- [ ] `FlagProvider.getDouble(String key)` default method parses from getString
- [ ] Both methods have context-aware overloads
- [ ] Parse failures and absent flags return Optional.empty()
- [ ] Numeric overflow returns Optional.empty(), not exception

### Outcome KPIs

- **Who**: Java developers using FlagProvider with long/double flags
- **Does what**: Access long and double flag values without manual parsing
- **By how much**: Complete typed accessor set (boolean, int, long, double) available on FlagProvider
- **Measured by**: getLong/getDouble return correct values for valid input, empty for invalid
- **Baseline**: Manual getString + Long.parseLong / Double.parseDouble

### Technical Notes

- These are purely additive default methods on FlagProvider -- no impact on existing implementations
- Not used by polymorphic dispatch (no LONG or DOUBLE FeatureType) -- conditional API only
- Pattern is identical to getInt/getBoolean: parse from getString, catch NumberFormatException, return empty
