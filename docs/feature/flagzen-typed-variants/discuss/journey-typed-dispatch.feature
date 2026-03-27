Feature: Typed Polymorphic Dispatch
  As a Java developer using FlagZen
  I want to dispatch on integer and boolean flag values
  So that my variant selection reflects the actual flag type without string encoding hacks

  # --- Annotation and Type Declaration ---

  Scenario: Declare integer-typed feature
    Given Kenji Tanaka defines an interface RetryStrategy
    And annotates it with @Feature(value = "max-retries", type = FeatureType.INT)
    And creates ConservativeRetry annotated with @Variant(intValue = 3)
    And creates AggressiveRetry annotated with @Variant(intValue = 10)
    When the annotation processor runs during compilation
    Then the processor generates RetryStrategy_FlagZenProxy with integer dispatch logic
    And no compilation errors are reported

  Scenario: Declare boolean-typed feature
    Given Kenji defines an interface DarkMode
    And annotates it with @Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
    And creates DarkModeOn annotated with @Variant(booleanValue = true)
    And creates DarkModeOff annotated with @Variant(booleanValue = false)
    When the annotation processor runs during compilation
    Then the processor generates DarkMode_FlagZenProxy with boolean dispatch logic

  Scenario: Default type is STRING for backward compatibility
    Given Kenji defines an interface Theme
    And annotates it with @Feature("theme") without specifying type
    And creates variants with @Variant(value = "dark") and @Variant(value = "light")
    When the annotation processor runs during compilation
    Then the processor treats the feature as FeatureType.STRING
    And existing string-based dispatch behavior is unchanged

  # --- Compile-Time Validation ---

  Scenario: Type mismatch -- string variant on integer feature
    Given Kenji has @Feature(value = "max-retries", type = FeatureType.INT)
    And a variant ConservativeRetry annotated with @Variant(value = "3")
    When the annotation processor runs
    Then compilation fails with error on ConservativeRetry
    And the error message states "Feature 'max-retries' declares type INT but variant ConservativeRetry uses string value. Use @Variant(intValue = 3) instead."

  Scenario: Type mismatch -- int variant on boolean feature
    Given Kenji has @Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
    And a variant DarkModeOn annotated with @Variant(intValue = 1)
    When the annotation processor runs
    Then compilation fails with error on DarkModeOn
    And the error message states "Feature 'dark-mode' declares type BOOLEAN but variant DarkModeOn uses intValue. Use @Variant(booleanValue = true) instead."

  Scenario: Mixed variant types within same feature
    Given Kenji has @Feature(value = "max-retries", type = FeatureType.INT)
    And ConservativeRetry annotated with @Variant(intValue = 3)
    And AggressiveRetry annotated with @Variant(value = "fast")
    When the annotation processor runs
    Then compilation fails listing both variants
    And the error identifies AggressiveRetry as using the wrong variant attribute

  Scenario: Duplicate typed values rejected
    Given Kenji has @Feature(value = "max-retries", type = FeatureType.INT)
    And ConservativeRetry annotated with @Variant(intValue = 3)
    And CautiousRetry annotated with @Variant(intValue = 3)
    When the annotation processor runs
    Then compilation fails with duplicate variant value error for intValue 3

  Scenario: Boolean REQUIRED completeness -- missing false variant
    Given Kenji has @Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)
    And only DarkModeOn annotated with @Variant(booleanValue = true)
    When the annotation processor runs
    Then compilation fails with "Boolean feature 'dark-mode' with REQUIRED fallback must have variants for both true and false"

  Scenario: Boolean REQUIRED completeness -- both present
    Given Kenji has @Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)
    And DarkModeOn annotated with @Variant(booleanValue = true)
    And DarkModeOff annotated with @Variant(booleanValue = false)
    When the annotation processor runs
    Then compilation succeeds

  Scenario: Boolean feature with DefaultVariant satisfies REQUIRED
    Given Kenji has @Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = REQUIRED)
    And DarkModeOn annotated with @Variant(booleanValue = true)
    And DefaultDarkMode annotated with @DefaultVariant
    When the annotation processor runs
    Then compilation succeeds
    And the false case is covered by the default variant

  # --- Runtime Dispatch ---

  Scenario: Integer proxy dispatches via getInt
    Given Kenji has a compiled RetryStrategy with FeatureType.INT
    And the FlagProvider returns Optional.of(3) for getInt("max-retries")
    When Kenji resolves RetryStrategy via FeatureDispatcher
    Then ConservativeRetry is selected and its execute method is called

  Scenario: Integer proxy dispatches to different variant on flag change
    Given Kenji has a compiled RetryStrategy with FeatureType.INT
    And the FlagProvider initially returns 3 for getInt("max-retries")
    When the flag value changes to 10
    And Kenji resolves RetryStrategy again
    Then AggressiveRetry is selected

  Scenario: Boolean proxy dispatches via getBoolean
    Given Kenji has a compiled DarkMode with FeatureType.BOOLEAN
    And the FlagProvider returns Optional.of(true) for getBoolean("dark-mode")
    When Kenji resolves DarkMode via FeatureDispatcher
    Then DarkModeOn is selected

  Scenario: Unmatched typed value triggers fallback strategy
    Given Kenji has RetryStrategy with FeatureType.INT and fallback EXCEPTION
    And the FlagProvider returns Optional.of(99) for getInt("max-retries")
    And 99 matches no variant
    When Kenji resolves RetryStrategy
    Then UnmatchedVariantException is thrown with message including "99" and known values [3, 10]

  Scenario: Typed feature with DefaultVariant handles unmatched value
    Given Kenji has RetryStrategy with FeatureType.INT
    And a DefaultRetry annotated with @DefaultVariant
    And the FlagProvider returns Optional.of(99) for getInt("max-retries")
    When Kenji resolves RetryStrategy
    Then DefaultRetry is selected

  Scenario: Typed feature with getInt returning empty triggers fallback
    Given Kenji has RetryStrategy with FeatureType.INT and fallback NOOP
    And the FlagProvider returns Optional.empty() for getInt("max-retries")
    When Kenji resolves RetryStrategy
    Then the NOOP proxy is used returning safe defaults


