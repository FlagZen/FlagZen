# ADR-014: @WhenTrue/@WhenFalse as Annotation Sugar for Boolean Variants

## Status

Accepted

## Context

FlagZen M2 introduces `FeatureType.BOOLEAN` for polymorphic dispatch on boolean-valued flags. Boolean features typically have exactly two variants (true/false). The canonical form `@Variant(booleanValue = true)` is verbose for such a common pattern. Additionally, boolean annotation attributes in Java require a tri-state sentinel strategy because `boolean` has no null -- making `@Variant(booleanValue = true)` ambiguous (is `true` the intended value or the default?).

Developers expect concise, readable annotations for the most common boolean patterns.

## Decision

Introduce `@WhenTrue` and `@WhenFalse` as standalone annotations. The annotation processor normalizes them to `@Variant(booleanValue = true)` and `@Variant(booleanValue = false)` respectively before any validation or code generation. They are pure syntactic sugar -- no separate processing path.

Both annotations have:

- `of()` attribute with `void.class` sentinel (same as `@Variant.of`)
- Retention: CLASS (same as `@Variant`)
- Target: TYPE (same as `@Variant`)
- `@Repeatable` with container annotations (`@WhenTrues`, `@WhenFalses`) for multi-feature classes

### Normalization Semantics

- `@WhenTrue` is normalized to a VariantModel with booleanValue `true`
- `@WhenFalse` is normalized to a VariantModel with booleanValue `false`
- `@WhenTrue(of = X.class)` is normalized to VariantModel targeting feature X with booleanValue `true`
- After normalization, all validation rules apply identically (type mismatch, completeness, duplicates)
- Mixed usage: `@WhenTrue` and `@Variant(booleanValue = false)` on the same feature is valid

### Validation Interaction

- `@WhenTrue` on a non-BOOLEAN feature: compile error (type mismatch, same as `@Variant(booleanValue = true)` on non-BOOLEAN)
- `@WhenTrue` and `@Variant(booleanValue = true)` on the same feature: compile error (duplicate boolean value `true`)
- `@WhenTrue` without `of=` on a class implementing multiple `@Feature` interfaces: compile error (ambiguous target, same as `@Variant` without `of=`)

## Alternatives Considered

### 1. Boolean sentinel strategy within @Variant only (no separate annotations)

Use a `BooleanValue` enum (`TRUE`, `FALSE`, `UNSET`) as the `@Variant` attribute type instead of primitive `boolean`.

- **Pro**: No new annotation types -- smaller API surface
- **Pro**: All variant metadata in one annotation
- **Con**: `@Variant(booleanValue = BooleanValue.TRUE)` is even more verbose than `@Variant(booleanValue = true)`
- **Con**: Introduces a single-purpose enum to the public API
- **Con**: Does not address the readability goal for boolean features

**Rejected**: Fails the primary goal of conciseness. The enum solves the sentinel problem but worsens verbosity. The sentinel problem is better solved by `@WhenTrue`/`@WhenFalse` which eliminate the `booleanValue` attribute entirely for the common case.

### 2. Meta-annotation approach (@WhenTrue as @Variant(booleanValue = true))

Define `@WhenTrue` as a meta-annotation that carries `@Variant(booleanValue = true)` via annotation composition (like Spring's composed annotations).

- **Pro**: Zero processing code -- Java annotation processing inherits meta-annotations
- **Con**: Java annotation processing does NOT support meta-annotation inheritance in `javax.annotation.processing` the way Spring does. Spring uses runtime reflection-based annotation lookup. FlagZen uses compile-time processing.
- **Con**: Would require FlagZen to implement its own meta-annotation discovery, adding complexity

**Rejected**: Java's standard annotation processing API does not support meta-annotation inheritance. The processor must explicitly discover `@WhenTrue`/`@WhenFalse` regardless.

### 3. Boolean-only feature annotation (@BooleanFeature)

A separate `@BooleanFeature` annotation that implies `FeatureType.BOOLEAN` and uses `@WhenTrue`/`@WhenFalse` exclusively.

- **Pro**: Clean separation of boolean features from other types
- **Con**: Duplicates `@Feature` metadata (`value`, `fallback`)
- **Con**: Fragments the annotation model -- two annotation families for the same concept
- **Con**: Prevents migration between types (changing from BOOLEAN to INT requires annotation replacement)

**Rejected**: Violates the principle of a unified annotation model. `@Feature(type = BOOLEAN)` is consistent with `@Feature(type = INT)`.

## Consequences

### Positive

- Boolean variants are concise: `@WhenTrue` is 9 characters vs `@Variant(booleanValue = true)` at 28+
- Readable: annotation name communicates intent directly
- Normalization means zero divergent processing paths -- all validation and code generation works on the unified VariantModel
- `of=` attribute follows established `@Variant.of` pattern for multi-feature classes

### Negative

- Two new annotation types in the public API (`@WhenTrue`, `@WhenFalse`) plus two container annotations (`@WhenTrues`, `@WhenFalses`)
- Processor must discover three annotation types (`@Variant`, `@WhenTrue`, `@WhenFalse`) in each processing round
- Developers have two ways to express the same thing (cognitive load for discoverability)
