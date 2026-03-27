# Journey: Condition Predicate-Based Variant Selection

## Persona

**Kenji Tanaka** -- Senior Java developer at a SaaS company. Uses FlagZen for polymorphic dispatch. Wants to select variants based on runtime context properties (user plan, region, deployment stage) without depending on an external flag service. Comfortable with annotation-based APIs and the Strategy pattern.

## Journey Goal

Define predicate-based conditions on variants so the proxy evaluates them in order against the EvaluationContext and dispatches to the first matching variant -- a declarative in-code Strategy pattern selector.

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
FeaturePredicate       @Condition on           processor              predicates in
implementation         @Variant                validates &            order, first
                                               generates proxy        match wins

  Feels:                 Feels:                 Feels:                 Feels:
  Familiar --            Declarative,           Safe -- errors         Confident --
  just a                 reads like             caught at compile      deterministic
  functional             intent                 time                   behavior
  interface

  Artifacts:             Artifacts:             Artifacts:             Artifacts:
  IsEnterprise.java      @Variant(when=...)     Generated proxy        Resolved variant
  (implements            on variant class       with predicate         instance
  FeaturePredicate)                             dispatch logic
```

## Step Details

### Step 1: Define FeaturePredicate

```
+-- Define Predicate ------------------------------------------------+
|                                                                     |
|  public class IsEnterprise implements FeaturePredicate {            |
|      @Override                                                      |
|      public boolean test(EvaluationContext ctx) {                   |
|          return "enterprise".equals(ctx.attribute("plan"));         |
|      }                                                              |
|  }                                                                  |
|                                                                     |
|  -- No-arg constructor (required)                                   |
|  -- Pure function: EvaluationContext in, boolean out                |
|  -- No side effects, no state                                       |
+---------------------------------------------------------------------+
  Emotional state: Familiar. "This is just a predicate -- I know this pattern."
```

### Step 2: Annotate Variant with @Condition

```
+-- Annotate Variant ------------------------------------------------+
|                                                                     |
|  @Feature("pricing-tier")                                           |
|  interface PricingStrategy { Money calculate(Order order); }        |
|                                                                     |
|  @Variant(when = @Condition(on = IsEnterprise.class, order = 1))    |
|  class EnterprisePricing implements PricingStrategy { ... }         |
|                                                                     |
|  @Variant(when = @Condition(on = IsStartup.class, order = 2))       |
|  class StartupPricing implements PricingStrategy { ... }            |
|                                                                     |
|  @DefaultVariant                                                    |
|  class StandardPricing implements PricingStrategy { ... }           |
|                                                                     |
|  -- @Condition(on = ...) references the predicate class             |
|  -- order = int determines evaluation sequence                      |
|  -- @DefaultVariant catches unmatched cases                         |
+---------------------------------------------------------------------+
  Emotional state: Declarative clarity. "This reads like my intent."
```

### Step 3: Compile-Time Validation

```
+-- Compile --------------------------------------------------------+
|                                                                    |
|  Annotation processor validates:                                   |
|                                                                    |
|  [OK] IsEnterprise implements FeaturePredicate                     |
|  [OK] IsStartup implements FeaturePredicate                        |
|  [OK] No duplicate order values within same @Feature               |
|  [OK] Predicate class has accessible no-arg constructor            |
|  [OK] @Condition not combined with value-based @Variant("...")     |
|                                                                    |
|  Generates: PricingStrategy_FlagZenProxy                           |
|  with predicate-based dispatch logic                               |
|                                                                    |
|  ERROR EXAMPLE:                                                    |
|  @Variant(when = @Condition(on = NotAPredicate.class, order = 1))  |
|  --> error: NotAPredicate does not implement FeaturePredicate      |
+--------------------------------------------------------------------+
  Emotional state: Safe. "If it compiles, it's correct."
```

### Step 4: Runtime Dispatch

```
+-- Runtime Dispatch -----------------------------------------------+
|                                                                    |
|  EvaluationContext ctx = EvaluationContext.builder()                |
|      .targetingKey("user-7291")                                    |
|      .attribute("plan", "enterprise")                              |
|      .build();                                                     |
|                                                                    |
|  FlagContext.run(ctx, () -> {                                      |
|      PricingStrategy pricing =                                     |
|          dispatcher.resolve(PricingStrategy.class);                 |
|                                                                    |
|      // Proxy evaluates:                                           |
|      // 1. IsEnterprise.test(ctx) --> true --> EnterprisePricing    |
|      // (IsStartup never evaluated -- first match wins)            |
|  });                                                               |
|                                                                    |
|  FALLBACK CASE (no match, no default):                             |
|  FallbackStrategy.EXCEPTION --> UnmatchedVariantException          |
|  FallbackStrategy.NOOP --> safe defaults                           |
+--------------------------------------------------------------------+
  Emotional state: Confident. "Deterministic, predictable, traceable."
```

## Error Paths

### E1: Predicate class does not implement FeaturePredicate

**When**: Compile time
**What user sees**: `error: IsEnterprise does not implement FeaturePredicate`
**Recovery**: Implement `FeaturePredicate` on the class

### E2: Duplicate order values within same @Feature

**When**: Compile time
**What user sees**: `error: Duplicate @Condition order 1 on PricingStrategy`
**Recovery**: Assign unique order values

### E3: No predicate matches and no @DefaultVariant

**When**: Runtime
**What user sees**: `UnmatchedVariantException` (EXCEPTION strategy) or NOOP default
**Recovery**: Add `@DefaultVariant` or handle exception

### E4: Predicate throws exception during test()

**When**: Runtime
**What user sees**: Exception propagated from predicate evaluation
**Recovery**: Fix predicate logic; predicates should be pure functions

### E5: Mixing value-based and condition-based @Variant on same feature

**When**: Compile time
**What user sees**: `error: PricingStrategy mixes value-based and condition-based variants`
**Recovery**: Use one dispatch mode per @Feature -- either value-based or condition-based

### E6: No EvaluationContext available for condition-based feature

**When**: Runtime
**What user sees**: No predicate can match (all receive null/empty context). Falls through to @DefaultVariant or FallbackStrategy.
**Recovery**: Ensure EvaluationContext is provided via explicit param, FlagContext.run(), or ContextAccessor

## Integration Points

- **M1 dependency**: EvaluationContext must exist (US-EC-01). FeaturePredicate.test() takes EvaluationContext.
- **Proxy generation**: Extends existing ProxyGenerator to emit predicate dispatch alongside value-based dispatch.
- **FallbackStrategy**: Existing enum reused unchanged -- @DefaultVariant and FallbackStrategy apply identically.
- **Spring module**: When flagzen-spring present, predicates can be instantiated via DI instead of no-arg constructor.
- **Context resolution order**: Same chain (explicit > accessor > scoped > default) provides the EvaluationContext to predicate evaluation.
