<!-- markdownlint-disable MD024 -->
# User Stories -- flagzen-openfeature

## US-OF-01: String Flag Resolution Through OpenFeature

### Problem

Ricardo Alves is a senior Java developer at a fintech company that uses OpenFeature with Flagd for feature flags. He wants to use FlagZen's polymorphic dispatch (`@Feature`/`@Variant`) but finds it impossible to connect FlagZen to his existing OpenFeature infrastructure because no adapter exists. He would have to maintain two flag systems or abandon one.

### Who

- Java developer | Using OpenFeature SDK with any compliant provider (Flagd, CloudBees, Split) | Wants FlagZen's compile-time type safety without replacing flag infrastructure

### Solution

A `flagzen-openfeature` Gradle module providing `OpenFeatureFlagProvider` that implements `FlagProvider` by delegating to OpenFeature's `Client` API. String resolution uses `client.getStringDetails()` to detect real values vs. defaults/errors.

### Domain Examples

#### 1: Happy Path -- Ricardo resolves a checkout flow variant through Flagd

Ricardo's Flagd instance has flag `checkout-flow` set to `"EXPRESS"` for all users. He adds `flagzen-openfeature` to his build, and `FeatureDispatcher.feature(CheckoutFlow.class)` dispatches to `ExpressCheckout` because `OpenFeatureFlagProvider.getString("checkout-flow")` returns `Optional.of("EXPRESS")`.

#### 2: Edge Case -- Flag not configured in upstream provider

Ricardo asks for flag `new-dashboard` which does not exist in Flagd. `client.getStringDetails("new-dashboard", "")` returns a result with reason `DEFAULT`. The adapter returns `Optional.empty()`, and FlagZen's `@DefaultVariant` or fallback strategy activates.

#### 3: Error -- OpenFeature provider returns evaluation error

The Flagd server is temporarily unreachable. `client.getStringDetails("checkout-flow", "")` returns a result with `errorCode` set. The adapter returns `Optional.empty()`. FlagZen's fallback strategy handles it (EXCEPTION throws, NOOP returns defaults).

#### 4: ServiceLoader auto-discovery

Ricardo drops the JAR on the classpath without any programmatic configuration. `ServiceLoader<FlagProvider>` discovers `OpenFeatureFlagProvider` via `META-INF/services/com.flagzen.spi.FlagProvider`. The no-arg constructor uses `OpenFeatureAPI.getInstance().getClient()`.

### UAT Scenarios (BDD)

#### Scenario: String flag resolved through OpenFeature

```gherkin
Given Ricardo has registered a Flagd provider with OpenFeature
And the flag "checkout-flow" is set to "EXPRESS" in Flagd
When he creates an OpenFeatureFlagProvider and calls getString("checkout-flow")
Then the result is Optional.of("EXPRESS")
```

#### Scenario: Flag not found returns empty

```gherkin
Given Ricardo has registered a Flagd provider with OpenFeature
And the flag "new-dashboard" does not exist in Flagd
When he calls getString("new-dashboard") on the adapter
Then the result is Optional.empty()
```

#### Scenario: Evaluation error returns empty

```gherkin
Given Ricardo has registered a Flagd provider with OpenFeature
And the Flagd server is unreachable
When he calls getString("checkout-flow") on the adapter
Then the result is Optional.empty()
```

#### Scenario: ServiceLoader discovers the adapter

```gherkin
Given the flagzen-openfeature JAR is on the classpath
And a global OpenFeature provider is registered
When ServiceLoader loads FlagProvider implementations
Then OpenFeatureFlagProvider is among the discovered providers
And it resolves flags through the global OpenFeature client
```

#### Scenario: Explicit Client construction

```gherkin
Given Ricardo creates an OpenFeature client named "payments"
When he constructs OpenFeatureFlagProvider.create(client)
Then the adapter delegates to the "payments" client, not the global default
```

### Acceptance Criteria

- [ ] `OpenFeatureFlagProvider` implements `FlagProvider`
- [ ] `getString(key)` delegates to `client.getStringDetails(key, defaultValue)` and returns `Optional.of(value)` when reason indicates a real resolution
- [ ] `getString(key)` returns `Optional.empty()` when reason is DEFAULT (no value configured) or when errorCode is set
- [ ] No-arg constructor uses `OpenFeatureAPI.getInstance().getClient()`
- [ ] `create(Client)` factory method accepts a specific client instance
- [ ] `META-INF/services/com.flagzen.spi.FlagProvider` contains the FQCN

### Outcome KPIs

- **Who**: Java developers using OpenFeature
- **Does what**: Resolve string feature flags through FlagZen's polymorphic dispatch
- **By how much**: 100% of FlagProvider string methods delegating to OpenFeature
- **Measured by**: Unit test suite covering happy path, default, and error cases
- **Baseline**: No OpenFeature adapter exists

### Technical Notes

