@walking-skeleton
Feature: Spring Boot auto-configuration for FlagZen feature proxies
  As a Spring Boot developer using FlagZen,
  I want feature proxies injected via @Autowired with zero manual wiring,
  so that I can use feature flags like any other Spring-managed dependency.

  # Walking Skeleton: thinnest E2E slice through flagzen-spring.
  # Developer defines a FlagProvider bean -> auto-configuration creates
  # FeatureDispatcher -> feature proxy is injectable -> proxy dispatches
  # to the correct variant. Proves: auto-config discovery, bean creation,
  # proxy registration, and runtime dispatch all work together through Spring DI.
  #
  # These scenarios are implemented as JUnit 5 @SpringBootTest integration tests
  # in SpringAutoConfigurationTest.java. The Gherkin serves as the specification.

  @pending @US-SPRING-01 @US-SPRING-02 @US-SPRING-03
  Scenario: Developer injects a feature proxy and dispatches to the active variant
    Given a Spring Boot application with a FlagProvider that returns "CLASSIC" for flag "checkout-flow"
    And CheckoutFlow is a feature interface on the classpath
    When the application starts
    Then the developer can inject CheckoutFlow via autowiring
    And calling a method on the injected CheckoutFlow executes the Classic variant

  @pending @US-SPRING-01 @US-SPRING-04
  Scenario: Application starts with fallback provider when no explicit provider is defined
    Given a Spring Boot application with no FlagProvider bean defined
    And CheckoutFlow is a feature interface on the classpath
    When the application starts
    Then the developer can inject CheckoutFlow via autowiring
    And the injected CheckoutFlow dispatches to the default variant

  @pending @US-SPRING-01 @US-SPRING-05
  Scenario: Custom dispatcher takes precedence over auto-configured one
    Given a Spring Boot application with a custom FeatureDispatcher bean
    When the application starts
    Then only the custom FeatureDispatcher bean exists in the application
    And auto-configuration does not create a second dispatcher
