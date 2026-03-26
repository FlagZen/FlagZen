@milestone-3
Feature: Testing support for flag-dependent code
  As a Java developer testing code that depends on feature flags,
  I want to pin flag values with minimal setup,
  so that my tests are concise, isolated, and free of mock infrastructure.

  # --- US-07: @PinFlag ---

  @US-07
  Scenario: Programmatic pinning via test context
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a test receives a test flag context as a parameter
    When the test pins "checkout-flow" to "PREMIUM" via the context
    And resolves "CheckoutFlow"
    Then the resolved proxy delegates to "PremiumCheckout"
    And the pin is scoped to the current test only

  @US-07
  Scenario: Multiple flags pinned in a single test
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a compiled feature "PaymentMethod" with variants "CREDIT_CARD" and "DEBIT"
    And a test method pinning "checkout-flow" to "PREMIUM" and "payment-method" to "CREDIT_CARD"
    When both features are resolved
    Then "CheckoutFlow" delegates to "PremiumCheckout"
    And "PaymentMethod" delegates to "CreditCardPayment"

  @US-07
  Scenario: Pin values are isolated between tests
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And test A pins "checkout-flow" to "PREMIUM"
    And test B pins "checkout-flow" to "CLASSIC"
    When both tests execute
    Then test A sees "PremiumCheckout"
    And test B sees "ClassicCheckout"
    And neither test affects the other

  @US-07
  Scenario: Feature interface injected as resolved proxy in test parameter
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a test method that pins "checkout-flow" to "PREMIUM"
    And the test method declares "CheckoutFlow" as a parameter
    When the test executes
    Then the "CheckoutFlow" parameter is a proxy resolving to "PremiumCheckout"

  # --- US-08: @FlagSource ---

  @US-08 @pending
  Scenario: Flags loaded from properties file for test class
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a properties file containing "checkout-flow=CLASSIC"
    And a test class configured to load flags from this file
    When a test in the class resolves "CheckoutFlow"
    Then the resolved proxy delegates to "ClassicCheckout"

  @US-08 @pending
  Scenario: Pin annotation overrides file-based flag source
    Given a properties file containing "checkout-flow=CLASSIC"
    And a test class configured to load flags from this file
    And a test method that pins "checkout-flow" to "PREMIUM"
    When the test resolves "CheckoutFlow"
    Then the resolved proxy delegates to "PremiumCheckout"
    And other tests in the class still resolve to "ClassicCheckout"

  @US-08 @pending
  Scenario: Missing flag source file produces a clear error
    Given a test class configured to load flags from "nonexistent.properties"
    When the test class initializes
    Then a clear error is raised stating the file was not found
    And the searched locations are listed in the message

  @US-08 @property @pending
  Scenario: Pin always takes priority over file source regardless of configuration order
    Given any test with both a file-based flag source and a pin annotation for the same flag
    When the flag is resolved
    Then the pinned value is always used over the file value
