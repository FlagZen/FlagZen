Feature: Conditional API for non-polymorphic typed flag access
  As a Java developer using FlagZen,
  I want typed accessor methods on the flag provider,
  so that I can read boolean, integer, long, and double flag values without manual parsing.

  @US-M2-08 @pending
  Scenario: Boolean conditional check returns true
    Given a flag provider with flag "maintenance-mode" having string value "true"
    When the developer reads the boolean value for "maintenance-mode"
    Then boolean true is returned

  @US-M2-08 @pending
  Scenario: Boolean conditional check returns false
    Given a flag provider with flag "maintenance-mode" having string value "false"
    When the developer reads the boolean value for "maintenance-mode"
    Then boolean false is returned

  @US-M2-08 @pending
  Scenario: Boolean conditional check returns no value for non-boolean string
    Given a flag provider with flag "feature-x" having string value "maybe"
    When the developer reads the boolean value for "feature-x"
    Then no value is returned

  @US-M2-08 @pending
  Scenario: Integer conditional check returns parsed value
    Given a flag provider with flag "max-items" having string value "200"
    When the developer reads the integer value for "max-items"
    Then integer 200 is returned

  @US-M2-08 @pending
  Scenario: Integer conditional check returns no value for non-numeric string
    Given a flag provider with flag "max-items" having string value "many"
    When the developer reads the integer value for "max-items"
    Then no value is returned

  @US-M2-08 @pending
  Scenario: Long conditional check returns parsed value
    Given a flag provider with flag "rate-limit" having string value "5000000000"
    When the developer reads the long value for "rate-limit"
    Then long 5000000000 is returned

  @US-M2-08 @pending
  Scenario: Long conditional check returns no value for non-numeric string
    Given a flag provider with flag "rate-limit" having string value "unlimited"
    When the developer reads the long value for "rate-limit"
    Then no value is returned

  @US-M2-08 @pending
  Scenario: Long conditional check returns no value for overflow
    Given a flag provider with flag "rate-limit" having string value "999999999999999999999"
    When the developer reads the long value for "rate-limit"
    Then no value is returned

  @US-M2-08 @pending
  Scenario: Double conditional check returns parsed value
    Given a flag provider with flag "sampling-ratio" having string value "0.75"
    When the developer reads the double value for "sampling-ratio"
    Then double 0.75 is returned

  @US-M2-08 @pending
  Scenario: Double conditional check returns no value for non-numeric string
    Given a flag provider with flag "sampling-ratio" having string value "high"
    When the developer reads the double value for "sampling-ratio"
    Then no value is returned

  @US-M2-08 @pending
  Scenario: All typed accessors return no value for absent flag
    Given a flag provider with no flag "unknown"
    When the developer reads the boolean, integer, long, and double values for "unknown"
    Then all return no value

  # --- Context-aware overloads ---

  @US-M2-08 @pending
  Scenario: Integer conditional check with evaluation context
    Given a context-aware flag provider for "max-items"
    And it returns string "500" for targeting key "premium-user"
    When the developer reads the integer value for "max-items" with context targeting "premium-user"
    Then integer 500 is returned

  @US-M2-08 @pending
  Scenario: Boolean conditional check with evaluation context
    Given a context-aware flag provider for "dark-mode"
    And it returns string "true" for targeting key "user-42"
    When the developer reads the boolean value for "dark-mode" with context targeting "user-42"
    Then boolean true is returned

  @US-M2-08 @pending
  Scenario: Long conditional check with evaluation context
    Given a context-aware flag provider for "rate-limit"
    And it returns string "10000000" for targeting key "enterprise-tenant"
    When the developer reads the long value for "rate-limit" with context targeting "enterprise-tenant"
    Then long 10000000 is returned

  @US-M2-08 @pending
  Scenario: Double conditional check with evaluation context
    Given a context-aware flag provider for "sampling-ratio"
    And it returns string "1.0" for targeting key "debug-user"
    When the developer reads the double value for "sampling-ratio" with context targeting "debug-user"
    Then double 1.0 is returned
