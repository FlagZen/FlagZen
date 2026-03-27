Feature: FeatureType Enum and @Feature Type Attribute
  As a Java developer using FlagZen
  I want to declare the expected flag type on @Feature
  So that the annotation model expresses the actual data type for dispatch

  Scenario: Feature with explicit INT type
    Given Carlos Mendes defines RetryStrategy annotated with @Feature(value = "max-retries", type = FeatureType.INT)
    When the annotation processor runs during compilation
    Then the FeatureModel records featureType as INT
    And no compilation errors are reported

  Scenario: Feature with explicit BOOLEAN type
    Given Mei Chen defines DarkMode annotated with @Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
    When the annotation processor runs
    Then the FeatureModel records featureType as BOOLEAN

  Scenario: Feature with explicit LONG type
    Given Carlos defines RateLimiter annotated with @Feature(value = "rate-limit", type = FeatureType.LONG)
    When the annotation processor runs
    Then the FeatureModel records featureType as LONG

  Scenario: Feature with explicit DOUBLE type
    Given Mei Chen defines SamplingStrategy annotated with @Feature(value = "sampling-ratio", type = FeatureType.DOUBLE)
    When the annotation processor runs
    Then the FeatureModel records featureType as DOUBLE

  Scenario: Feature without type defaults to STRING
    Given Carlos has an existing @Feature("checkout-flow") without type attribute
    When the annotation processor runs
    Then the FeatureModel records featureType as STRING
    And existing proxy generation behavior is unchanged


Feature: Typed @Variant Attributes
  As a Java developer using FlagZen
  I want to annotate variants with typed attributes matching the feature type
  So that variant match values are expressed in their actual type

  Scenario: @Variant with intValue for INT feature
    Given Carlos annotates ConservativeRetry with @Variant(intValue = 3)
    And annotates AggressiveRetry with @Variant(intValue = 10)
    When the annotation processor runs
    Then VariantModel records integer 3 for ConservativeRetry
    And VariantModel records integer 10 for AggressiveRetry

  Scenario: @Variant with booleanValue for BOOLEAN feature
    Given Mei Chen annotates DarkModeOn with @Variant(booleanValue = true)
    And annotates DarkModeOff with @Variant(booleanValue = false)
    When the annotation processor runs
    Then VariantModel records boolean true for DarkModeOn
    And VariantModel records boolean false for DarkModeOff

  Scenario: @Variant with longValue for LONG feature
    Given Carlos annotates StandardLimit with @Variant(longValue = 1000)
    And annotates HighVolumeLimit with @Variant(longValue = 50000)
    When the annotation processor runs
    Then VariantModel records long 1000 for StandardLimit
    And VariantModel records long 50000 for HighVolumeLimit

  Scenario: @Variant with doubleValue and @CloseTo default delta
    Given Mei Chen annotates LowSampling with @Variant(doubleValue = @CloseTo(value = 0.1))
    When the annotation processor runs
    Then VariantModel records double 0.1 with delta 1e-10

  Scenario: @Variant with doubleValue and @CloseTo explicit delta
    Given Mei Chen annotates MediumSampling with @Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01))
    When the annotation processor runs
    Then VariantModel records double 0.5 with delta 0.01

  Scenario: Existing string @Variant unchanged
    Given Carlos has @Variant("CLASSIC") on ClassicCheckout for a STRING feature
    When the annotation processor runs
    Then the annotation is processed with value "CLASSIC" as before


Feature: @WhenTrue and @WhenFalse Convenience Annotations
  As a Java developer using FlagZen with boolean features
  I want convenience annotations for true/false variants
  So that boolean dispatch code is more readable

  Scenario: @WhenTrue equivalent to @Variant(booleanValue = true)
    Given Mei Chen annotates DarkModeOn with @WhenTrue
    When the annotation processor runs
    Then the processor treats it identically to @Variant(booleanValue = true)

  Scenario: @WhenFalse equivalent to @Variant(booleanValue = false)
    Given Mei Chen annotates DarkModeOff with @WhenFalse
    When the annotation processor runs
    Then the processor treats it identically to @Variant(booleanValue = false)

  Scenario: @WhenTrue with of= for multi-feature class
    Given Carlos defines DarkOnMaintenanceOff implementing both DarkMode and MaintenanceMode
    And annotates it with @WhenTrue(of = DarkMode.class)
    And annotates it with @WhenFalse(of = MaintenanceMode.class)
    When the annotation processor runs
    Then the processor registers booleanValue true for DarkMode on DarkOnMaintenanceOff
    And registers booleanValue false for MaintenanceMode on DarkOnMaintenanceOff

  Scenario: @WhenTrue without of= on single-feature class
    Given Mei Chen annotates DarkModeOn implementing only DarkMode with @WhenTrue
    When the annotation processor runs
    Then the processor infers the target feature as DarkMode

  Scenario: @WhenTrue mixed with @Variant(booleanValue = false) on same feature
    Given DarkModeOn annotated with @WhenTrue
    And DarkModeOff annotated with @Variant(booleanValue = false)
    And both implement DarkMode with FeatureType.BOOLEAN
    When the annotation processor runs
    Then compilation succeeds with both boolean variants registered


