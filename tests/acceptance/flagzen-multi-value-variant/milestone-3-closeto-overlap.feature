Feature: Compile-time detection of overlapping @CloseTo ranges
  As a Java developer using FlagZen with double-typed features,
  I want the compiler to detect overlapping @CloseTo ranges at compile time,
  so that ambiguous dispatch is caught before the code ever runs.

  # --- Inter-variant overlap (across different classes) ---

  @US-MV-07
  Scenario: Overlapping @CloseTo ranges across variants is rejected
    Given a feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a variant "SmallDiscount" implementing "DiscountRate" with @CloseTo value 0.1 and delta 0.05
    And a variant "MediumDiscount" implementing "DiscountRate" with @CloseTo value 0.12 and delta 0.05
    When the project compiles
    Then compilation fails with error containing "Overlapping @CloseTo ranges"
    And the error names "SmallDiscount" and "MediumDiscount"
    And the error shows the computed ranges
    And the error suggests reducing delta or merging variants

  @US-MV-07
  Scenario: Non-overlapping @CloseTo ranges across variants compiles successfully
    Given a feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a variant "SmallDiscount" implementing "DiscountRate" with @CloseTo value 0.1 and delta 0.01
    And a variant "LargeDiscount" implementing "DiscountRate" with @CloseTo value 0.5 and delta 0.01
    When the project compiles
    Then compilation succeeds

  @US-MV-07
  Scenario: Overlapping @CloseTo ranges with default delta is detected
    Given a feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a variant "SmallDiscount" implementing "DiscountRate" with @CloseTo value 0.1
    And a variant "MediumDiscount" implementing "DiscountRate" with @CloseTo value 0.1000000001
    When the project compiles
    Then compilation fails with error containing "Overlapping @CloseTo ranges"

  # --- Intra-variant overlap (within same variant's array) ---

  @US-MV-07
  Scenario: Overlapping @CloseTo ranges within the same variant array is rejected
    Given a feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a variant "SmallDiscount" implementing "DiscountRate" with @CloseTo values 0.1 delta 0.05 and 0.12 delta 0.05
    When the project compiles
    Then compilation fails with error containing "Overlapping @CloseTo ranges within variant SmallDiscount"
    And the error shows both ranges
    And the error suggests reducing delta or removing the redundant entry

  @US-MV-07
  Scenario: Non-overlapping @CloseTo ranges within the same variant array compiles successfully
    Given a feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a variant "SmallDiscount" implementing "DiscountRate" with @CloseTo values 0.1 and 0.5
    When the project compiles
    Then compilation succeeds
