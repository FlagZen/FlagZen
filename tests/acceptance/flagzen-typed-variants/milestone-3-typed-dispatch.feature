Feature: Typed proxy dispatch for feature flags
  As a Java developer using FlagZen with typed features,
  I want generated proxies to dispatch on typed flag values,
  so that variant selection uses the actual flag type without string encoding.

  # --- US-M2-05: INT dispatch ---

  @US-M2-05
  Scenario: Integer proxy dispatches to matching variant
    Given a feature "RetryStrategy" with type INT and variants for integer values 3 and 10
    And a flag provider returning integer 3 for "max-retries"
    When the developer resolves "RetryStrategy"
    Then "ConservativeRetry" handles the method call

  @US-M2-05
  Scenario: Integer proxy follows runtime flag changes
    Given a flag provider initially returning integer 3 for "max-retries"
    When the flag value changes to integer 10
    And the developer calls a method on the resolved "RetryStrategy" proxy
    Then "AggressiveRetry" handles the call

  @US-M2-05
  Scenario: Unmatched integer value with EXCEPTION fallback
    Given a feature "RetryStrategy" with type INT and fallback EXCEPTION
    And a flag provider returning integer 99 for "max-retries"
    When the developer resolves "RetryStrategy"
    Then resolution fails with an unmatched variant error listing known values 3 and 10

  @US-M2-05
  Scenario: Unmatched integer value falls back to default variant
    Given a feature "RetryStrategy" with type INT and a default variant "DefaultRetry"
    And a flag provider returning integer 99 for "max-retries"
    When the developer resolves "RetryStrategy"
    Then "DefaultRetry" handles the method call

  @US-M2-05
  Scenario: Empty integer value triggers NOOP fallback
    Given a feature "RetryStrategy" with type INT and fallback NOOP
    And a flag provider returning no value for "max-retries"
    When the developer resolves "RetryStrategy"
    Then the NOOP proxy is used returning safe defaults

  # --- US-M2-06: BOOLEAN dispatch ---

  @US-M2-06
  Scenario: Boolean proxy dispatches true to matching variant
    Given a boolean feature "DarkMode" with variants for true and false
    And a flag provider returning boolean true for "dark-mode"
    When the developer resolves "DarkMode"
    Then "DarkModeOn" handles the method call

  @US-M2-06
  Scenario: Boolean proxy dispatches false to matching variant
    Given a boolean feature "DarkMode" with variants for true and false
    And a flag provider returning boolean false for "dark-mode"
    When the developer resolves "DarkMode"
    Then "DarkModeOff" handles the method call

  # --- US-M2-06: Typed dispatch with evaluation context ---

  @US-M2-06
  Scenario: Integer dispatch with explicit evaluation context
    Given a context-aware flag provider for "max-retries"
    And it returns integer 3 for plan "free" and integer 10 for plan "enterprise"
    When the developer resolves "RetryStrategy" with context targeting plan "enterprise"
    Then "AggressiveRetry" is selected

  @US-M2-06
  Scenario: Boolean dispatch with explicit evaluation context
    Given a context-aware flag provider returning boolean true for "dark-mode" when preference is "dark"
    When the developer resolves "DarkMode" with context attribute preference "dark"
    Then "DarkModeOn" is selected

  @US-M2-06
  Scenario: Typed dispatch with block-scoped context
    Given a context-aware flag provider for "max-retries" returning integer 3 for free and 10 for enterprise
    When the developer runs a scoped context block targeting plan "enterprise"
    And resolves "RetryStrategy" inside the block
    Then "AggressiveRetry" is selected
    And after the block exits the scoped context is cleared

  @US-M2-06
  Scenario: Explicit context overrides scoped context for typed dispatch
    Given a scoped context block with plan "free"
    When the developer resolves "RetryStrategy" with explicit context targeting plan "enterprise" inside the block
    Then the explicit context wins and "AggressiveRetry" is selected

  @US-M2-06
  Scenario: Typed dispatch with context accessor
    Given a context accessor providing context with plan "enterprise"
    And a context-aware flag provider for "max-retries" returning integer 3 for free and 10 for enterprise
    When the developer resolves typed feature "RetryStrategy" without explicit context
    Then the accessor-provided context is used and "AggressiveRetry" is selected

  @US-M2-06
  Scenario: Typed dispatch without context falls back to default resolution
    Given a flag provider returning integer 3 for "max-retries" regardless of context
    When the developer resolves "RetryStrategy" without any evaluation context
    Then "ConservativeRetry" is selected based on the contextless flag value

  # --- US-M2-07: LONG dispatch ---

  @US-M2-07
  Scenario: Long proxy dispatches to matching variant
    Given a feature "RateLimiter" with type LONG and variants for long values 1000 and 50000
    And a flag provider returning long 50000 for "rate-limit"
    When the developer resolves "RateLimiter"
    Then "HighVolumeLimit" handles the method call

  # --- US-M2-07: DOUBLE dispatch ---

  @US-M2-07
  Scenario: Double proxy dispatches via approximate matching with default tolerance
    Given a double feature "SamplingStrategy"
    And a dispatch variant "LowSampling" at double value 0.1 with default tolerance
    And a flag provider returning double 0.09999999999 for "sampling-ratio"
    When the developer resolves "SamplingStrategy"
    Then "LowSampling" is selected because the value is within tolerance

  @US-M2-07
  Scenario: Double proxy matches variant with explicit tolerance
    Given a double feature "SamplingStrategy"
    And a dispatch variant "MediumSampling" at double value 0.5 with tolerance 0.01
    And a flag provider returning double 0.505 for "sampling-ratio"
    When the developer resolves "SamplingStrategy"
    Then "MediumSampling" is selected because the value is within tolerance

  @US-M2-07
  Scenario: Double proxy rejects value outside all tolerances
    Given a feature "SamplingStrategy" with type DOUBLE and fallback EXCEPTION
    And variants with double values 0.1 and 0.5
    And a flag provider returning double 0.99 for "sampling-ratio"
    When the developer resolves "SamplingStrategy"
    Then resolution fails with an unmatched variant error

  @US-M2-07
  Scenario: Double proxy selects first matching variant when ranges overlap
    Given a double feature "SamplingStrategy"
    And a dispatch variant "LowSampling" at double value 0.1 with tolerance 0.05
    And a dispatch variant "NearLowSampling" at double value 0.12 with tolerance 0.05
    And a flag provider returning double 0.11 for "sampling-ratio"
    When the developer resolves "SamplingStrategy"
    Then "LowSampling" is selected as the first match

  # --- FlagProvider typed methods (parsing) ---

  @US-M2-05
  Scenario: Flag provider integer accessor parses from string value
    Given a flag provider with flag "some-key" having string value "42"
    When the developer reads the integer value for "some-key"
    Then integer 42 is returned

  @US-M2-05
  Scenario: Flag provider boolean accessor rejects non-boolean strings
    Given a flag provider with flag "feature-x" having string value "maybe"
    When the developer reads the boolean value for "feature-x"
    Then no value is returned

  @US-M2-05
  Scenario: Flag provider boolean accessor parses "true" case-insensitively
    Given a flag provider with flag "feature-x" having string value "TRUE"
    When the developer reads the boolean value for "feature-x"
    Then boolean true is returned

  @US-M2-07
  Scenario: Flag provider long accessor returns no value for overflow
    Given a flag provider with flag "big-number" having string value "999999999999999999999"
    When the developer reads the long value for "big-number"
    Then no value is returned

  @US-M2-07
  Scenario: Flag provider double accessor parses valid double
    Given a flag provider with flag "sampling-ratio" having string value "0.75"
    When the developer reads the double value for "sampling-ratio"
    Then double 0.75 is returned

  @US-M2-05 @US-M2-07
  Scenario: All typed accessors return no value for absent flag
    Given a flag provider with no flag "unknown"
    When the developer reads the boolean, integer, long, and double values for "unknown"
    Then all return no value
