Feature: Environment Variable Flag Provider
  As a backend developer deploying 12-factor apps
  I want to source feature flag values from environment variables
  So that ops can toggle features per environment without code changes or external services

  Background:
    Given the flagzen-env module is on the classpath

  # --- Zero-Config Defaults ---

  Scenario: Default config resolves FLAGZEN_ prefixed env var to kebab-case flag key
    Given environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
    When Kenji creates a provider with EnvironmentVariableFlagProvider.create()
    And Kenji calls getString("checkout-flow")
    Then the result is "PREMIUM"

  Scenario: Default config with multi-segment flag key
    Given environment variable FLAGZEN_MAX_RETRY_COUNT is set to "5"
    When Kenji creates a provider with EnvironmentVariableFlagProvider.create()
    And Kenji calls getString("max-retry-count")
    Then the result is "5"

  Scenario: Default config with single-segment flag key
    Given environment variable FLAGZEN_DARKMODE is set to "on"
    When Kenji creates a provider with EnvironmentVariableFlagProvider.create()
    And Kenji calls getString("darkmode")
    Then the result is "on"

  Scenario: Non-matching env vars are excluded from the flag map
    Given environment variable HOME is set to "/Users/kenji"
    And environment variable FLAGZEN_DARK_MODE is set to "true"
    When Kenji creates a provider with EnvironmentVariableFlagProvider.create()
    Then getString("dark-mode") returns "true"
    And getString("home") returns empty

  # --- Eager Loading and Immutable Map ---

  Scenario: Provider loads all env vars at construction time
    Given environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
    When Kenji constructs an EnvironmentVariableFlagProvider
    Then getString("checkout-flow") returns "PREMIUM"
    And no System.getenv() call occurs after construction

  Scenario: getString is a pure map lookup
    Given environment variable FLAGZEN_MAX_RETRIES is set to "5"
    And the provider has been constructed
    When Kenji calls getString("max-retries") multiple times
    Then every call returns "5"

  Scenario: Typed resolution delegates to FlagProvider defaults
    Given environment variable FLAGZEN_MAX_RETRIES is set to "5"
    When Kenji calls getInt("max-retries")
    Then the result is 5

  Scenario: Boolean flag resolution
    Given environment variable FLAGZEN_DARK_MODE is set to "true"
    When Kenji calls getBoolean("dark-mode")
    Then the result is true

  Scenario: Long flag resolution
    Given environment variable FLAGZEN_RATE_LIMIT is set to "100000"
    When Kenji calls getLong("rate-limit")
    Then the result is 100000

  Scenario: Double flag resolution
    Given environment variable FLAGZEN_SAMPLING_RATIO is set to "0.1"
    When Kenji calls getDouble("sampling-ratio")
    Then the result is approximately 0.1

  # --- ServiceLoader Discovery ---

  Scenario: Auto-discovery via ServiceLoader
    Given Kenji has flagzen-env on the classpath
    And no FlagProvider is explicitly configured
    When FlagZen.create() is called
    Then EnvironmentVariableFlagProvider is available via ServiceLoader

  Scenario: ServiceLoader file contains correct FQCN
    Given the file META-INF/services/com.flagzen.spi.FlagProvider exists in flagzen-env
    When its contents are read
    Then it contains "com.flagzen.env.EnvironmentVariableFlagProvider"

  Scenario: Provider works without explicit registration
    Given Kenji has flagzen-env on the classpath
    And environment variable FLAGZEN_CHECKOUT_FLOW is set to "STREAMLINED"
    When Kenji resolves the "checkout-flow" flag through the auto-discovered provider
    Then the value "STREAMLINED" is returned

  # --- Built-in Parsers: screamingSnakeCase ---

  Scenario: screamingSnakeCase with prefix parses matching env var
    Given parser FlagKeyParsers.screamingSnakeCase("FLAGZEN_")
    When the parser receives "FLAGZEN_CHECKOUT_FLOW"
    Then it returns segments ["checkout", "flow"]

  Scenario: screamingSnakeCase with prefix rejects non-matching env var
    Given parser FlagKeyParsers.screamingSnakeCase("FLAGZEN_")
    When the parser receives "HOME"
    Then it returns empty

  Scenario: screamingSnakeCase without prefix parses any SCREAMING_SNAKE var
    Given parser FlagKeyParsers.screamingSnakeCase() with no prefix
    When the parser receives "CHECKOUT_FLOW"
    Then it returns segments ["checkout", "flow"]

  Scenario: screamingSnakeCase single-segment
    Given parser FlagKeyParsers.screamingSnakeCase("FLAGZEN_")
    When the parser receives "FLAGZEN_DARKMODE"
    Then it returns segments ["darkmode"]

  # --- Built-in Parsers: camelCase ---

  Scenario: camelCase with prefix parses matching env var
    Given parser FlagKeyParsers.camelCase("myApp")
    When the parser receives "myAppCheckoutFlow"
    Then it returns segments ["checkout", "flow"]

  Scenario: camelCase with prefix rejects non-matching env var
    Given parser FlagKeyParsers.camelCase("myApp")
    When the parser receives "FLAGZEN_CHECKOUT_FLOW"
    Then it returns empty

  Scenario: camelCase without prefix parses bare camelCase var
    Given parser FlagKeyParsers.camelCase() with no prefix
    When the parser receives "checkoutFlow"
    Then it returns segments ["checkout", "flow"]

  # --- Custom Lambda Parser ---

  Scenario: Custom lambda parser matches project-specific convention
    Given Kenji configures a custom lambda parser for "FEAT_" prefixed env vars
    And environment variable FEAT_CHECKOUT_FLOW is set to "BETA"
    When the provider is constructed and Kenji calls getString("checkout-flow")
    Then the result is "BETA"

  # --- Built-in Formatters ---

  Scenario: kebabCase formatter joins segments with hyphens
    Given formatter FlagKeyFormats.kebabCase()
    When it formats segments ["checkout", "flow"]
    Then the flag key is "checkout-flow"

  Scenario: snakeCase formatter joins segments with underscores
    Given formatter FlagKeyFormats.snakeCase()
    When it formats segments ["checkout", "flow"]
    Then the flag key is "checkout_flow"

  Scenario: camelCase formatter capitalizes subsequent segments
    Given formatter FlagKeyFormats.camelCase()
    When it formats segments ["checkout", "flow"]
    Then the flag key is "checkoutFlow"

  Scenario: pascalCase formatter capitalizes all segments
    Given formatter FlagKeyFormats.pascalCase()
    When it formats segments ["checkout", "flow"]
    Then the flag key is "CheckoutFlow"

  Scenario: dotCase formatter joins segments with dots
    Given formatter FlagKeyFormats.dotCase()
    When it formats segments ["checkout", "flow"]
    Then the flag key is "checkout.flow"

  Scenario: colonCase formatter joins segments with colons
    Given formatter FlagKeyFormats.colonCase()
    When it formats segments ["checkout", "flow"]
    Then the flag key is "checkout:flow"

  Scenario: Single segment formatted without delimiter
    Given formatter FlagKeyFormats.kebabCase()
    When it formats segments ["darkmode"]
    Then the flag key is "darkmode"

  # --- Custom Lambda Formatter ---

  Scenario: Custom lambda formatter uses custom delimiter
    Given a custom FlagKeyFormat lambda that joins with "/"
    When it formats segments ["checkout", "flow"]
    Then the flag key is "checkout/flow"

  # --- Multiple Parsers ---

  Scenario: Multiple parsers contribute different flags
    Given the provider is configured with parsers screamingSnakeCase("FLAGZEN_") and camelCase("myApp")
    And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
    And environment variable myAppMaxRetries is set to "5"
    When the provider is constructed
    Then getString("checkout-flow") returns "PREMIUM"
    And getString("max-retries") returns "5"

  Scenario: Conflict from multiple parsers triggers warning
    Given the provider is configured with parsers screamingSnakeCase("FLAGZEN_") and camelCase("myApp")
    And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
    And environment variable myAppCheckoutFlow is set to "BASIC"
    And conflict strategy is WARN
    When the provider is constructed
    Then a warning is logged mentioning both env var names
    And the provider continues operating

  # --- Multiple Formatters ---

  Scenario: Multiple formatters produce multiple flag keys from one env var
    Given the provider is configured with formatters kebabCase() and snakeCase()
    And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
    When the provider is constructed
    Then getString("checkout-flow") returns "PREMIUM"
    And getString("checkout_flow") returns "PREMIUM"

  # --- Multi-Parser + Multi-Formatter (Dangerous) ---

  Scenario: Multi-parser multi-formatter defaults to ERROR strategy
    Given the provider is configured with 2 parsers and 2 formatters
    And no explicit conflict strategy is set
    When the builder builds the provider
    Then the default conflict strategy is ERROR

  Scenario: Multi-parser multi-formatter with explicit WARN override
    Given the provider is configured with 2 parsers and 2 formatters
    And conflict strategy is explicitly set to WARN
    When the builder builds the provider
    Then the conflict strategy is WARN

  # --- ConflictStrategy: WARN ---

  Scenario: WARN strategy logs warning and keeps last mapping
    Given the provider is configured with conflict strategy WARN
    And two env vars map to the same flag key "checkout-flow" with different values
    When the provider is constructed
    Then a warning is logged with both env var names and flag key "checkout-flow"
    And the provider continues operating

  # --- ConflictStrategy: ERROR ---

  Scenario: ERROR strategy throws at construction on conflict
    Given the provider is configured with conflict strategy ERROR
    And two env vars map to the same flag key "checkout-flow" with different values
    When the provider is constructed
    Then an IllegalStateException is thrown
    And the exception message mentions both env var names and flag key "checkout-flow"

  # --- Conflict Warning on First Access ---

  Scenario: First access of conflicted key logs a warning
    Given the provider was constructed with WARN strategy
    And flag key "checkout-flow" had a conflict during construction
    When Kenji calls getString("checkout-flow") for the first time
    Then a warning is logged mentioning the conflict

  Scenario: Subsequent access does not repeat warning
    Given Kenji has already called getString("checkout-flow") once
    When Kenji calls getString("checkout-flow") again
    Then no additional warning is logged

  Scenario: Non-conflicted key produces no warning on access
    Given flag key "max-retries" had no conflict during construction
    When Kenji calls getString("max-retries")
    Then no warning is logged

  # --- Error Paths ---

  Scenario: Missing flag key returns empty
    Given no environment variable maps to flag key "nonexistent"
    When Kenji calls getString("nonexistent")
    Then the result is empty

  Scenario: Unparseable integer returns empty for typed access
    Given environment variable FLAGZEN_MAX_RETRIES is set to "not-a-number"
    When Kenji calls getInt("max-retries")
    Then the integer result is empty
    But getString("max-retries") returns "not-a-number"

  Scenario: Unparseable boolean returns empty for typed access
    Given environment variable FLAGZEN_DARK_MODE is set to "maybe"
    When Kenji calls getBoolean("dark-mode")
    Then the boolean result is empty
    But getString("dark-mode") returns "maybe"

  Scenario: Empty environment variable value is preserved
    Given environment variable FLAGZEN_CHECKOUT_FLOW is set to ""
    When Kenji calls getString("checkout-flow")
    Then the result is ""

  Scenario: Context-aware getString delegates to context-free getString
    Given environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
    When Kenji calls getString("checkout-flow", someEvaluationContext)
    Then the result is "PREMIUM"
    And the evaluation context is ignored because env vars are static

  # --- Builder API ---

  Scenario: Builder with custom prefix parser
    Given Mei-Lin configures the builder with parser screamingSnakeCase("FF_")
    And environment variable FF_CHECKOUT_FLOW is set to "PREMIUM"
    When the provider is built
    Then getString("checkout-flow") returns "PREMIUM"

  Scenario: Builder with custom formatter
    Given Kenji configures the builder with formatter snakeCase()
    And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
    When the provider is built
    Then getString("checkout_flow") returns "PREMIUM"

  Scenario: Builder with multiple parsers and single formatter
    Given Mei-Lin configures the builder with parsers screamingSnakeCase("FLAGZEN_") and camelCase("myApp")
    And formatter kebabCase()
    And conflict strategy WARN
    When the provider is built
    Then both FLAGZEN_ and myApp env vars contribute to the flag map