Feature: Typed FlagProvider Methods (Conditional API)
  As a Java developer using FlagZen
  I want typed accessor methods on FlagProvider
  So that I can read boolean, int, long, and double flag values without manual parsing

  Scenario: getBoolean parses string "true"
    Given the FlagProvider has flag "feature-x" with string value "true"
    When Kenji calls flags.getBoolean("feature-x")
    Then Optional.of(true) is returned

  Scenario: getBoolean parses string "false"
    Given the FlagProvider has flag "feature-x" with string value "false"
    When Kenji calls flags.getBoolean("feature-x")
    Then Optional.of(false) is returned

  Scenario: getBoolean returns empty for absent flag
    Given the FlagProvider has no flag "feature-x"
    When Kenji calls flags.getBoolean("feature-x")
    Then Optional.empty() is returned

  Scenario: getBoolean returns empty for non-boolean string
    Given the FlagProvider has flag "feature-x" with string value "maybe"
    When Kenji calls flags.getBoolean("feature-x")
    Then Optional.empty() is returned

  Scenario: getInt parses valid integer
    Given the FlagProvider has flag "max-items" with string value "100"
    When Kenji calls flags.getInt("max-items")
    Then Optional.of(100) is returned

  Scenario: getInt returns empty for non-integer string
    Given the FlagProvider has flag "max-items" with string value "abc"
    When Kenji calls flags.getInt("max-items")
    Then Optional.empty() is returned

  Scenario: getLong parses valid long
    Given the FlagProvider has flag "rate-limit" with string value "1000000000"
    When Kenji calls flags.getLong("rate-limit")
    Then Optional.of(1000000000L) is returned

  Scenario: getDouble parses valid double
    Given the FlagProvider has flag "ratio" with string value "0.75"
    When Kenji calls flags.getDouble("ratio")
    Then Optional.of(0.75) is returned

  Scenario: getDouble returns empty for non-numeric string
    Given the FlagProvider has flag "ratio" with string value "high"
    When Kenji calls flags.getDouble("ratio")
    Then Optional.empty() is returned

  Scenario: Typed methods with evaluation context
    Given the FlagProvider has context-dependent flag "max-items"
    And for user "kenji-123" the value is "50"
    When Kenji calls flags.getInt("max-items", context) with targeting key "kenji-123"
    Then Optional.of(50) is returned
