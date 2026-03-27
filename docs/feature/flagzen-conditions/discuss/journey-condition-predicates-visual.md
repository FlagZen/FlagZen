# Journey: Condition Predicate-Based Variant Selection

## Persona

**Kenji Tanaka** -- Senior Java developer at a SaaS company. Uses FlagZen for polymorphic dispatch. Wants to select variants based on flag values (pricing tier, retry count, threshold) without depending on an external flag service. Comfortable with annotation-based APIs and the Strategy pattern.

## Journey Goal

Define predicate-based conditions on variants so the proxy evaluates them in order against the flag value and dispatches to the first matching variant -- a declarative in-code Strategy pattern selector. Exact matches and conditions can coexist on the same @Feature via unified ordered dispatch.

## Emotional Arc

```
Start: Curious/Purposeful    Middle: Focused/Methodical    End: Confident/Satisfied
"I know what I want --       "Each piece snaps into        "It works exactly as I
let me wire conditions        place cleanly"                expected, no surprises"
to variants"
```

## Journey Flow

```
[Define Predicate]     [Annotate Variant]     [Compile]              [Dispatch]
     |                      |                     |                      |
     v                      v                     v                      v
Kenji writes a         Kenji puts              Annotation             Proxy evaluates
JDK predicate          @Condition on           processor              predicates in
implementation         @Variant with           validates &            order, first
(tests flag value)     order on @Variant       generates proxy        match wins

  Feels:                 Feels:                 Feels:                 Feels:
  Familiar --            Declarative,           Safe -- errors         Confident --
  just a                 reads like             caught at compile      deterministic
  JDK functional         intent                 time                   behavior
  interface

  Artifacts:             Artifacts:             Artifacts:             Artifacts:
  Enterprise.java        @Variant(when=...,     Generated proxy        Resolved variant
  (implements            order=N)               with predicate         instance
  Predicate<String>)     on variant class       dispatch logic
```

## Step Details

### Step 1: Define Predicate

```
+-- Define Predicate ------------------------------------------------+
|                                                                     |
|  public class Enterprise implements Predicate<String> {             |
|      @Override                                                      |
|      public boolean test(String value) {                            |
|          return "enterprise".equals(value);                         |
|      }                                                              |
|  }                                                                  |
|                                                                     |
|  public class HighRetryRange implements IntPredicate {              |
|      @Override                                                      |
|      public boolean test(int value) {                               |
|          return value >= 7;                                          |
|      }                                                              |
|  }                                                                  |
|                                                                     |
|  -- No-arg constructor (required)                                   |
|  -- Pure function: flag value in, boolean out                       |
|  -- No side effects, no state                                       |
+---------------------------------------------------------------------+
  Emotional state: Familiar. "This is just a JDK predicate -- I know this pattern."
```

### Step 2: Annotate Variant with @Condition

```
+-- Annotate Variant ------------------------------------------------+
|                                                                     |
|  @Feature("pricing-tier")                                           |
|  interface PricingStrategy { Money calculate(Order order); }        |
|                                                                     |
|  @Variant(when = @Condition(matches = Enterprise.class), order = 1) |
|  class EnterprisePricing implements PricingStrategy { ... }         |
|                                                                     |
|  @Variant(when = @Condition(matches = Startup.class), order = 2)    |
|  class StartupPricing implements PricingStrategy { ... }            |
|                                                                     |
|  @DefaultVariant                                                    |
|  class StandardPricing implements PricingStrategy { ... }           |
|                                                                     |
|  -- @Condition(matches = ...) references the predicate class        |
|  -- @Condition(notMatches = ...) for negation (mutually exclusive)   |
|  -- order on @Variant determines evaluation sequence                |
|  -- @DefaultVariant catches unmatched cases                         |
|  -- Exact matches and conditions can coexist on the same @Feature   |
+---------------------------------------------------------------------+
  Emotional state: Declarative clarity. "This reads like my intent."
```

### Step 3: Compile-Time Validation

```
+-- Compile --------------------------------------------------------+
|                                                                    |
|  Annotation processor validates:                                   |
|                                                                    |
|  [OK] Enterprise implements Predicate<String>                      |
|  [OK] Startup implements Predicate<String>                         |
|  [OK] No duplicate order values within same @Feature               |
|  [OK] Predicate class has accessible no-arg constructor            |
|  [OK] matches and notMatches are not both specified                |
|                                                                    |
|  Generates: PricingStrategy_FlagZenProxy                           |
|  with unified dispatch logic (exact + predicate)                   |
|                                                                    |
|  ERROR EXAMPLE:                                                    |
|  @Variant(when = @Condition(matches = NotAPredicate.class))        |
|  --> error: NotAPredicate does not implement a supported            |
|             predicate interface                                    |
+--------------------------------------------------------------------+
  Emotional state: Safe. "If it compiles, it's correct."
```

### Step 4: Runtime Dispatch

```
+-- Runtime Dispatch -----------------------------------------------+
|                                                                    |
|  PricingStrategy pricing =                                         |
|      dispatcher.resolve(PricingStrategy.class);                    |
|                                                                    |
|  // Proxy evaluates:                                               |
|  // 1. Exact matches checked first                                 |
|  // 2. Enterprise.test("enterprise") --> true --> EnterprisePricing |
|  // (Startup never evaluated -- first match wins)                  |
|                                                                    |
|  FALLBACK CASE (no match, no default):                             |
|  FallbackStrategy.EXCEPTION --> UnmatchedVariantException          |
|  FallbackStrategy.NOOP --> safe defaults                           |
+--------------------------------------------------------------------+
  Emotional state: Confident. "Deterministic, predictable, traceable."
```

## Error Paths

### E1: Predicate class does not implement a JDK predicate interface

**When**: Compile time
**What user sees**: `error: Enterprise does not implement a supported predicate interface`
**Recovery**: Implement `Predicate<String>`, `IntPredicate`, `LongPredicate`, or `DoublePredicate` on the class

### E2: Duplicate order values within same @Feature

**When**: Compile time
**What user sees**: `error: Duplicate @Variant order 1 on PricingStrategy`
**Recovery**: Assign unique order values

### E3: No predicate matches and no @DefaultVariant

**When**: Runtime
**What user sees**: `UnmatchedVariantException` (EXCEPTION strategy) or NOOP default
**Recovery**: Add `@DefaultVariant` or handle exception

### E4: Predicate throws exception during test()

**When**: Runtime
**What user sees**: Exception propagated from predicate evaluation
**Recovery**: Fix predicate logic; predicates should be pure functions

### E5: matches and notMatches both specified on @Condition

**When**: Compile time
**What user sees**: `error: matches and notMatches are mutually exclusive on @Condition`
**Recovery**: Use only one of `matches` or `notMatches` per @Condition

### E6: No flag value available for condition-based feature

**When**: Runtime
**What user sees**: No predicate can match (all receive null/empty value). Falls through to @DefaultVariant or FallbackStrategy.
**Recovery**: Ensure flag value is available via FlagProvider

## Integration Points

- **Proxy generation**: ProxyGenerator emits unified dispatch logic supporting both exact matches and conditions on the same @Feature.
- **FallbackStrategy**: Existing enum reused unchanged -- @DefaultVariant and FallbackStrategy apply identically.
- **Spring module**: When flagzen-spring present, predicates can be instantiated via DI instead of no-arg constructor.
- **Unified dispatch**: Exact matches and conditions coexist. `order` on @Variant controls evaluation sequence. Exact matches are checked first, then conditions by order.
