Feature: Context accessor SPI and resolution order
  As a Java developer with multiple context sources,
  I want a deterministic resolution order for evaluation context
  so that flag resolution is predictable when multiple sources are active.

  # --- US-EC-06: ContextAccessor SPI ---

  @US-EC-06 @pending
  Scenario: Context accessor provides context when no explicit context given
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And a context accessor registered with priority 100 returning targeting key "accessor-user-99"
    When the developer resolves "CheckoutFlow" without explicit context
    Then the flag provider receives context with targeting key "accessor-user-99"

  @US-EC-06 @pending
  Scenario: Lower priority accessor is consulted first
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And a context accessor "ReactorAccessor" with priority 50 returning targeting key "reactor-user"
    And a context accessor "ServletAccessor" with priority 100 returning targeting key "servlet-user"
    When the developer resolves "CheckoutFlow" without explicit context
    Then the flag provider receives context with targeting key "reactor-user"

  @US-EC-06 @error @pending
  Scenario: Accessor returning empty is skipped
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And a context accessor with priority 50 returning no context
    And a context accessor with priority 100 returning targeting key "servlet-user"
    When the developer resolves "CheckoutFlow" without explicit context
    Then the empty accessor is skipped
    And the flag provider receives context with targeting key "servlet-user"

  @US-EC-06 @error @pending
  Scenario: No accessor registered is handled gracefully
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    And an in-memory flag provider with "checkout-flow" set to "CLASSIC"
    And no context accessors are registered
    When the developer resolves "CheckoutFlow" without explicit context
    Then the accessor step is skipped without error
    And the resolve falls through to scoped or default context

  # --- US-EC-07: Resolution Order ---

  @US-EC-07 @pending
  Scenario: Explicit context beats all other sources
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And all four context sources are active:
      | source   | targeting key     |
      | explicit | explicit-user     |
      | accessor | accessor-user     |
      | scoped   | scoped-user       |
      | default  | default-user      |
    When the developer resolves "CheckoutFlow" with the explicit context
    Then the flag provider receives context with targeting key "explicit-user"

  @US-EC-07 @pending
  Scenario: Accessor beats scoped and default when no explicit context
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And a context accessor returning targeting key "accessor-user"
    And a scoped context with targeting key "scoped-user"
    And a default context with targeting key "default-user"
    When the developer resolves "CheckoutFlow" without explicit context
    Then the flag provider receives context with targeting key "accessor-user"

  @US-EC-07 @pending
  Scenario: Scoped context beats default when no explicit or accessor
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And no context accessors are registered
    And a scoped context with targeting key "scoped-user"
    And a default context with targeting key "default-user"
    When the developer resolves "CheckoutFlow" inside the scoped block
    Then the flag provider receives context with targeting key "scoped-user"

  @US-EC-07 @pending
  Scenario: Default context is used as last resort
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And no context accessors are registered
    And no scoped context is active
    And a default context configured with targeting key "default-user"
    When the developer resolves "CheckoutFlow" without explicit context
    Then the flag provider receives context with targeting key "default-user"

  @US-EC-07 @pending
  Scenario: No context at all preserves pre-context behavior
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    And an in-memory flag provider with "checkout-flow" set to "CLASSIC"
    And no context accessors are registered
    And no scoped context is active
    And no default context is configured
    When the developer resolves "CheckoutFlow" without explicit context
    Then the flag provider receives a contextless flag lookup
    And the resolved proxy dispatches to the "CLASSIC" variant

  @US-EC-07 @error @pending
  Scenario: Explicit context overrides scoped context within a block
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And a scoped context with targeting key "scoped-user"
    And an explicit evaluation context with targeting key "explicit-override"
    When the developer resolves "CheckoutFlow" with the explicit context inside the scoped block
    Then the flag provider receives context with targeting key "explicit-override"

  # --- Property-Shaped: Resolution Order Invariant ---

  @US-EC-07 @property @pending
  Scenario: Resolution order is deterministic regardless of registration order
    Given any combination of context sources
    When the developer resolves a feature multiple times with the same sources active
    Then the same context source wins every time
    And the order is always: explicit, then accessor, then scoped, then default