Feature: Compile-Time Type Validation
  As a Java developer using FlagZen
  I want the annotation processor to reject type mismatches at compile time
  So that I catch errors before runtime

  # --- Attribute mismatch ---

  Scenario: String variant on INT feature rejected
    Given Carlos has @Feature(value = "max-retries", type = FeatureType.INT)
    And ConservativeRetry annotated with @Variant(value = "3")
    When the annotation processor runs
    Then compilation fails with error on ConservativeRetry
    And the error message includes "type INT" and "string value" and suggests @Variant(intValue = 3)

  Scenario: intValue on BOOLEAN feature rejected
    Given Mei Chen has @Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
    And DarkModeOn annotated with @Variant(intValue = 1)
    When the annotation processor runs
    Then compilation fails with error on DarkModeOn
    And the error suggests using @Variant(booleanValue = true)

  Scenario: booleanValue on LONG feature rejected
    Given Carlos has @Feature(value = "rate-limit", type = FeatureType.LONG)
    And a variant annotated with @Variant(booleanValue = true)
    When the annotation processor runs
    Then compilation fails suggesting @Variant(longValue = ...)

  Scenario: intValue on DOUBLE feature rejected
    Given Mei Chen has @Feature(value = "sampling-ratio", type = FeatureType.DOUBLE)
    And a variant annotated with @Variant(intValue = 1)
    When the annotation processor runs
    Then compilation fails suggesting @Variant(doubleValue = @CloseTo(...))

  # --- Mixed types within feature ---

  Scenario: Mixed variant attributes within same feature rejected
    Given Carlos has @Feature(value = "max-retries", type = FeatureType.INT)
    And ConservativeRetry with @Variant(intValue = 3)
    And AggressiveRetry with @Variant(value = "fast")
    When the annotation processor runs
    Then compilation fails identifying AggressiveRetry as using the wrong attribute

  # --- Duplicate typed values ---

  Scenario: Duplicate intValue rejected
    Given Carlos has @Feature(value = "max-retries", type = FeatureType.INT)
    And ConservativeRetry with @Variant(intValue = 3)
    And CautiousRetry with @Variant(intValue = 3)
    When the annotation processor runs
    Then compilation fails with duplicate variant value error for intValue 3

  Scenario: Duplicate longValue rejected
    Given Carlos has @Feature(value = "rate-limit", type = FeatureType.LONG)
    And StandardLimit with @Variant(longValue = 1000)
    And BasicLimit with @Variant(longValue = 1000)
    When the annotation processor runs
    Then compilation fails with duplicate variant value error for longValue 1000

  Scenario: Duplicate booleanValue true rejected
    Given Mei Chen has @Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
    And DarkModeOn with @WhenTrue
    And AlsoDarkModeOn with @Variant(booleanValue = true)
    When the annotation processor runs
    Then compilation fails with duplicate variant value error for booleanValue true

  # --- BOOLEAN REQUIRED completeness ---

  Scenario: Boolean REQUIRED missing false variant
    Given @Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)
    And only DarkModeOn annotated with @WhenTrue
    And no variant for false and no @DefaultVariant
    When the annotation processor runs
    Then compilation fails stating "REQUIRED BOOLEAN feature requires variants for both true and false"

  Scenario: Boolean REQUIRED both present
    Given @Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)
    And DarkModeOn with @WhenTrue
    And DarkModeOff with @WhenFalse
    When the annotation processor runs
    Then compilation succeeds

  Scenario: Boolean REQUIRED satisfied by DefaultVariant
    Given @Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)
    And DarkModeOn with @WhenTrue
    And FallbackDarkMode with @DefaultVariant
    When the annotation processor runs
    Then compilation succeeds


