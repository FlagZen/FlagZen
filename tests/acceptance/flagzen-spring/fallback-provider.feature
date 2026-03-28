Feature: InMemoryFlagProvider fallback with diagnostic warning
  As a developer onboarding to a FlagZen-enabled project,
  I want the application to start even without a FlagProvider configured,
  so that I can develop iteratively and see a clear warning about the missing provider.

  # --- US-SPRING-04: InMemoryFlagProvider Fallback ---

  @pending @US-SPRING-04
  Scenario: Fallback provider created when no FlagProvider bean is defined
    Given a Spring Boot application with flagzen-spring on the classpath
    And no FlagProvider bean is defined
    When the application starts
    Then an InMemoryFlagProvider fallback is active
    And a FeatureDispatcher bean is created using the fallback provider

  @pending @US-SPRING-04
  Scenario: Warning logged when fallback provider is activated
    Given a Spring Boot application with no FlagProvider bean defined
    When the application starts
    Then a warning is logged containing "No FlagProvider bean found"
    And the warning mentions "InMemoryFlagProvider"
    And the warning mentions "dev/test only"

  @pending @US-SPRING-04
  Scenario: Features dispatch to default variant with fallback provider
    Given a Spring Boot application using the InMemoryFlagProvider fallback
    And CheckoutFlow is a feature interface with a default variant
    When the developer calls a method on the injected CheckoutFlow
    Then the default variant executes

  @pending @US-SPRING-04
  Scenario: No fallback provider created when explicit FlagProvider exists
    Given a Spring Boot application with an explicit FlagProvider bean
    When the application starts
    Then no InMemoryFlagProvider fallback is created
    And no "No FlagProvider bean found" warning is logged

  # --- US-SPRING-05: ConditionalOnMissingBean Guards ---

  @pending @US-SPRING-05
  Scenario: Custom FlagProvider prevents fallback provider creation
    Given a Spring Boot application with a custom FlagProvider bean
    When the application starts
    Then the FeatureDispatcher uses the custom flag provider
    And no InMemoryFlagProvider fallback is created

  @pending @US-SPRING-05
  Scenario: Feature proxy beans use custom FeatureDispatcher when provided
    Given a Spring Boot application with a custom FeatureDispatcher bean
    And CheckoutFlow is a feature interface with generated metadata
    When the application starts
    Then the CheckoutFlow proxy bean is resolved through the custom dispatcher

  @pending @US-SPRING-05
  Scenario: Full override with all custom beans causes zero auto-configuration
    Given a Spring Boot application with custom FlagProvider, FeatureDispatcher, and feature proxy beans
    When the application starts
    Then auto-configuration creates no beans
    And all custom beans are used throughout the application
