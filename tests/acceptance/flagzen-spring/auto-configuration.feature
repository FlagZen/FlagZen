@spring-test
Feature: FeatureDispatcher auto-configuration from FlagProvider
  As a Spring Boot developer,
  I want a FeatureDispatcher bean created automatically from my FlagProvider,
  so that I never write manual dispatcher wiring code.

  # --- US-SPRING-01: Auto-Configure FeatureDispatcher ---

  @US-SPRING-01
  Scenario: FeatureDispatcher bean created from explicit FlagProvider bean
    Given a Spring Boot application with a FlagProvider bean named "envVarProvider"
    When the application starts
    Then a FeatureDispatcher bean exists in the application
    And the FeatureDispatcher uses the "envVarProvider" flag provider

  @US-SPRING-01
  Scenario: Auto-configuration backs off when FeatureDispatcher already exists
    Given a Spring Boot application with a custom FeatureDispatcher bean
    And a FlagProvider bean also exists
    When the application starts
    Then only the custom FeatureDispatcher bean exists in the application
    And auto-configuration does not create a second dispatcher

  @US-SPRING-01
  Scenario: Auto-configuration discovered via Spring Boot imports mechanism
    Given the flagzen-spring module is on the classpath
    When Spring Boot scans for auto-configuration classes
    Then FlagZenAutoConfiguration is discovered and processed

  # --- US-SPRING-03: FlagProvider Bean Detection ---

  @US-SPRING-03
  Scenario: Profile-specific FlagProvider is used for the active profile
    Given a Spring Boot application with a production FlagProvider for profile "prod"
    And a development FlagProvider for profile "dev"
    When the application starts with active profile "prod"
    Then the FeatureDispatcher uses the production flag provider

  @US-SPRING-03
  Scenario: Ambiguous FlagProvider beans fail with a clear error
    Given a Spring Boot application with two FlagProvider beans and no primary designation
    When the application attempts to start
    Then the application fails to start
    And the error identifies both conflicting flag provider beans

  @US-SPRING-03
  Scenario: FlagProvider from another FlagZen module is auto-detected
    Given a Spring Boot application with flagzen-env on the classpath
    And flagzen-env registers its own FlagProvider bean
    When the application starts
    Then the FeatureDispatcher uses the flagzen-env flag provider
    And the developer has written zero provider configuration
