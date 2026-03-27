# ADR-010: @Condition Annotation Nesting in @Variant

## Status

Accepted

## Context

M6 introduces `@Condition` to bind a `FeaturePredicate` to a `@Variant`. The annotation must reference the predicate class and specify evaluation order. The design question is where `@Condition` lives syntactically relative to `@Variant`.

## Decision

`@Condition` is a nested annotation used exclusively inside `@Variant(when = @Condition(on = IsEnterprise.class, order = 1))`. It is not a standalone annotation applied directly to classes.

`@Variant` gains a `when` attribute of type `@Condition` with a sentinel default value representing "no condition."

## Alternatives Considered

### 1. Standalone @Condition annotation on the variant class

`@Condition(on = IsEnterprise.class, order = 1)` applied directly alongside `@Variant` on the class.

```java
@Variant(of = PricingStrategy.class)
@Condition(on = IsEnterprise.class, order = 1)
public class EnterprisePricing implements PricingStrategy { ... }
```

**Rejected because**: Cross-annotation matching is error-prone. The processor must correlate `@Condition` with `@Variant` -- but which `@Variant` does the `@Condition` belong to when a class has `@Repeatable` `@Variant` for multiple features? The nesting approach makes the binding explicit: each `@Variant(when = ...)` owns its condition.

### 2. @Condition as a separate annotation with explicit feature binding

`@Condition(feature = PricingStrategy.class, on = IsEnterprise.class, order = 1)` -- standalone with its own feature reference.

**Rejected because**: Duplicates the `@Variant(of = ...)` feature binding. The developer declares the feature twice (once in `@Variant`, once in `@Condition`), creating a consistency risk. If they disagree, the processor must detect and report the conflict. Nesting eliminates this entire class of errors.

### 3. Predicate class reference directly in @Variant

No separate `@Condition` annotation. Instead: `@Variant(predicate = IsEnterprise.class, order = 1)`.

**Rejected because**: Pollutes `@Variant` with condition-specific attributes that are meaningless for value-based variants. The `when = @Condition(...)` pattern cleanly separates value-based attributes (`value`) from condition-based attributes (`on`, `order`). It also reads well as English: "variant when condition on IsEnterprise."

## Consequences

### Positive

- Self-contained variant declarations: all dispatch metadata in one annotation
- No cross-annotation correlation needed in the processor
- Reads naturally: `@Variant(when = @Condition(on = IsEnterprise.class, order = 1))`
- Clean separation: `@Variant.value` for value-based, `@Variant.when` for condition-based
- Multi-feature classes with `@Repeatable` `@Variant` each carry their own condition unambiguously

### Negative

- Slightly verbose syntax for simple cases (one `@Condition` nested inside `@Variant`)
- Sentinel default for `@Variant.when` requires a sentinel class (minor implementation detail)
- Annotation nesting is a less common Java pattern -- some developers may find it unfamiliar
