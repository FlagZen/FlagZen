Feature: Startup diagnostics logging
  As a developer debugging FlagZen integration,
  I want to see a summary of what was auto-configured at startup,
  so that I can diagnose misconfiguration from logs alone.

  # --- US-SPRING-06: Startup Diagnostics Logging ---

  @pending @US-SPRING-06
  Scenario: Startup summary logged with provider and feature details
    Given a Spring Boot application with a FlagProvider and two feature interfaces
    When the application starts
    Then an informational startup summary is logged
    And the summary includes the provider name
    And the summary lists the feature interface names
    And the summary includes the count of registered proxy beans

  @pending @US-SPRING-06
  Scenario: Zero features logged clearly in startup summary
    Given a Spring Boot application with a FlagProvider but no feature interfaces
    When the application starts
    Then the startup summary indicates zero feature proxies registered

  @pending @US-SPRING-06
  Scenario: Individual feature registration logged at debug level
    Given a Spring Boot application with CheckoutFlow as a feature interface
    And diagnostic logging is set to debug level
    When the application starts
    Then a debug message logs the registration of CheckoutFlow
    And the debug message includes the flag key "checkout-flow"
