@walking-skeleton
Feature: Flag resolution through an OpenFeature-backed provider
  As a Java developer using OpenFeature with an existing flag management service,
  I want FlagZen to resolve feature flags through the OpenFeature SDK,
  so I can use polymorphic dispatch without replacing my flag infrastructure.

  # Walking Skeleton 1: Thinnest E2E slice -- string flag resolved through OpenFeature.
  # Proves: OpenFeatureFlagProvider implements FlagProvider, delegates to OpenFeature Client,
  # reason-based absence detection works, and the adapter returns the resolved value.

  @US-OF-01
  Scenario: Developer resolves a string flag through the OpenFeature adapter
    Given the flag management service has flag "checkout-flow" set to "EXPRESS"
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves string flag "checkout-flow" through the adapter
    Then the adapter returns "EXPRESS"

  # Walking Skeleton 2: Typed resolution -- boolean flag resolved natively.
  # Proves: typed delegation works without string round-tripping.

  @US-OF-02
  Scenario: Developer resolves a boolean flag through the OpenFeature adapter
    Given the flag management service has boolean flag "dark-mode" set to true
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves boolean flag "dark-mode" through the adapter
    Then the adapter returns boolean true