- Depends on `dev.openfeature:sdk` (version TBD -- use latest stable 1.x)
- Depends on `flagzen-core` (same version)
- No-arg constructor required for ServiceLoader
- Thread safety: `Client` from OpenFeature SDK is thread-safe per OpenFeature spec
- The `reason` field in `FlagEvaluationDetails` determines whether a real value was resolved

---

## US-OF-02: Typed Flag Resolution via Native OpenFeature Methods

### Problem

Ricardo Alves uses FlagZen with typed features (`@Feature(type = BOOLEAN)`, `@Feature(type = INT)`). The default `FlagProvider` typed methods parse from `getString`, but his OpenFeature provider stores native booleans and integers. Parsing from string loses type fidelity and is slower. He wants the adapter to use OpenFeature's native typed evaluation methods.

### Who

- Java developer | Using typed FlagZen features (boolean, int, long, double) | OpenFeature provider stores native typed values

### Solution

Override `getBoolean`, `getInt`, `getLong`, `getDouble` (and their context-aware overloads) in `OpenFeatureFlagProvider` to delegate to OpenFeature's `client.getBooleanDetails`, `client.getIntegerDetails`, `client.getDoubleDetails` respectively.

### Domain Examples

#### 1: Happy Path -- Boolean flag for dark mode

Ricardo's OpenFeature provider has `dark-mode` set to boolean `true`. `getBoolean("dark-mode")` calls `client.getBooleanDetails("dark-mode", false)`, gets `true` with a valid reason, and returns `Optional.of(true)`.

#### 2: Edge Case -- Integer flag with large value

Ricardo has `max-retries` set to `42` in his provider. `getInt("max-retries")` calls `client.getIntegerDetails("max-retries", 0)`, gets `42`, returns `OptionalInt.of(42)`. No string parsing involved.

#### 3: Error -- Typed evaluation error returns empty

The provider cannot evaluate `rollout-percentage` (a double flag) due to a type mismatch. `client.getDoubleDetails("rollout-percentage", 0.0)` returns an error. `getDouble("rollout-percentage")` returns `OptionalDouble.empty()`.

### UAT Scenarios (BDD)

#### Scenario: Boolean flag resolved natively

```gherkin
Given the OpenFeature provider stores "dark-mode" as boolean true
When Ricardo calls getBoolean("dark-mode") on the adapter
Then the result is Optional.of(true)
And the adapter called client.getBooleanDetails (not client.getStringDetails)
```

#### Scenario: Integer flag resolved natively

```gherkin
Given the OpenFeature provider stores "max-retries" as integer 42
When Ricardo calls getInt("max-retries") on the adapter
Then the result is OptionalInt.of(42)
```

#### Scenario: Double flag resolved natively

```gherkin
Given the OpenFeature provider stores "rollout-percentage" as double 0.75
When Ricardo calls getDouble("rollout-percentage") on the adapter
Then the result is OptionalDouble.of(0.75)
```

#### Scenario: Long flag resolved via integer widening

```gherkin
Given the OpenFeature provider stores "event-threshold" as integer 100000
When Ricardo calls getLong("event-threshold") on the adapter
Then the result is OptionalLong.of(100000)
```

#### Scenario: Typed evaluation error returns empty

```gherkin
Given the OpenFeature provider returns an error for boolean flag "dark-mode"
When Ricardo calls getBoolean("dark-mode") on the adapter
Then the result is Optional.empty()
```

### Acceptance Criteria

- [ ] `getBoolean(key)` delegates to `client.getBooleanDetails(key, false)` and wraps the result
- [ ] `getInt(key)` delegates to `client.getIntegerDetails(key, 0)` and wraps the result
- [ ] `getLong(key)` delegates to `client.getIntegerDetails(key, 0)` with widening to long
- [ ] `getDouble(key)` delegates to `client.getDoubleDetails(key, 0.0)` and wraps the result
- [ ] All typed methods return empty when OpenFeature reason is DEFAULT or errorCode is set
- [ ] All typed methods have context-aware overloads delegating with mapped context

### Outcome KPIs

- **Who**: Java developers using typed FlagZen features over OpenFeature
- **Does what**: Resolve typed flags through native OpenFeature typed methods
- **By how much**: Zero string-to-type parsing for boolean, int, long, double flags
- **Measured by**: Unit tests asserting native typed OpenFeature client methods are called
- **Baseline**: FlagProvider defaults parse all types from getString

### Technical Notes

- OpenFeature SDK has `getBooleanDetails`, `getIntegerDetails`, `getDoubleDetails` but no `getLongDetails`. For `getLong`, use `getIntegerDetails` and widen to long. Document this limitation.
- Default values passed to OpenFeature details methods (false, 0, 0.0) are sentinel-like but the `reason` field distinguishes real resolution from defaults.

---

## US-OF-03: EvaluationContext Mapping for Targeted Resolution

### Problem

Ricardo Alves needs per-user feature flag resolution. His OpenFeature provider (Flagd) supports targeting rules based on user attributes. FlagZen's `EvaluationContext` carries targeting key and attributes, but these are different classes from OpenFeature's `EvaluationContext`. Without a mapper, Ricardo cannot pass targeting context through the adapter.

### Who