Feature: Typed Proxy Dispatch
  As a Java developer using FlagZen
  I want generated proxies to dispatch on typed flag values
  So that variant selection uses the actual flag type without string encoding

  # --- INT dispatch ---

  Scenario: INT proxy dispatches via map lookup
    Given Carlos has RetryStrategy with FeatureType.INT and variants intValue 3 and 10
    And the FlagProvider returns OptionalInt.of(3) for getInt("max-retries")
    When Carlos resolves RetryStrategy via FeatureDispatcher
    Then ConservativeRetry handles the method call

  Scenario: INT proxy follows runtime flag changes
    Given the FlagProvider initially returns 3 for getInt("max-retries")
    When the flag value changes to 10
    And Carlos calls a method on the resolved RetryStrategy proxy
    Then AggressiveRetry handles the call

  # --- LONG dispatch ---

  Scenario: LONG proxy dispatches via map lookup
    Given Carlos has RateLimiter with FeatureType.LONG and variants longValue 1000 and 50000
    And the FlagProvider returns OptionalLong.of(50000) for getLong("rate-limit")
    When Carlos resolves RateLimiter via FeatureDispatcher
    Then HighVolumeLimit handles the method call

  # --- BOOLEAN dispatch ---

  Scenario: BOOLEAN proxy dispatches true via map lookup
    Given Mei Chen has DarkMode with FeatureType.BOOLEAN
    And DarkModeOn for booleanValue true, DarkModeOff for booleanValue false
    And the FlagProvider returns Optional.of(true) for getBoolean("dark-mode")
    When Mei Chen resolves DarkMode
    Then DarkModeOn handles the method call

  Scenario: BOOLEAN proxy dispatches false
    Given the FlagProvider returns Optional.of(false) for getBoolean("dark-mode")
    When Mei Chen resolves DarkMode
    Then DarkModeOff handles the method call

  # --- DOUBLE dispatch ---

  Scenario: DOUBLE proxy dispatches via approximate matching with default delta
    Given Mei Chen has SamplingStrategy with FeatureType.DOUBLE
    And LowSampling for @CloseTo(value = 0.1) with default delta 1e-10
    And MediumSampling for @CloseTo(value = 0.5, delta = 0.01)
    And the FlagProvider returns OptionalDouble.of(0.0999999999)
    When Mei Chen resolves SamplingStrategy
    Then LowSampling is selected because Math.abs(0.0999999999 - 0.1) <= 1e-10

  Scenario: DOUBLE proxy matches variant with explicit delta
    Given the FlagProvider returns OptionalDouble.of(0.505) for getDouble("sampling-ratio")
    When Mei Chen resolves SamplingStrategy
    Then MediumSampling is selected because Math.abs(0.505 - 0.5) <= 0.01

  Scenario: DOUBLE proxy no match when outside all deltas
    Given the FlagProvider returns OptionalDouble.of(0.99) for getDouble("sampling-ratio")
    And no variant has value within delta of 0.99
    When Mei Chen resolves SamplingStrategy with fallback EXCEPTION
    Then UnmatchedVariantException is thrown

  # --- Fallback behavior ---

  Scenario: Unmatched INT value triggers fallback strategy
    Given RetryStrategy with fallback EXCEPTION
    And the FlagProvider returns OptionalInt.of(99) for getInt("max-retries")
    When Carlos resolves RetryStrategy
    Then UnmatchedVariantException is thrown with message including "99" and known values [3, 10]

  Scenario: Typed feature with DefaultVariant handles unmatched value
    Given RetryStrategy with FeatureType.INT and DefaultRetry as @DefaultVariant
    And the FlagProvider returns OptionalInt.of(99)
    When Carlos resolves RetryStrategy
    Then DefaultRetry is selected

  Scenario: Empty typed value triggers fallback
    Given RetryStrategy with fallback NOOP
    And the FlagProvider returns OptionalInt.empty() for getInt("max-retries")
    When Carlos resolves RetryStrategy
    Then the NOOP proxy is used returning safe defaults


Feature: Typed Dispatch with Evaluation Context
  As a Java developer using FlagZen with typed dispatch and evaluation context
  I want typed polymorphic dispatch to respect the M1 evaluation context chain
  So that the same typed feature resolves to different variants per user or tenant

  Scenario: INT dispatch with explicit EvaluationContext
    Given a context-aware FlagProvider for "max-retries" returning 3 for plan "free" and 10 for plan "enterprise"
    When Carlos resolves RetryStrategy with explicit context targeting plan "enterprise"
    Then AggressiveRetry is selected

  Scenario: BOOLEAN dispatch with explicit context
    Given a context-aware FlagProvider returning true for "dark-mode" when preference is "dark"
    When Mei Chen resolves DarkMode with context attribute preference "dark"
    Then DarkModeOn is selected

  Scenario: Typed dispatch with block-scoped context
    Given a context-aware FlagProvider for "max-retries" (3 for free, 10 for enterprise)
    When Carlos runs FlagContext.run with context targeting plan "enterprise"
    And resolves RetryStrategy inside the block
    Then AggressiveRetry is selected
    And after the block exits the scoped context is cleared

  Scenario: Explicit context overrides scoped context for typed features
    Given a scoped context block with plan "free"
    When Carlos resolves RetryStrategy with explicit context targeting plan "enterprise" inside the block
    Then the explicit context wins and AggressiveRetry is selected

  Scenario: Typed dispatch with context accessor
    Given a ContextAccessor providing context with plan "enterprise"
    And a context-aware FlagProvider for "max-retries" (3 for free, 10 for enterprise)
    When Carlos resolves RetryStrategy without explicit context
    Then the accessor-provided context is used and AggressiveRetry is selected

  Scenario: Typed dispatch with no context falls back to default resolution
    Given a FlagProvider returning 3 for getInt("max-retries") regardless of context
    When Carlos resolves RetryStrategy without any evaluation context
    Then ConservativeRetry is selected based on the contextless flag value


