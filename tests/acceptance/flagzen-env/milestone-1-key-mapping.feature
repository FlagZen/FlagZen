Feature: Key mapping from source names to flag keys
  As a developer using environment variable flags,
  I want parsers and formatters to correctly translate between naming conventions,
  so that I can use standard flag key formats regardless of how env vars are named.

  # --- US-ENV-05: Built-in parsers ---

  @US-ENV-05
  Scenario: Screaming snake case parser with prefix extracts segments
    Given a screaming snake case parser with prefix "FLAGZEN_"
    When it parses the name "FLAGZEN_CHECKOUT_FLOW"
    Then the segments are "checkout" and "flow"

  @US-ENV-05
  Scenario: Screaming snake case parser rejects names without matching prefix
    Given a screaming snake case parser with prefix "FLAGZEN_"
    When it parses the name "HOME"
    Then no segments are returned

  @US-ENV-05
  Scenario: Screaming snake case parser without prefix parses any screaming snake name
    Given a screaming snake case parser without prefix
    When it parses the name "CHECKOUT_FLOW"
    Then the segments are "checkout" and "flow"

  @US-ENV-05
  Scenario: Screaming snake case parser handles single-segment name
    Given a screaming snake case parser with prefix "FLAGZEN_"
    When it parses the name "FLAGZEN_DARKMODE"
    Then the only segment is "darkmode"

  @US-ENV-05
  Scenario: Camel case parser with prefix extracts segments
    Given a camel case parser with prefix "myApp"
    When it parses the name "myAppCheckoutFlow"
    Then the segments are "checkout" and "flow"

  @US-ENV-05
  Scenario: Camel case parser rejects names without matching prefix
    Given a camel case parser with prefix "myApp"
    When it parses the name "FLAGZEN_CHECKOUT_FLOW"
    Then no segments are returned

  @pending @US-ENV-05
  Scenario: Camel case parser without prefix parses bare camel case name
    Given a camel case parser without prefix
    When it parses the name "checkoutFlow"
    Then the segments are "checkout" and "flow"

  # --- US-ENV-06: Built-in formatters ---

  @pending @US-ENV-06
  Scenario Outline: Built-in formatter produces correct flag key
    Given a <formatter> formatter
    When it formats the segments "checkout" and "flow"
    Then the flag key is "<expected_key>"

    Examples:
      | formatter  | expected_key   |
      | kebab case | checkout-flow  |
      | snake case | checkout_flow  |
      | camel case | checkoutFlow   |
      | pascal case| CheckoutFlow   |
      | dot case   | checkout.flow  |
      | colon case | checkout:flow  |

  @pending @US-ENV-06
  Scenario: Formatter handles single segment without delimiter
    Given a kebab case formatter
    When it formats the single segment "darkmode"
    Then the flag key is "darkmode"

  @pending @US-ENV-06
  Scenario: Custom lambda formatter applies custom delimiter
    Given a custom formatter that joins segments with "/"
    When it formats the segments "checkout" and "flow"
    Then the flag key is "checkout/flow"
