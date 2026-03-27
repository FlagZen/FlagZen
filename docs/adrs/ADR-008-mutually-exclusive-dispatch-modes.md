# ADR-008: Mutually Exclusive Dispatch Modes

## Status

Accepted

## Context

FlagZen M0 supports value-based dispatch: a `FlagProvider` returns a string, and the proxy looks up the corresponding `@Variant` in a map. M6 introduces condition-based dispatch: user-defined predicates evaluate an `EvaluationContext` and select a variant.

A `@Feature` interface could theoretically support both modes simultaneously -- some variants selected by flag value, others by predicate. However, this creates ambiguous dispatch semantics:

- Which mode takes priority?
- What happens when both a flag value and a predicate match?
- How does `FallbackStrategy` behave when one mode matches but the other does not?
- How does the developer reason about which variant will be selected?

The annotation processor must decide whether to allow, reject, or mediate mixed dispatch modes per `@Feature`.

## Decision

A `@Feature` interface uses exactly one dispatch mode: value-based OR condition-based, never both. The annotation processor rejects any `@Feature` that mixes `@Variant("value")` with `@Variant(when = @Condition(...))`.

`@DefaultVariant` is compatible with both modes and is not counted as either.

Different `@Feature` interfaces may use different modes. Mode is a per-feature decision, not a global one.

## Alternatives Considered

### 1. Allow mixed modes with flag-value priority

Value-based dispatch runs first. If no flag value matches, condition-based dispatch runs. This creates a fallback chain: flag provider > predicates > @DefaultVariant > FallbackStrategy.

**Rejected because**: The dispatch order is implicit and surprising. Developers must reason about two dispatch paths to predict behavior. Debugging "why did this variant get selected?" becomes significantly harder. The mental model is no longer "one feature, one dispatch mechanism."

### 2. Allow mixed modes with explicit priority annotation

Add a `@DispatchPriority` annotation or attribute to control which mode runs first. Developers declare the precedence explicitly.

**Rejected because**: Over-engineering for a scenario with no demonstrated need. Adds annotation surface area, processor complexity, and cognitive load. If a developer needs both flag-based and predicate-based selection, they can decompose into two `@Feature` interfaces with clear responsibilities.

### 3. Implicit mode detection from variant annotations (no mixing rejection)

If all variants have `@Condition`, use condition mode. If all have values, use value mode. If mixed, emit a warning but compile anyway.

**Rejected because**: Warnings are ignored. Allowing mixed modes without clear semantics guarantees confusion. Compile errors are the only reliable way to enforce architectural intent.

## Consequences

### Positive

- Clear mental model: one feature, one dispatch mechanism
- Simpler processor logic: partition variants, reject if both non-empty
- Simpler generated proxy: each proxy uses exactly one dispatch path
- Debuggable: variant selection is explainable from one mechanism
- Two features can still collaborate (one value-based, one condition-based) if the domain requires both

### Negative

- Cannot express "check flag value first, fall back to predicate" in a single @Feature
- Developers must decompose complex selection into separate @Feature interfaces
- If a feature needs to migrate from value-based to condition-based, all variants must change simultaneously (but this is a compile-time check, so the migration is safe)
