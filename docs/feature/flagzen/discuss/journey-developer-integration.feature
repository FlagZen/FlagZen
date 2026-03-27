Feature: Java Developer Integrates FlagZen
  As a Java developer working with feature flags,
  I want to replace if/else flag conditionals with type-safe polymorphic dispatch,
  so that my flag-dependent code is cleaner, testable, and provider-independent.

  Background:
    Given Marco Pellegrini is a senior Java developer at a fintech company
    And his project uses Java 17+ with Gradle
    And he currently uses LaunchDarkly for product flags and env vars for infrastructure flags

  # Step 1: Discover
  Scenario: Developer discovers FlagZen value proposition from README
    Given Marco visits the FlagZen GitHub repository
    When he reads the README Quick Start section
    Then he sees a complete @Feature/@Variant polymorphic dispatch example
    And he sees the @PinFlag testing example with 2 lines of setup
    And he understands the value proposition within 30 seconds

  # Step 2: Add Dependency
  Scenario: Developer adds FlagZen to a Gradle project
    Given Marco has a Gradle-based Java 17+ project
    When he adds "com.flagzen:flagzen-core:1.1.0" to implementation dependencies
    And he adds "com.flagzen:flagzen-core:1.1.0" to annotationProcessor dependencies
    And he adds "com.flagzen:flagzen-test:1.1.0" to testImplementation dependencies
    Then the project compiles without errors
    And the annotation processor is discovered via META-INF/services

  Scenario: Developer adds FlagZen to a Maven project
    Given Marco has a Maven-based Java 17+ project
    When he adds flagzen-core as a dependency with scope compile
    And he configures the maven-compiler-plugin with flagzen-core as annotation processor
    And he adds flagzen-test as a dependency with scope test
    Then the project compiles without errors

  # Step 3: Define @Feature Interface
  Scenario: Developer defines a feature with variant enum
    Given Marco creates a Java interface CheckoutFlow
    When he annotates it with @Feature("checkout-flow")
    And he defines an inner enum Variant with values CLASSIC, STREAMLINED, PREMIUM
    Then the annotation processor registers "checkout-flow" as a feature
    And variant values are constrained to the enum

  Scenario: Developer defines a feature without variant enum
    Given Marco creates a Java interface DarkMode
    When he annotates it with @Feature("dark-mode")
    And he does not define a Variant enum
    Then the annotation processor registers "dark-mode" as a feature
    And variant values are validated as free-form strings

  # Step 4: Implement @Variant Classes
  Scenario: Developer implements a valid variant with enum validation
    Given CheckoutFlow is annotated with @Feature("checkout-flow")
    And CheckoutFlow defines Variant enum with CLASSIC, STREAMLINED, PREMIUM
    When Marco creates ClassicCheckout annotated with @Variant("CLASSIC")
    And ClassicCheckout implements CheckoutFlow
    Then compilation succeeds
    And the annotation processor generates CheckoutFlow_FlagZenProxy

  Scenario: Compile error for variant value not in enum
    Given CheckoutFlow defines Variant enum with CLASSIC, STREAMLINED, PREMIUM
    When Marco creates TurboCheckout annotated with @Variant("TURBO")
    And TurboCheckout implements CheckoutFlow
    Then compilation fails
    And the error message states "@Variant(\"TURBO\") does not match any value in CheckoutFlow.Variant"
    And the error lists valid values: CLASSIC, STREAMLINED, PREMIUM

  Scenario: Compile error for duplicate variant value
    Given ClassicCheckout is annotated with @Variant("CLASSIC") implementing CheckoutFlow
    When Marco creates LegacyCheckout also annotated with @Variant("CLASSIC") implementing CheckoutFlow
    Then compilation fails
    And the error identifies both ClassicCheckout and LegacyCheckout as conflicting

  Scenario: Developer implements a class with multiple @Feature variants
    Given CheckoutFlow is annotated with @Feature("checkout-flow")
    And PaymentMethod is annotated with @Feature("payment-method")
    When Marco creates PremiumCreditCheckout with @Variant(of = CheckoutFlow.class, value = "PREMIUM") and @Variant(of = PaymentMethod.class, value = "CREDIT_CARD")
    And PremiumCreditCheckout implements both CheckoutFlow and PaymentMethod
    Then compilation succeeds
    And PremiumCreditCheckout is registered for both features

  Scenario: Developer adds a @DefaultVariant
    Given CheckoutFlow is annotated with @Feature(value = "checkout-flow", fallback = REQUIRED)
    When Marco creates DefaultCheckout annotated with @DefaultVariant
    And DefaultCheckout implements CheckoutFlow
    Then compilation succeeds
    And DefaultCheckout is used when no other variant matches

  # Step 5: Resolve via FeatureDispatcher
  Scenario: Developer resolves a feature to the active variant
    Given an in-memory flag provider returns "STREAMLINED" for "checkout-flow"
    And StreamlinedCheckout is registered as @Variant("STREAMLINED")
    When Marco calls dispatcher.resolve(CheckoutFlow.class)
    Then the returned proxy delegates method calls to StreamlinedCheckout

  Scenario: Proxy follows runtime flag changes
    Given the flag provider initially returns "CLASSIC" for "checkout-flow"
    And Marco has resolved CheckoutFlow via the dispatcher
    When the flag provider value changes to "STREAMLINED"
    Then the next method call on the CheckoutFlow proxy delegates to StreamlinedCheckout

  Scenario: Resolution with evaluation context for A/B testing
    Given the flag provider resolves "checkout-flow" based on user segment
    When Marco calls dispatcher.resolve(CheckoutFlow.class, context) with user "maria.santos@example.com" in segment "premium"
    Then the proxy resolves to the variant matching Maria's segment

  # Step 5 Error Paths
  Scenario: No flag provider configured
    Given no FlagProvider is on the classpath or configured
    When Marco calls dispatcher.resolve(CheckoutFlow.class)
    Then a FlagZenException is thrown
    And the message suggests adding flagzen-env or configuring a custom provider

  Scenario: Flag value has no matching variant with EXCEPTION strategy
    Given CheckoutFlow uses FallbackStrategy.EXCEPTION
    And the flag provider returns "BETA" for "checkout-flow"
    And no @Variant("BETA") exists
    When Marco calls dispatcher.resolve(CheckoutFlow.class).execute(cart)
    Then an UnmatchedVariantException is thrown
    And the message lists known variants: CLASSIC, STREAMLINED, PREMIUM
    And the message suggests adding @Variant("BETA") or @DefaultVariant

  Scenario: Flag value has no matching variant with NOOP strategy
    Given CheckoutFlow uses FallbackStrategy.NOOP
    And the flag provider returns "BETA" for "checkout-flow"
    And no @Variant("BETA") exists
    When Marco calls dispatcher.resolve(CheckoutFlow.class).execute(cart)
    Then no exception is thrown
    And the void method is a no-op

  Scenario: REQUIRED strategy with missing variant triggers compile error
    Given CheckoutFlow uses FallbackStrategy.REQUIRED
    And CheckoutFlow.Variant defines CLASSIC, STREAMLINED, PREMIUM
    And only ClassicCheckout and StreamlinedCheckout exist as @Variant implementations
    When Marco compiles the project
    Then compilation fails
    And the error states variant PREMIUM has no implementation
    And the error suggests adding a @Variant("PREMIUM") class or @DefaultVariant

  # Step 6: Test with @PinFlag
  Scenario: Developer tests with @PinFlag annotation
    Given Marco has a test class with @ExtendWith(FlagZenExtension.class)
    When he annotates a test method with @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    And the test method receives CheckoutFlow as a parameter
    Then the CheckoutFlow resolves to PremiumCheckout for the duration of the test
    And no mock setup or flag provider configuration is needed

  Scenario: Developer tests with programmatic pinning
    Given Marco has a test method receiving TestFlagContext as a parameter
    When he calls flags.pin("checkout-flow", "PREMIUM")
    Then subsequent resolution of CheckoutFlow returns PremiumCheckout
    And the pin is scoped to the current test method only
    And other tests are not affected

  Scenario: Developer tests with file-based flag source
    Given Marco has a file src/test/resources/flags-test.properties containing "checkout-flow=CLASSIC"
    When he annotates the test class with @FlagSource("flags-test.properties")
    Then all tests in the class resolve "checkout-flow" to CLASSIC by default
    And method-level @PinFlag overrides the file source

  Scenario: @PinFlag overrides @FlagSource
    Given the test class has @FlagSource("flags-test.properties") with checkout-flow=CLASSIC
    When a test method has @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    Then that test resolves to PREMIUM
    And other tests without @PinFlag still resolve to CLASSIC

  # Step 7: Spring Boot Integration
  Scenario: Developer uses FlagZen with Spring Boot auto-configuration
    Given Marco has a Spring Boot application
    And "com.flagzen:flagzen-spring:1.1.0" is on the classpath
    When he @Autowires a CheckoutFlow field in a @Service bean
    Then Spring injects the FlagZen proxy
    And the proxy resolves to the active variant based on the configured FlagProvider bean

  Scenario: Variant implementations participate in Spring DI
    Given ClassicCheckout is annotated with both @Variant("CLASSIC") and @Component
    And ClassicCheckout has @Autowired dependencies
    When Spring context starts
    Then ClassicCheckout receives its injected dependencies
    And FlagZen dispatches to the Spring-managed ClassicCheckout instance

  Scenario: FlagZen works without Spring
    Given Marco has a plain Java application without Spring
    When he creates a FeatureDispatcher with FlagZen.dispatcher()
    And he configures an in-memory flag provider
    Then he can resolve @Feature interfaces without any DI framework
