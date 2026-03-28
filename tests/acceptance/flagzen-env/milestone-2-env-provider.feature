Feature: Environment variable flag provider with eager loading
  As a backend developer running high-throughput services,
  I want flags loaded eagerly into an immutable map at construction time,
  so that flag lookups are fast, predictable, and thread-safe.

  # --- US-ENV-01: Zero-config defaults ---

  @pending @US-ENV-01
  Scenario: Default configuration resolves multi-segment flag key
    Given environment variable "FLAGZEN_MAX_RETRY_COUNT" is set to "5"
    When the developer creates a provider with default configuration
    And the developer looks up flag "max-retry-count"
    Then the flag value is "5"

  @pending @US-ENV-01
  Scenario: Default configuration resolves single-segment flag key
    Given environment variable "FLAGZEN_DARKMODE" is set to "on"
    When the developer creates a provider with default configuration
    And the developer looks up flag "darkmode"
    Then the flag value is "on"

  @pending @US-ENV-01
  Scenario: Non-matching environment variables are excluded from flag map
    Given environment variable "HOME" is set to "/Users/kenji"
    And environment variable "FLAGZEN_DARK_MODE" is set to "true"
    When the developer creates a provider with default configuration
    Then looking up flag "dark-mode" returns "true"
    And looking up flag "home" returns no value

  # --- US-ENV-02: Eager loading and immutable map ---

  @pending @US-ENV-02
  Scenario: Flag lookups are consistent after construction
    Given environment variable "FLAGZEN_MAX_RETRIES" is set to "5"
    And the provider has been constructed with default configuration
    When the developer looks up flag "max-retries" multiple times
    Then every lookup returns "5"

  @pending @US-ENV-02
  Scenario: Empty environment variable value is preserved
    Given environment variable "FLAGZEN_CHECKOUT_FLOW" is set to ""
    When the developer creates a provider with default configuration
    And the developer looks up flag "checkout-flow"
    Then the flag value is ""

  @pending @US-ENV-02
  Scenario: Context-aware lookup ignores evaluation context for static env vars
    Given environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    When the developer creates a provider with default configuration
    And the developer looks up flag "checkout-flow" with an evaluation context
    Then the flag value is "PREMIUM"

  # --- US-ENV-03: ServiceLoader discovery ---

  @pending @US-ENV-03
  Scenario: Provider is discoverable via service loading
    Given the environment variable provider module is on the classpath
    When the service loader discovers available flag providers
    Then the environment variable provider is among the discovered providers

  @pending @US-ENV-03
  Scenario: Auto-discovered provider resolves flags with default configuration
    Given the environment variable provider module is on the classpath
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "STREAMLINED"
    When the developer resolves flag "checkout-flow" through the auto-discovered provider
    Then the flag value is "STREAMLINED"

  # --- US-ENV-04: Custom parser configuration ---

  @pending @US-ENV-04
  Scenario: Builder accepts a custom prefix parser
    Given the developer configures a provider with screaming snake case parser using prefix "FF_"
    And environment variable "FF_CHECKOUT_FLOW" is set to "PREMIUM"
    When the provider is built
    And the developer looks up flag "checkout-flow"
    Then the flag value is "PREMIUM"

  @pending @US-ENV-04
  Scenario: Custom prefix parser excludes non-matching environment variables
    Given the developer configures a provider with screaming snake case parser using prefix "FF_"
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    When the provider is built
    And the developer looks up flag "checkout-flow"
    Then no flag value is returned

  @pending @US-ENV-04
  Scenario: Builder accepts a custom lambda parser
    Given the developer configures a custom parser for "FEAT_" prefixed names
    And environment variable "FEAT_CHECKOUT_FLOW" is set to "BETA"
    When the provider is built
    And the developer looks up flag "checkout-flow"
    Then the flag value is "BETA"

  @pending @US-ENV-04
  Scenario: Builder accepts a custom formatter
    Given the developer configures a provider with snake case formatter
    And environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "PREMIUM"
    When the provider is built
    And the developer looks up flag "checkout_flow"
    Then the flag value is "PREMIUM"

  # --- Error paths ---

  @pending @US-ENV-02
  Scenario: Unparseable integer value returns no typed result but string is available
    Given environment variable "FLAGZEN_MAX_RETRIES" is set to "not-a-number"
    When the developer creates a provider with default configuration
    Then looking up integer flag "max-retries" returns no value
    But looking up string flag "max-retries" returns "not-a-number"

  @pending @US-ENV-02
  Scenario: Unparseable boolean value returns no typed result but string is available
    Given environment variable "FLAGZEN_DARK_MODE" is set to "maybe"
    When the developer creates a provider with default configuration
    Then looking up boolean flag "dark-mode" returns no value
    But looking up string flag "dark-mode" returns "maybe"
