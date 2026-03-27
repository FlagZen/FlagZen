# ADR-008: Unified Ordered Dispatch Model

## Status

Accepted (supersedes original "Mutually Exclusive Dispatch Modes")

## Context

FlagZen M0 supports value-based dispatch: a `FlagProvider` returns a value, and the proxy looks up the corresponding `@Variant` in a map. M6 introduces condition-based dispatch: user-defined predicates evaluate the flag value and select a variant (e.g., range matching on integers).

The original design (mutually exclusive modes) forced each `@Feature` to be either value-based OR condition-based, never both. This was a simplification that prevented valid use cases like: "use SpecialOfferPricing when the flag value is exactly 'SPECIAL_OFFER', use EnterprisePricing when the integer value is above a threshold, otherwise use StandardPricing."

## Decision

Value-based and condition-based variants can coexist on the same `@Feature`. Dispatch order is controlled by an optional `order` attribute on `@Variant`:

```java
@Feature("pricing")
interface Pricing { Money calculate(Order order); }

@Variant(value = "SPECIAL_OFFER", order = 1)          // exact match, checked first
@Variant(when = @Condition(matches = HighValue.class), order = 2)  // condition, checked second
@Variant(value = "STANDARD", order = 3)                // exact match, checked third

@DefaultVariant                                        // no order needed, always last
class FallbackPricing implements Pricing { ... }
```

Dispatch evaluates variants in `order` sequence. Exact match checks the flag value. Condition evaluates the predicate against the flag value. First match wins. `@DefaultVariant` is always last.

### When `order` is required

- **Only exact matches, no conditions**: `order` optional — existing map-lookup behavior, order is irrelevant (values are unique by validation)
- **Only one condition + optional default**: `order` optional — only one thing to evaluate
- **Mixed exact matches and conditions, or multiple conditions**: `order` mandatory on every `@Variant` — processor enforces at compile time

### Proxy generation strategy

- When no `order` is present: O(1) map lookup (existing behavior, no regression)
- When `order` is present: ordered list of `(matcher, supplier)` pairs evaluated sequentially (O(n), n typically 2-5 variants)

## Alternatives Considered

### 1. Mutually exclusive dispatch modes (original ADR-008)

A `@Feature` uses exactly one dispatch mode: value-based OR condition-based, never both. The processor rejects mixing.

**Superseded because**: Prevents valid use cases (exact match + range fallback). Forces developers to decompose into separate `@Feature` interfaces for what is conceptually one feature. The unified model is strictly more expressive with no additional complexity for the simple case (no `order` = same behavior as before).

### 2. Implicit ordering by annotation source order

Use the order annotations appear in the source file rather than an explicit `order` attribute.

**Rejected because**: Java annotation ordering in source code is not guaranteed to be preserved by all compilers. Explicit `order` is deterministic and portable. Also allows reordering without moving code.

### 3. Priority-based dispatch with separate attribute

A `@DispatchPriority` annotation or a `priority` attribute separate from `order`.

**Rejected because**: Adding another annotation or attribute creates redundancy. `order` on `@Variant` is sufficient and keeps all dispatch metadata in one place.

## Consequences

### Positive

- Unified mental model: one feature, ordered dispatch, first match wins
- Simple case unchanged: no `order` = map lookup (zero regression)
- Mixed dispatch enables "exact match with condition fallback" patterns
- Compile-time enforcement: processor requires `order` when ambiguity exists
- `@DefaultVariant` always last — no need to assign it an order

### Negative

- Ordered dispatch is O(n) per method call (acceptable for typical variant counts)
- `order` attribute adds to `@Variant`'s surface area
- Developers must think about evaluation order when mixing modes