Feature: Typed FlagProvider Methods (Conditional API)
  As a Java developer using FlagZen
  I want typed accessor methods on FlagProvider
  So that I can read boolean, int, long, and double flag values without manual parsing

  # --- getBoolean ---

  Scenario: getBoolean parses "true"
    Given the FlagProvider has flag "feature-x" with string value "true"
    When Carlos calls flags.getBoolean("feature-x")
    Then Optional.of(true) is returned

  Scenario: getBoolean parses "false"
    Given the FlagProvider has flag "feature-x" with string value "false"
    When Carlos calls flags.getBoolean("feature-x")
    Then Optional.of(false) is returned

  Scenario: getBoolean returns empty for absent flag
    Given the FlagProvider has no flag "feature-x"
    When Carlos calls flags.getBoolean("feature-x")
    Then Optional.empty() is returned

  Scenario: getBoolean returns empty for non-boolean string
    Given the FlagProvider has flag "feature-x" with string value "maybe"
    When Carlos calls flags.getBoolean("feature-x")
    Then Optional.empty() is returned

  # --- getInt ---

  Scenario: getInt parses valid integer
    Given the FlagProvider has flag "max-items" with string value "200"
    When Carlos calls flags.getInt("max-items")
    Then OptionalInt.of(200) is returned

  Scenario: getInt returns empty for non-integer string
    Given the FlagProvider has flag "max-items" with string value "many"
    When Carlos calls flags.getInt("max-items")
    Then OptionalInt.empty() is returned

  Scenario: getInt returns empty for absent flag
    Given the FlagProvider has no flag "max-items"
    When Carlos calls flags.getInt("max-items")
    Then OptionalInt.empty() is returned

  # --- getLong ---

  Scenario: getLong parses valid long
    Given the FlagProvider has flag "rate-limit" with string value "5000000000"
    When Carlos calls flags.getLong("rate-limit")
    Then OptionalLong.of(5000000000L) is returned

  Scenario: getLong returns empty for non-long string
    Given the FlagProvider has flag "rate-limit" with string value "unlimited"
    When Carlos calls flags.getLong("rate-limit")
    Then OptionalLong.empty() is returned

  Scenario: getLong returns empty for overflow
    Given the FlagProvider has flag "rate-limit" with string value "999999999999999999999"
    When Carlos calls flags.getLong("rate-limit")
    Then OptionalLong.empty() is returned

  # --- getDouble ---

  Scenario: getDouble parses valid double
    Given the FlagProvider has flag "sampling-ratio" with string value "0.75"
    When Carlos calls flags.getDouble("sampling-ratio")
    Then OptionalDouble.of(0.75) is returned

  Scenario: getDouble returns empty for non-numeric string
    Given the FlagProvider has flag "sampling-ratio" with string value "high"
    When Carlos calls flags.getDouble("sampling-ratio")
    Then OptionalDouble.empty() is returned

  # --- Context-aware overloads ---

  Scenario: getInt with EvaluationContext
    Given the FlagProvider has context-dependent flag "max-items"
    And for targeting key "premium-user" the value is "500"
    When Carlos calls flags.getInt("max-items", context) with targeting key "premium-user"
    Then OptionalInt.of(500) is returned

  Scenario: getBoolean with EvaluationContext
    Given the FlagProvider has context-dependent flag "dark-mode"
    And for targeting key "user-42" the value is "true"
    When Mei Chen calls flags.getBoolean("dark-mode", context) with targeting key "user-42"
    Then Optional.of(true) is returned

  Scenario: getLong with EvaluationContext
    Given the FlagProvider has context-dependent flag "rate-limit"
    And for targeting key "enterprise-tenant" the value is "10000000"
    When Carlos calls flags.getLong("rate-limit", context) with targeting key "enterprise-tenant"
    Then OptionalLong.of(10000000L) is returned

  Scenario: getDouble with EvaluationContext
    Given the FlagProvider has context-dependent flag "sampling-ratio"
    And for targeting key "debug-user" the value is "1.0"
    When Carlos calls flags.getDouble("sampling-ratio", context) with targeting key "debug-user"
    Then OptionalDouble.of(1.0) is returned
