Feature: Evaluation Context for Targeted Flag Resolution
  As a Java developer using FlagZen for polymorphic dispatch,
  I need to pass evaluation context (user, tenant, attributes) to flag resolution
  so that flags resolve differently per user for A/B testing and segmentation.

  Background:
    Given FlagZen M1 is on the classpath
    And a FeatureDispatcher is configured with a FlagProvider

  # --- Step 1: Discover Context API ---

  Scenario: Context-aware resolve method is available
    When Kenji Tanaka views the FeatureDispatcher interface
    Then both resolve(Class) and resolve(Class, EvaluationContext) methods are available
    And the Javadoc explains context is used for targeted flag resolution

  # --- Step 2: Model Evaluation Context ---

  Scenario: Create EvaluationContext with targeting key and attributes
    When Kenji Tanaka builds an EvaluationContext with:
      | targetingKey | user-7291              |
      | plan         | enterprise             |
      | region       | eu-west                |
      | beta-tester  | true                   |
    Then the context targeting key is "user-7291"
    And the context attribute "plan" is "enterprise"
    And the context attribute "region" is "eu-west"
    And the context attribute "beta-tester" is true

  Scenario: Create EvaluationContext without targeting key
    When Kenji Tanaka builds an EvaluationContext with only attribute "region" = "eu-west"
    Then the context is valid
    And the targeting key is null
    And the context attribute "region" is "eu-west"

  # Immutability of EvaluationContext is an architectural constraint, not a runtime behavior.
  # Verified via unit test (no setters, unmodifiable collections) rather than a Gherkin scenario.

  # --- Step 3: Pass Explicit Context to resolve() ---

  Scenario: Explicit context is passed to FlagProvider
    Given a FlagProvider that returns "PREMIUM" when context attribute "plan" is "enterprise"
    And an EvaluationContext with targeting key "user-7291" and attribute "plan" = "enterprise"
    When Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class, context)
    Then the FlagProvider receives getString("checkout-flow", context)
    And the resolved proxy dispatches to the PREMIUM variant

  Scenario: Resolve without context is backward compatible
    Given a FlagProvider that returns "CLASSIC" for getString("checkout-flow")
    When Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class)
    Then the FlagProvider receives getString("checkout-flow") without context
    And the resolved proxy dispatches to the CLASSIC variant

  Scenario: FlagProvider without context overload falls back gracefully
    Given a FlagProvider that only implements getString(String key)
    And an EvaluationContext with targeting key "user-7291"
    When Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class, context)
    Then the FlagProvider's getString(String, EvaluationContext) default method delegates to getString(String)
    And the context is effectively ignored by that provider

  # --- Step 4: Block-Scoped Context via FlagContext.run() ---

  Scenario: Block-scoped context applies to all resolve calls within block
    Given an EvaluationContext with targeting key "user-7291" and attribute "plan" = "enterprise"
    When Kenji Tanaka wraps two resolve calls inside FlagContext.run(context, ...)
    Then both resolve(CheckoutFlow.class) and resolve(PaymentMethod.class) use the scoped context
    And the FlagProvider receives the context for both flag lookups

  Scenario: Explicit context overrides block-scoped context
    Given a block-scoped context A with targeting key "user-7291"
    And an explicit context B with targeting key "user-vip-42"
    When Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class, contextB) inside FlagContext.run(contextA, ...)
    Then context B is used for that resolve call
    And context A is ignored for that specific call

  Scenario: Context is cleared after FlagContext.run() block exits
    Given Kenji Tanaka has completed a FlagContext.run() block with targeting key "user-7291"
    When he calls dispatcher.resolve(CheckoutFlow.class) after the block
    Then no scoped context is available for that call
    And the resolve call falls through to ContextAccessor or default

  Scenario: Nested FlagContext.run() uses innermost context
    Given an outer FlagContext.run() with context A (targeting key "user-100")
    And an inner FlagContext.run() with context B (targeting key "user-200")
    When dispatcher.resolve(CheckoutFlow.class) is called inside the inner block
    Then context B is used
    When dispatcher.resolve(CheckoutFlow.class) is called after the inner block but inside the outer
    Then context A is used

  Scenario: FlagContext.run() with Supplier returns the result
    Given an EvaluationContext with targeting key "user-7291"
    When Kenji Tanaka calls FlagContext.run(context, () -> dispatcher.resolve(CheckoutFlow.class))
    Then the result is the resolved CheckoutFlow proxy
    And the context was used for that resolution

  # --- Step 5: ContextAccessor SPI ---

  Scenario: Custom ContextAccessor provides context when no explicit context given
    Given a RequestContextAccessor registered via ServiceLoader with priority 100
    And the RequestContextAccessor returns context with targeting key "request-user-555"
    When dispatcher.resolve(CheckoutFlow.class) is called without explicit context
    Then the ContextAccessor's context is used
    And the FlagProvider receives the accessor-provided context

  Scenario: ContextAccessor is skipped when explicit context is provided
    Given a RequestContextAccessor registered with priority 100
    And an explicit EvaluationContext with targeting key "user-vip-42"
    When Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class, explicitContext)
    Then the ContextAccessor is not invoked
    And the explicit context is used

  Scenario: Multiple ContextAccessors resolve by priority
    Given a ContextAccessor "ReactorAccessor" with priority 50 returning context with targeting key "reactor-user"
    And a ContextAccessor "ServletAccessor" with priority 100 returning context with targeting key "servlet-user"
    When dispatcher.resolve(CheckoutFlow.class) is called without explicit context
    Then "ReactorAccessor" (priority 50) is consulted first
    And its context with targeting key "reactor-user" is used

  Scenario: ContextAccessor returns empty
    Given a ContextAccessor that returns Optional.empty()
    And a block-scoped context with targeting key "scoped-user"
    When dispatcher.resolve(CheckoutFlow.class) is called
    Then the ContextAccessor's empty result is skipped
    And the block-scoped context is used

  # --- Step 6: Resolution Order ---

  Scenario: Full resolution order -- explicit wins over all
    Given all four context sources are present:
      | source      | targeting_key     |
      | explicit    | explicit-user     |
      | accessor    | accessor-user     |
      | scoped      | scoped-user       |
      | default     | default-user      |
    When dispatcher.resolve(CheckoutFlow.class, explicitContext) is called
    Then the FlagProvider receives context with targeting key "explicit-user"

  Scenario: Full resolution order -- accessor wins when no explicit
    Given no explicit context is passed
    And a ContextAccessor returns targeting key "accessor-user"
    And a scoped context has targeting key "scoped-user"
    And a default context has targeting key "default-user"
    When dispatcher.resolve(CheckoutFlow.class) is called
    Then the FlagProvider receives context with targeting key "accessor-user"

  Scenario: Full resolution order -- scoped wins when no explicit or accessor
    Given no explicit context is passed
    And no ContextAccessor is registered
    And a scoped context has targeting key "scoped-user"
    And a default context has targeting key "default-user"
    When dispatcher.resolve(CheckoutFlow.class) is called inside FlagContext.run()
    Then the FlagProvider receives context with targeting key "scoped-user"

  Scenario: Full resolution order -- default is last resort
    Given no explicit context, no ContextAccessor, no scoped context
    And a default context configured via FlagZen.configure(c -> c.defaultContext(ctx))
    When dispatcher.resolve(CheckoutFlow.class) is called
    Then the FlagProvider receives the default context

  Scenario: No context at all -- M0 backward compatibility
    Given no explicit context, no ContextAccessor, no scoped context, no default context
    When dispatcher.resolve(CheckoutFlow.class) is called
    Then the FlagProvider receives getString("checkout-flow") without any context
    And behavior is identical to M0

  # --- Thread Safety ---

  @property
  Scenario: EvaluationContext is thread-safe
    Given an EvaluationContext instance shared across multiple threads
    Then concurrent reads of targeting key and attributes never produce inconsistent results
    And no synchronization is needed by the caller

  @property
  Scenario: FlagContext.run() is thread-safe
    Given multiple threads each running FlagContext.run() with different contexts
    Then each thread sees only its own scoped context
    And no cross-thread context leakage occurs

  @property
  Scenario: Zero runtime reflection maintained
    Given the flagzen-core module with evaluation context support
    Then no classes in com.flagzen use java.lang.reflect at runtime
    And all dispatch remains compile-time generated code
