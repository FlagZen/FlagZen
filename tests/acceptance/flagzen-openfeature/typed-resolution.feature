Feature: Typed flag resolution via native OpenFeature methods
  As a Java developer using typed FlagZen features over OpenFeature,
  I want boolean, integer, long, and double flags resolved through native OpenFeature methods,
  so flag values preserve their original types without string round-tripping.

  # --- US-OF-02: Typed resolution happy paths ---

  @US-OF-02
  Scenario: Integer flag resolved natively
    Given the flag management service has integer flag "max-retries" set to 42
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves integer flag "max-retries" through the adapter
    Then the adapter returns integer 42

  @US-OF-02
  Scenario: Double flag resolved natively
    Given the flag management service has double flag "rollout-percentage" set to 0.75
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves double flag "rollout-percentage" through the adapter
    Then the adapter returns double 0.75

  @US-OF-02
  Scenario: Long flag resolved via integer widening
    Given the flag management service has integer flag "event-threshold" set to 100000
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves long flag "event-threshold" through the adapter
    Then the adapter returns long 100000

  # --- US-OF-02: Error and edge paths ---

  @US-OF-02
  Scenario: Typed flag evaluation error returns no value
    Given the flag management service returns an error for boolean flag "dark-mode"
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves boolean flag "dark-mode" through the adapter
    Then the adapter returns no boolean value

  @US-OF-02
  Scenario: Absent typed flag returns no value
    Given the flag management service has no flag named "max-retries"
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves integer flag "max-retries" through the adapter
    Then the adapter returns no integer value
