# ADR-006: Annotation Retention and Targets

## Status

Accepted

## Context

FlagZen defines several annotations (`@Feature`, `@Variant`, `@DefaultVariant`, `@PinFlag`, `@FlagSource`). Each annotation needs:

1. **Retention policy**: SOURCE, CLASS, or RUNTIME -- determines when the annotation is available
2. **Target**: TYPE, METHOD, FIELD, etc. -- determines where the annotation can be placed

The choice directly impacts:

- Whether the annotation processor can read the annotation
- Whether runtime code (JUnit extension, Spring auto-config) can read the annotation
- Whether misuse produces a compile error or a runtime error

### Key Constraint

Core module targets zero runtime reflection. Compile-time annotations should use CLASS retention (available to processor, discarded by JVM). Test annotations must use RUNTIME retention (read by JUnit extension at test time).

## Decision

|    Annotation     | Retention |    Target    |                                           Rationale                                            |
| ----------------- | --------- | ------------ | ---------------------------------------------------------------------------------------------- |
| `@Feature`        | CLASS     | TYPE         | Processed at compile time. Not needed at runtime (proxy is already generated).                 |
| `@Variant`        | CLASS     | TYPE         | Processed at compile time. Runtime dispatch uses the generated proxy, not annotation metadata. |
| `@DefaultVariant` | CLASS     | TYPE         | Same as @Variant -- compile-time only.                                                         |
| `@PinFlag`        | RUNTIME   | METHOD       | Must be read by JUnit extension at test time via reflection.                                   |
| `@FlagSource`     | RUNTIME   | TYPE, METHOD | Must be read by JUnit extension at test time via reflection.                                   |

`@Variant` is `@Repeatable` (using a `@Variants` container annotation) to support multi-feature implementations.

`@PinFlag` is `@Repeatable` (using a `@PinFlags` container annotation) to support pinning multiple flags in one test.

## Alternatives Considered

### Alternative 1: RUNTIME Retention for All Annotations

Make all annotations RUNTIME so they can be read by runtime code (e.g., Spring classpath scanning, runtime validation).

- **Pro**: Maximum flexibility -- annotations available everywhere
- **Pro**: Spring component scanning could discover @Feature interfaces at runtime
- **Con**: Violates zero-runtime-reflection for core annotations
- **Con**: Annotation metadata in bytecode increases class file size (minor)
- **Con**: Encourages runtime annotation processing which the project explicitly avoids

**Rejected**: Core annotations (@Feature, @Variant, @DefaultVariant) are fully consumed by the annotation processor at compile time. Runtime retention would signal that runtime processing is expected, which contradicts the zero-reflection constraint.

### Alternative 2: SOURCE Retention for Core Annotations

Make @Feature/@Variant SOURCE-only -- discarded after compilation.

- **Pro**: Minimal bytecode impact
- **Con**: SOURCE retention is not available to annotation processors (javax.annotation.processing operates on CLASS and RUNTIME retained annotations)
- **Con**: Would make the annotation processor unable to read the annotations

**Rejected**: Annotation processors require at minimum CLASS retention. SOURCE retention makes annotations invisible to `javax.annotation.processing.Processor`.

### Alternative 3: @Feature Target Includes FIELD for Injection Points

Allow `@Feature` on fields to mark injection points (e.g., `@Feature("dark-mode") DarkMode darkMode`).

- **Pro**: Could enable injection without `@Autowired` or `FeatureDispatcher.resolve()`
- **Con**: Dual meaning of @Feature (definition vs. injection) is confusing
- **Con**: Requires runtime processing of @Feature on fields -- violates zero-reflection
- **Con**: DI injection is handled by framework-specific modules (Spring @Autowired, CDI @Inject)

**Rejected**: @Feature means "this interface IS a feature flag." Using it for injection points conflates definition with consumption. DI frameworks handle injection via their own mechanisms.

## Consequences

### Positive

- Clear separation: CLASS = compile-time (core), RUNTIME = test-time (test module)
- Zero runtime reflection for core annotations -- consistent with project constraint
- @Repeatable support enables multi-feature variants and multi-flag test pinning
- Target restrictions (TYPE only for @Feature/@Variant) produce compile errors on misuse

### Negative

- CLASS retention means Spring cannot discover @Feature interfaces via classpath scanning at runtime -- flagzen-spring must use a different mechanism (generated metadata or explicit registration)
- Test annotations require RUNTIME retention, meaning flagzen-test does use reflection (acceptable -- test module, not core)
