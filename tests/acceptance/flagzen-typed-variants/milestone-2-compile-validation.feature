Feature: Compile-time type validation for typed features
  As a Java developer using FlagZen with typed features,
  I want the annotation processor to reject type mismatches at compile time,
  so that I catch errors before runtime dispatch fails silently.

  # --- Attribute mismatch: wrong attribute for feature type ---

  @US-M2-04 @pending
  Scenario: String variant on INT feature is rejected
    Given a feature "RetryStrategy" with type INT
    And a variant "ConservativeRetry" with string value "3"
    When the project compiles
    Then compilation fails with an error on "ConservativeRetry"
    And the error message mentions type INT and string value
    And the error suggests using integer value 3

  @US-M2-04 @pending
  Scenario: Integer variant on BOOLEAN feature is rejected
    Given a feature "DarkMode" with type BOOLEAN
    And a variant "DarkModeOn" with integer value 1
    When the project compiles
    Then compilation fails with an error on "DarkModeOn"
    And the error suggests using a boolean variant

  @US-M2-04 @pending
  Scenario: Boolean variant on LONG feature is rejected
    Given a feature "RateLimiter" with type LONG
    And a variant with boolean value true
    When the project compiles
    Then compilation fails suggesting a long value variant

  @US-M2-04 @pending
  Scenario: Integer variant on DOUBLE feature is rejected
    Given a feature "SamplingStrategy" with type DOUBLE
    And a variant with integer value 1
    When the project compiles
    Then compilation fails suggesting an approximate double value variant

  @US-M2-04 @pending
  Scenario: Long variant on INT feature is rejected
    Given a feature "RetryStrategy" with type INT
    And a variant with long value 1000
    When the project compiles
    Then compilation fails suggesting an integer value variant

  # --- Mixed attribute types within same feature ---

  @US-M2-04 @pending
  Scenario: Mixed variant attributes within same feature are rejected
    Given a feature "RetryStrategy" with type INT
    And a variant "ConservativeRetry" with integer value 3
    And a variant "AggressiveRetry" with string value "fast"
    When the project compiles
    Then compilation fails identifying "AggressiveRetry" as using the wrong attribute

  # --- Duplicate typed values ---

  @US-M2-04 @pending
  Scenario: Duplicate integer values for the same feature are rejected
    Given a feature "RetryStrategy" with type INT
    And a variant "ConservativeRetry" with integer value 3
    And a variant "CautiousRetry" with integer value 3
    When the project compiles
    Then compilation fails with a duplicate variant value error for integer 3

  @US-M2-04 @pending
  Scenario: Duplicate long values for the same feature are rejected
    Given a feature "RateLimiter" with type LONG
    And a variant "StandardLimit" with long value 1000
    And a variant "BasicLimit" with long value 1000
    When the project compiles
    Then compilation fails with a duplicate variant value error for long 1000

  @US-M2-04 @pending
  Scenario: Duplicate boolean true values from mixed annotations are rejected
    Given a feature "DarkMode" with type BOOLEAN
    And a variant "DarkModeOn" annotated as active when true
    And a variant "AlsoDarkModeOn" with boolean value true
    When the project compiles
    Then compilation fails with a duplicate variant value error for boolean true

  # --- BOOLEAN REQUIRED completeness ---

  @US-M2-04 @pending
  Scenario: Boolean REQUIRED feature missing false variant is rejected
    Given a feature "DarkMode" with type BOOLEAN and fallback REQUIRED
    And a variant "DarkModeOn" annotated as active when true
    And no variant for false and no default variant
    When the project compiles
    Then compilation fails requiring variants for both true and false

  @US-M2-04 @pending
  Scenario: Boolean REQUIRED feature with both variants compiles
    Given a feature "DarkMode" with type BOOLEAN and fallback REQUIRED
    And a variant "DarkModeOn" annotated as active when true
    And a variant "DarkModeOff" annotated as active when false
    When the project compiles
    Then compilation succeeds

  @US-M2-04 @pending
  Scenario: Boolean REQUIRED feature satisfied by default variant
    Given a feature "DarkMode" with type BOOLEAN and fallback REQUIRED
    And a variant "DarkModeOn" annotated as active when true
    And a default variant "FallbackDarkMode"
    When the project compiles
    Then compilation succeeds

  # --- Error message quality ---

  @US-M2-04 @pending
  Scenario: Type mismatch error message includes feature name and variant name
    Given a feature "RetryStrategy" with flag key "max-retries" and type INT
    And a variant "ConservativeRetry" with string value "3"
    When the project compiles
    Then the error message includes "max-retries" and "ConservativeRetry"

  @US-M2-04 @pending @property
  Scenario: Every type mismatch produces an actionable suggested fix
    Given any feature type and a variant using the wrong attribute
    When the annotation processor validates the variant
    Then the error message includes a suggested fix with the correct attribute syntax
