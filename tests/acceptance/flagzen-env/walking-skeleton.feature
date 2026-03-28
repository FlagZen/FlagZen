@walking-skeleton
Feature: Environment variable flag resolution with sensible defaults
  As a backend developer deploying 12-factor apps,
  I want to source feature flag values from environment variables with zero configuration,
  so that ops can toggle features per environment without code changes or external services.

  # Walking Skeleton: thinnest E2E slice through the entire flagzen-env system.
  # Sets one env var with FLAGZEN_ prefix -> creates provider with defaults ->
  # getString() returns the value. Proves: parser, formatter, eager loading, immutable map,
  # and FlagProvider contract all work together.

  @US-ENV-01 @US-ENV-02 @US-ENV-05 @US-ENV-06
  Scenario: Developer resolves a flag from an environment variable with zero configuration
    Given environment variable "FLAGZEN_CHECKOUT_FLOW" is set to "CLASSIC"
    When the developer creates a provider with default configuration
    And the developer looks up flag "checkout-flow"
    Then the flag value is "CLASSIC"

  @US-ENV-01 @US-ENV-02
  Scenario: Missing flag key returns no value
    Given no environment variable maps to flag "nonexistent-flag"
    When the developer creates a provider with default configuration
    And the developer looks up flag "nonexistent-flag"
    Then no flag value is returned
