Feature: Block-scoped evaluation context
  As a Java developer with request-handling code,
  I want to scope evaluation context to a block of code
  so that multiple resolve calls share the same context without parameter drilling.

  # --- US-EC-05: FlagContext.run() ---

  @US-EC-05 @pending
  Scenario: Scoped context applies to all resolve calls within block
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a feature "PaymentMethod" with variants "CARD" and "INVOICE"
    And a flag provider that uses targeting key for resolution
    And an evaluation context with targeting key "maria-santos-1042"
    When the developer wraps resolve calls for "CheckoutFlow" and "PaymentMethod" inside a scoped context block
    Then both flag lookups receive the context with targeting key "maria-santos-1042"

  @US-EC-05 @error @pending
  Scenario: Context is cleared after scoped block exits
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And an evaluation context with targeting key "user-7291"
    When the developer completes a scoped context block with that context
    And the developer resolves "CheckoutFlow" after the block
    Then no scoped context is available for that resolve call
    And the flag provider receives a contextless flag lookup

  @US-EC-05 @pending
  Scenario: Nested scoped context uses innermost context
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And an outer evaluation context with targeting key "user-100"
    And an inner evaluation context with targeting key "user-200"
    When the developer nests an inner scoped block inside an outer scoped block
    And resolves "CheckoutFlow" inside the inner block
    Then the flag provider receives targeting key "user-200"

  @US-EC-05 @pending
  Scenario: Outer context restored after inner scoped block exits
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And an outer evaluation context with targeting key "user-100"
    And an inner evaluation context with targeting key "user-200"
    When the developer exits the inner scoped block but remains in the outer block
    And resolves "CheckoutFlow"
    Then the flag provider receives targeting key "user-100"

  @US-EC-05 @pending
  Scenario: Scoped context with supplier returns the result
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that returns "PREMIUM" when targeting key is "user-7291"
    And an evaluation context with targeting key "user-7291"
    When the developer resolves "CheckoutFlow" inside a scoped supplier block
    Then the returned result is the resolved "CheckoutFlow" proxy
    And the proxy dispatches to the "PREMIUM" variant

  @US-EC-05 @error @pending
  Scenario: Exception in scoped block still cleans up context
    Given an evaluation context with targeting key "user-7291"
    When a scoped context block throws an exception
    Then the exception propagates to the caller
    And the scoped context is cleaned up
    And subsequent resolve calls do not see targeting key "user-7291"

  @US-EC-05 @error @pending
  Scenario: Null context in scoped block is rejected
    When the developer attempts to run a scoped block with null context
    Then the operation is rejected with a clear error message

  # --- US-EC-08: ScopedValue / ThreadLocal Carrier ---
  # Note: US-EC-08 is Release 2. Acceptance test validates identical behavior
  # regardless of carrier. The carrier selection itself is an internal optimization.

  @US-EC-08 @pending
  Scenario: Scoped context behavior is identical regardless of runtime version
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that returns "PREMIUM" when targeting key is "user-7291"
    And an evaluation context with targeting key "user-7291"
    When the developer resolves "CheckoutFlow" inside a scoped context block
    Then the resolved proxy dispatches to the "PREMIUM" variant
    And the context is properly scoped to the block regardless of the carrier mechanism

  # --- Thread Safety (Property-Shaped) ---

  @US-EC-05 @property @pending
  Scenario: Each thread sees only its own scoped context
    Given two threads each running scoped context blocks with different targeting keys
    Then each thread's resolve calls use only its own targeting key
    And no cross-thread context leakage occurs
