@walking-skeleton
Feature: Typed polymorphic dispatch for feature flags
  As a Java developer using FlagZen for typed feature flags,
  I want to declare a feature's value type and have the proxy dispatch on typed values,
  so that variant selection uses actual types instead of string encoding.

  # Walking Skeleton 1: Declare typed feature, annotate typed variants, compile successfully
  # Covers: US-M2-01, US-M2-02 (thinnest slice through typed annotation model)
  @US-M2-01 @US-M2-02
  Scenario: Developer declares an integer-typed feature with typed variants
    Given a feature interface "RetryStrategy" with flag key "max-retries" and type INT
    And a method "execute" declared on "RetryStrategy"
    And a variant "ConservativeRetry" implementing "RetryStrategy" for integer value 3
    And a variant "AggressiveRetry" implementing "RetryStrategy" for integer value 10
    When the project compiles
    Then compilation succeeds
    And a dispatch proxy "RetryStrategy_FlagZenProxy" is generated

  # Walking Skeleton 2: Runtime typed dispatch through FeatureDispatcher
  # Covers: US-M2-05 (thinnest slice through typed runtime dispatch)
  @US-M2-05
  Scenario: Developer resolves an integer-typed feature to the matching variant at runtime
    Given a compiled feature "RetryStrategy" with integer variants 3 and 10
    And a flag provider with "max-retries" returning integer value 3
    And the dispatcher is configured with this provider
    When the developer resolves "RetryStrategy" through the dispatcher
    And calls "execute" on the resolved proxy
    Then the call is handled by the "ConservativeRetry" variant

  # Walking Skeleton 3: Boolean dispatch with convenience annotations
  # Covers: US-M2-03, US-M2-06 (thinnest slice through boolean typed dispatch)
  @US-M2-03 @US-M2-06
  Scenario: Developer dispatches a boolean feature using convenience annotations
    Given a feature interface "DarkMode" with flag key "dark-mode" and type BOOLEAN
    And a variant "DarkModeOn" implementing "DarkMode" annotated as active when true
    And a variant "DarkModeOff" implementing "DarkMode" annotated as active when false
    And a flag provider with "dark-mode" returning boolean true
    When the developer resolves "DarkMode" through the dispatcher
    Then the call is handled by the "DarkModeOn" variant
