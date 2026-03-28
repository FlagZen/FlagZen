Feature: Typed annotation model for feature flags
  As a Java developer using FlagZen,
  I want to declare feature types and annotate variants with typed values,
  so that the annotation model expresses the actual data type for dispatch.

  # --- US-M2-01: FeatureType enum and @Feature.type attribute ---

  @US-M2-01 @pending
  Scenario: Feature with explicit BOOLEAN type compiles
    Given a feature interface "DarkMode" with flag key "dark-mode" and type BOOLEAN
    When the project compiles
    Then compilation succeeds
    And the feature model records type as BOOLEAN

  @US-M2-01 @pending
  Scenario: Feature with explicit LONG type compiles
    Given a feature interface "RateLimiter" with flag key "rate-limit" and type LONG
    When the project compiles
    Then compilation succeeds
    And the feature model records type as LONG

  @US-M2-01 @pending
  Scenario: Feature with explicit DOUBLE type compiles
    Given a feature interface "SamplingStrategy" with flag key "sampling-ratio" and type DOUBLE
    When the project compiles
    Then compilation succeeds
    And the feature model records type as DOUBLE

  @US-M2-01 @pending
  Scenario: Feature without type attribute defaults to STRING
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow" and no type attribute
    When the project compiles
    Then compilation succeeds
    And the feature model records type as STRING
    And existing proxy generation behavior is unchanged

  # --- US-M2-02: Typed @Variant attributes ---

  @US-M2-02 @pending
  Scenario: Variant with integer value for INT feature
    Given a feature "RetryStrategy" with type INT
    And a variant "ConservativeRetry" with integer value 3
    And a variant "AggressiveRetry" with integer value 10
    When the project compiles
    Then compilation succeeds
    And the variant model records integer 3 for "ConservativeRetry"
    And the variant model records integer 10 for "AggressiveRetry"

  @US-M2-02 @pending
  Scenario: Variant with boolean value for BOOLEAN feature
    Given a feature "DarkMode" with type BOOLEAN
    And a variant "DarkModeOn" with boolean value true
    And a variant "DarkModeOff" with boolean value false
    When the project compiles
    Then compilation succeeds
    And the variant model records boolean true for "DarkModeOn"
    And the variant model records boolean false for "DarkModeOff"

  @US-M2-02 @pending
  Scenario: Variant with long value for LONG feature
    Given a feature "RateLimiter" with type LONG
    And a variant "StandardLimit" with long value 1000
    And a variant "HighVolumeLimit" with long value 50000
    When the project compiles
    Then compilation succeeds
    And the variant model records long 1000 for "StandardLimit"

  @US-M2-02 @pending
  Scenario: Variant with approximate double value and default tolerance
    Given a feature "SamplingStrategy" with type DOUBLE
    And a variant "LowSampling" with double value 0.1 and default tolerance
    When the project compiles
    Then compilation succeeds
    And the variant model records double 0.1 with tolerance 1e-10

  @US-M2-02 @pending
  Scenario: Variant with approximate double value and explicit tolerance
    Given a feature "SamplingStrategy" with type DOUBLE
    And a variant "MediumSampling" with double value 0.5 and tolerance 0.01
    When the project compiles
    Then compilation succeeds
    And the variant model records double 0.5 with tolerance 0.01

  @US-M2-02 @pending
  Scenario: Existing string variant remains unchanged
    Given a feature "CheckoutFlow" with type STRING
    And a variant "ClassicCheckout" with string value "CLASSIC"
    When the project compiles
    Then compilation succeeds
    And the variant model records string "CLASSIC" for "ClassicCheckout"

  # --- US-M2-03: @WhenTrue / @WhenFalse convenience annotations ---

  @US-M2-03 @pending
  Scenario: Convenience annotation for true is equivalent to boolean variant true
    Given a feature "DarkMode" with type BOOLEAN
    And a variant "DarkModeOn" annotated as active when true
    When the project compiles
    Then the processor treats it identically to a boolean variant with value true

  @US-M2-03 @pending
  Scenario: Convenience annotation for false is equivalent to boolean variant false
    Given a feature "DarkMode" with type BOOLEAN
    And a variant "DarkModeOff" annotated as active when false
    When the project compiles
    Then the processor treats it identically to a boolean variant with value false

  @US-M2-03 @pending
  Scenario: Convenience annotations with explicit feature target for multi-feature class
    Given a variant "DarkOnMaintenanceOff" implementing both "DarkMode" and "MaintenanceMode"
    And it is annotated as active when true targeting "DarkMode"
    And it is annotated as active when false targeting "MaintenanceMode"
    When the project compiles
    Then boolean true is registered for "DarkMode" on "DarkOnMaintenanceOff"
    And boolean false is registered for "MaintenanceMode" on "DarkOnMaintenanceOff"

  @US-M2-03 @pending
  Scenario: Convenience annotation infers target on single-feature class
    Given a variant "DarkModeOn" implementing only "DarkMode"
    And it is annotated as active when true without an explicit target
    When the project compiles
    Then the processor infers the target feature as "DarkMode"

  @US-M2-03 @pending
  Scenario: Convenience annotation mixed with explicit boolean variant on same feature
    Given a feature "DarkMode" with type BOOLEAN
    And a variant "DarkModeOn" annotated as active when true
    And a variant "DarkModeOff" with boolean value false
    When the project compiles
    Then compilation succeeds with both boolean variants registered
