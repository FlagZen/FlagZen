@spring-test
Feature: Feature proxy bean injection via Spring DI
  As a Spring Boot developer,
  I want feature proxies automatically registered as Spring beans,
  so that I can inject them via @Autowired like any other dependency.

  # --- US-SPRING-02: Register Feature Proxy Beans ---

  @US-SPRING-02
  Scenario: Feature proxy bean registered from discovered feature metadata
    Given CheckoutFlow is a feature interface with generated metadata
    And the application has an auto-configured FeatureDispatcher
    When the application starts
    Then a CheckoutFlow bean is available in the application
    And the bean is the dispatch proxy created by the FeatureDispatcher

  @US-SPRING-02
  Scenario: Multiple feature proxy beans registered for multiple feature interfaces
    Given CheckoutFlow and ShippingMethod are feature interfaces with generated metadata
    And the application has an auto-configured FeatureDispatcher
    When the application starts
    Then both CheckoutFlow and ShippingMethod beans are available
    And each bean is a distinct dispatch proxy

  @US-SPRING-02
  Scenario: Feature proxy injected via constructor autowiring
    Given a CheckoutFlow proxy bean is registered in the application
    And PaymentService declares CheckoutFlow as a constructor dependency
    When PaymentService is created by Spring
    Then PaymentService receives the FlagZen dispatch proxy
    And PaymentService can call methods on the injected CheckoutFlow

  @US-SPRING-02
  Scenario: Injected proxy dispatches dynamically as flag values change
    Given a CheckoutFlow proxy is injected into PaymentService
    And the flag provider returns "CLASSIC" for flag "checkout-flow"
    When PaymentService calls a method on CheckoutFlow
    Then the Classic variant executes
    When the flag provider value changes to "EXPRESS" for flag "checkout-flow"
    And PaymentService calls the method on CheckoutFlow again
    Then the Express variant executes

  @US-SPRING-02
  Scenario: No feature metadata found logs informational message and starts normally
    Given a Spring Boot application with no feature interfaces on the classpath
    When the application starts
    Then no feature proxy beans are registered
    And the application logs that no feature metadata was found
    And the application is running successfully