- Java developer | Needs per-user/per-segment flag resolution | Uses targeting rules in OpenFeature provider

### Solution

A context mapper that converts `com.flagzen.EvaluationContext` to `dev.openfeature.sdk.EvaluationContext`. All context-aware `FlagProvider` overloads (`getString(key, ctx)`, `getBoolean(key, ctx)`, etc.) use this mapper before delegating to the OpenFeature client.

### Domain Examples

#### 1: Happy Path -- Enterprise user targeting

Ricardo builds `EvaluationContext.builder().targetingKey("user-7291").attribute("plan", "enterprise").build()`. The mapper produces an OpenFeature `EvaluationContext` with targeting key `"user-7291"` and attribute `plan = Value("enterprise")`. The flag resolves to the enterprise-specific variant.

#### 2: Edge Case -- No targeting key

Ricardo builds `EvaluationContext.builder().attribute("region", "EU").build()` without a targeting key. The mapper produces an OpenFeature context with no targeting key and attribute `region = Value("EU")`.

#### 3: Error -- Unsupported attribute type

Ricardo puts `attribute("timestamp", Instant.now())` in his context. `Instant` is not a type OpenFeature's `Value` supports. The mapper skips this attribute, logs a warning, and maps the remaining attributes.

#### 4: Complex attributes -- List and Map values

Ricardo puts `attribute("tags", List.of("beta", "early-access"))` and `attribute("limits", Map.of("maxUsers", 100))`. The mapper converts these to OpenFeature `Value` list and structure types respectively.

### UAT Scenarios (BDD)

#### Scenario: Full context mapping with targeting key and string attribute

```gherkin
Given Ricardo builds a FlagZen EvaluationContext with targetingKey "user-7291" and attribute "plan" = "enterprise"
When the adapter maps this to an OpenFeature EvaluationContext
Then the OpenFeature context has targetingKey "user-7291"
And the OpenFeature context has attribute "plan" with String value "enterprise"
```

#### Scenario: Context without targeting key

```gherkin
Given Ricardo builds a FlagZen EvaluationContext with no targeting key and attribute "region" = "EU"
When the adapter maps this to an OpenFeature EvaluationContext
Then the OpenFeature context has no targeting key set
And the OpenFeature context has attribute "region" with String value "EU"
```

#### Scenario: Numeric and boolean attributes mapped to Value types

```gherkin
Given Ricardo builds a FlagZen EvaluationContext with attributes "age" = 34 (Integer) and "premium" = true (Boolean)
When the adapter maps this to an OpenFeature EvaluationContext
Then the OpenFeature context has attribute "age" with Integer value 34
And the OpenFeature context has attribute "premium" with Boolean value true
```

#### Scenario: Unsupported attribute type skipped with warning

```gherkin
Given Ricardo builds a FlagZen EvaluationContext with attribute "timestamp" = an Instant value
When the adapter maps this to an OpenFeature EvaluationContext
Then the "timestamp" attribute is not present in the OpenFeature context
And a warning is logged: "Unsupported attribute type java.time.Instant for key 'timestamp'"
```

#### Scenario: Context-aware string resolution end-to-end

```gherkin
Given the OpenFeature provider returns "EXPRESS" for "checkout-flow" when targeting key is "user-7291"
And Ricardo builds a FlagZen EvaluationContext with targetingKey "user-7291"
When he calls getString("checkout-flow", context) on the adapter
Then the result is Optional.of("EXPRESS")
```

### Acceptance Criteria

- [ ] Mapper converts `com.flagzen.EvaluationContext` to `dev.openfeature.sdk.EvaluationContext`
- [ ] Targeting key maps 1:1 (including null targeting key)
- [ ] Supported attribute value types: String, Boolean, Integer, Long, Double, `List<?>`, `Map<String, ?>`
- [ ] Unsupported attribute types are skipped with a warning log (not an exception)
- [ ] All context-aware FlagProvider overloads (`getString(k,ctx)`, `getBoolean(k,ctx)`, `getInt(k,ctx)`, `getLong(k,ctx)`, `getDouble(k,ctx)`) use the mapper

### Outcome KPIs

- **Who**: Java developers needing per-user flag resolution through OpenFeature
- **Does what**: Pass FlagZen evaluation context through to OpenFeature providers
- **By how much**: 100% of FlagZen context fields (targeting key + all supported attribute types) mapped
- **Measured by**: Unit tests for each attribute type; integration test for end-to-end context-aware resolution
- **Baseline**: No context mapping exists

### Technical Notes

- OpenFeature `Value` supports: Boolean, String, Integer, Double, List, Structure (map), Instant, null. FlagZen attributes are `Map<String, Object>` -- need runtime type checking for conversion.
- OpenFeature also supports `Instant` in `Value` -- so actually `Instant` IS supported. Verify against the actual OpenFeature SDK API before implementation. If `Value.of(Instant)` exists, support it.
- Long values: OpenFeature `Value` may not distinguish Integer vs Long. Check SDK API. May need to convert Long to Integer (lossy for large values) or to Double.
