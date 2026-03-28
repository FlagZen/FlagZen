Feature: Spring Boot Auto-Configuration for FlagZen
  As a Spring Boot developer using FlagZen
  I want feature proxies injected via @Autowired
  So that I can use feature flags without manual dispatcher wiring

  Background:
    Given a Spring Boot 3.x application with "com.flagzen:flagzen-spring:1.1.0" on the classpath
    And the annotation processor "com.flagzen:flagzen-core:1.1.0" has generated feature metadata

  # --- Step 1: Dependency Resolution ---

  Scenario: FlagZen Spring starter resolves with transitive core dependency
    Given Rafael Oliveira's Spring Boot project uses Gradle
    When he adds "com.flagzen:flagzen-spring:1.1.0" as an implementation dependency
    Then the dependency resolves successfully
    And "com.flagzen:flagzen-core" is included as a transitive dependency

  # --- Step 2: FlagProvider Detection ---

  Scenario: Explicit FlagProvider bean is used by auto-configuration
    Given Rafael defines a FlagProvider @Bean returning a LaunchDarkly provider
    When the Spring application context starts
    Then FlagZenAutoConfiguration detects the FlagProvider bean
    And creates a FeatureDispatcher bean using that FlagProvider
    And no InMemoryFlagProvider is created

  Scenario: InMemoryFlagProvider created when no FlagProvider bean exists
    Given Rafael has not defined any FlagProvider @Bean
    When the Spring application context starts
    Then FlagZenAutoConfiguration creates an InMemoryFlagProvider bean
    And creates a FeatureDispatcher bean using the InMemoryFlagProvider
    And a warning is logged containing "No FlagProvider bean found"

  Scenario: FeatureDispatcher bean is not created when one already exists
    Given Rafael defines both a FlagProvider @Bean and a FeatureDispatcher @Bean
    When the Spring application context starts
    Then FlagZenAutoConfiguration does not create a second FeatureDispatcher
    And Rafael's custom FeatureDispatcher bean is used

  # --- Step 3: Feature Proxy Injection ---

  Scenario: Feature proxy registered as Spring bean via FeatureMetadata discovery
    Given CheckoutFlow is a @Feature interface with generated metadata
    And FlagZenAutoConfiguration has created a FeatureDispatcher bean
    When the Spring application context starts
    Then a bean of type CheckoutFlow is available in the ApplicationContext
    And the bean is the dispatch proxy from FeatureDispatcher.resolve(CheckoutFlow.class)

  Scenario: Multiple feature proxies registered for multiple @Feature interfaces
    Given CheckoutFlow and PaymentMethod are @Feature interfaces with generated metadata
    When the Spring application context starts
    Then beans of type CheckoutFlow and PaymentMethod are both available
    And each bean is a distinct dispatch proxy

  Scenario: Feature proxy injected via constructor @Autowired
    Given CheckoutFlow proxy bean is registered in the ApplicationContext
    When Rafael's CheckoutService declares @Autowired CheckoutFlow in its constructor
    Then Spring injects the FlagZen dispatch proxy
    And CheckoutService can call methods on CheckoutFlow

  # --- Step 4: Runtime Dispatch ---

  Scenario: Proxy dispatches to active variant based on flag value
    Given the CheckoutFlow proxy is injected into CheckoutService
    And the FlagProvider returns "CLASSIC" for flag key "checkout-flow"
    When CheckoutService calls process() on the CheckoutFlow proxy
    Then the ClassicCheckout variant's process() method executes

  Scenario: Proxy reflects flag value changes without restart
    Given the CheckoutFlow proxy is injected into CheckoutService
    And the FlagProvider initially returns "CLASSIC" for "checkout-flow"
    When the FlagProvider value changes to "EXPRESS"
    And CheckoutService calls process() on the CheckoutFlow proxy
    Then the ExpressCheckout variant's process() method executes

  # --- Error Paths ---

  Scenario: Clear error when annotation processor not configured
    Given Rafael added flagzen-spring but forgot the annotationProcessor declaration
    And no FeatureMetadata classes exist on the classpath
    When the Spring application context starts
    Then FlagZenAutoConfiguration logs "No @Feature metadata found on classpath"
    And no feature proxy beans are registered
    And FeatureDispatcher bean exists but resolves no features

  Scenario: Spring fails clearly on duplicate FlagProvider beans
    Given Rafael defines two FlagProvider @Bean methods without @Primary
    When the Spring application context starts
    Then Spring raises NoUniqueBeanDefinitionException for FlagProvider
    And the error message names both conflicting beans
