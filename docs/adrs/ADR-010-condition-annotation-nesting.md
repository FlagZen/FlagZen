# ADR-010: @Condition Annotation Design

## Status

Accepted (updated: `matches`/`notMatches` attributes, `order` moved to `@Variant`)

## Context

M6 introduces `@Condition` to bind a predicate to a `@Variant`. The annotation must reference the predicate class. The design questions are: (1) where `@Condition` lives syntactically, (2) what its attributes are named, and (3) where dispatch order is specified.

## Decision

### Nesting and syntax

`@Condition` is a nested annotation used inside `@Variant(when = @Condition(matches = X.class))`. It is not a standalone annotation.

```java
@Variant(when = @Condition(matches = HighRetryRange.class), order = 2)
class AggressiveRetry implements RetryStrategy { ... }
```

### Attributes

- `matches` — the predicate class (required). Must implement the correct JDK predicate type for the feature's `FeatureType` (see ADR-009).
- `notMatches` — the predicate class for negation (optional). Variant is selected when the predicate does NOT match.
- `matches` and `notMatches` are **mutually exclusive** — the processor rejects annotations that set both. This keeps usage readable: either "when condition matches X" or "when condition not matches X."

### Order on @Variant, not @Condition

`order` is an attribute of `@Variant`, not `@Condition`. This enables unified ordering across exact-match and condition-based variants (see ADR-008):

```java
@Variant(value = "SPECIAL", order = 1)                                    // exact match
@Variant(when = @Condition(matches = HighValue.class), order = 2)          // condition
@Variant(value = "STANDARD", order = 3)                                    // exact match
```

## Alternatives Considered

### 1. `on` attribute instead of `matches`

`@Condition(on = IsEnterprise.class)` — the original design.

**Superseded because**: `matches` reads better as English: "when condition matches Enterprise." Also enables `notMatches` as a natural pair. With `on`, the negation would be `notOn` which is awkward.

### 2. `is` / `isNot` attributes

`@Condition(is = Enterprise.class)` — reads fluently for single predicates.

**Rejected because**: Scaling to arrays (`is = {A.class, B.class}`) reads poorly — "condition is A and B" is ambiguous (AND or OR?). `matches`/`notMatches` avoid this problem by being explicitly single-valued and mutually exclusive.

### 3. `order` inside @Condition

`@Condition(matches = X.class, order = 1)` — keeps order with the condition.

**Rejected because**: `order` applies to the `@Variant` as a whole, not just to the condition. Exact-match variants also need ordering when mixed with conditions. Placing `order` on `@Variant` enables the unified ordered dispatch model (ADR-008).

### 4. Standalone @Condition annotation on the variant class

`@Condition(matches = X.class)` applied directly alongside `@Variant` on the class.

**Rejected because**: Cross-annotation matching is error-prone with `@Repeatable` `@Variant` for multi-feature classes. Nesting makes the binding explicit.

## Consequences

### Positive

- Reads as English: `@Variant(when = @Condition(matches = X.class), order = 2)`
- `matches`/`notMatches` are clear and mutually exclusive — no ambiguity
- `order` on `@Variant` enables mixed exact-match + condition dispatch (ADR-008)
- Self-contained variant declarations: all dispatch metadata in one annotation
- Negation without writing wrapper predicates

### Negative

- Slightly verbose syntax for simple cases
- Sentinel default for `@Variant.when` requires a sentinel class
- `notMatches` is not "not matches" in English — but `doesNotMatch` is too long, and `notMatching` changes the tense
