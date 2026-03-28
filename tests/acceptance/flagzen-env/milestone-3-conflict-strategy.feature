Feature: Conflict handling for multi-convention environments
  As a platform engineer managing services with multiple naming conventions,
  I want configurable conflict handling when multiple environment variables map to the same flag key,
  so that ambiguous configurations are detected and handled according to my risk tolerance.

  # --- US-ENV-07: Multiple parsers ---

  @pending @US-ENV-07
  Scenario: Multiple parsers contribute different flags from different conventions
    Given the developer configures parsers for both "FLAGZEN_" screaming snake case and "myApp" camel case
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    And environment variable "myAppMaxRetries" is set to "5"
    When the provider is built with default conflict handling
    Then looking up flag "checkout-flow" returns "PREMIUM"
    And looking up flag "max-retries" returns "5"

  @pending @US-ENV-07
  Scenario: Multiple parsers with no overlapping flags produce no conflict
    Given the developer configures parsers for both "FLAGZEN_" screaming snake case and "myApp" camel case
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    And no "myApp" prefixed variable maps to "checkout-flow"
    When the provider is built
    Then no conflict warning is produced

  # --- US-ENV-08: Multiple formatters ---

  @pending @US-ENV-08
  Scenario: Multiple formatters produce multiple flag keys from one environment variable
    Given the developer configures formatters for both kebab case and snake case
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    When the provider is built
    Then looking up flag "checkout-flow" returns "PREMIUM"
    And looking up flag "checkout_flow" returns "PREMIUM"

  @pending @US-ENV-08
  Scenario: Single-segment key resolves identically from both formatters
    Given the developer configures formatters for both kebab case and snake case
    And environment variable "FLAGZEN_DARKMODE" is set to "on"
    When the provider is built
    Then looking up flag "darkmode" returns "on"

  # --- US-ENV-09: ConflictStrategy cardinality defaults ---

  @pending @US-ENV-09
  Scenario: Single parser and single formatter default to warn on conflict
    Given the developer configures one parser and one formatter
    And no explicit conflict strategy is set
    When the provider is built
    Then the default conflict strategy is warn

  @pending @US-ENV-09
  Scenario: Multiple parsers and single formatter default to warn on conflict
    Given the developer configures two parsers and one formatter
    And no explicit conflict strategy is set
    When the provider is built
    Then the default conflict strategy is warn

  @pending @US-ENV-09
  Scenario: Single parser and multiple formatters default to warn on conflict
    Given the developer configures one parser and two formatters
    And no explicit conflict strategy is set
    When the provider is built
    Then the default conflict strategy is warn

  @pending @US-ENV-09
  Scenario: Multiple parsers and multiple formatters default to error on conflict
    Given the developer configures two parsers and two formatters
    And no explicit conflict strategy is set
    When the provider is built
    Then the default conflict strategy is error

  @pending @US-ENV-09
  Scenario: Multiple parsers and multiple formatters can be overridden to warn
    Given the developer configures two parsers and two formatters
    And the conflict strategy is explicitly set to warn
    When the provider is built
    Then the conflict strategy is warn

  # --- US-ENV-09: WARN behavior ---

  @pending @US-ENV-09 @US-ENV-07
  Scenario: Warn strategy logs conflict and continues operating
    Given the developer configures two parsers mapping to the same flag key
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    And environment variable "myAppCheckoutFlow" is set to "BASIC"
    And the conflict strategy is set to warn
    When the provider is built
    Then a conflict warning is produced mentioning both environment variable names
    And the provider continues operating normally

  # --- US-ENV-09: ERROR behavior ---

  @pending @US-ENV-09 @US-ENV-07
  Scenario: Error strategy rejects construction when conflict is detected
    Given the developer configures two parsers mapping to the same flag key
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    And environment variable "myAppCheckoutFlow" is set to "BASIC"
    And the conflict strategy is set to error
    When the provider is built
    Then construction fails with a conflict error
    And the error message mentions both environment variable names and flag key "checkout-flow"

  # --- US-ENV-10: First-access conflict warning ---

  @pending @US-ENV-10
  Scenario: First access of a conflicted flag key produces a warning
    Given a provider was built with warn strategy and flag key "checkout-flow" had a conflict
    When the developer looks up flag "checkout-flow" for the first time
    Then a conflict warning is produced at the point of use

  @pending @US-ENV-10
  Scenario: Subsequent access of the same conflicted flag key produces no warning
    Given a provider was built with warn strategy and flag key "checkout-flow" had a conflict
    And the developer has already looked up flag "checkout-flow" once
    When the developer looks up flag "checkout-flow" again
    Then no additional conflict warning is produced

  @pending @US-ENV-10
  Scenario: Non-conflicted flag key produces no warning on access
    Given a provider was built with warn strategy
    And flag key "max-retries" had no conflict during construction
    When the developer looks up flag "max-retries"
    Then no conflict warning is produced
