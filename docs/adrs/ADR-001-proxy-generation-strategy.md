# ADR-001: Proxy Generation Strategy

## Status

Accepted

## Context

FlagZen needs a mechanism to dispatch method calls on a `@Feature` interface to the active `@Variant` implementation based on the current flag value. The dispatch mechanism must support runtime flag changes (proxy follows new value on next call) and must involve zero runtime reflection in the core module.

The project brief explicitly requires: "Zero runtime reflection for the core module" and "Proxy generation at compile time."

### Quality Attributes

- **Performance**: dispatch must be nanosecond-level (map lookup + delegation)
- **Testability**: dispatch code must be debuggable and inspectable in IDE
- **Maintainability**: generated code must be readable and well-formatted
- **Zero reflection**: hard constraint from project brief

## Decision

Generate one concrete proxy class per `@Feature` interface at compile time via annotation processing. The proxy is a regular Java class (`{Feature}_FlagZenProxy`) that implements the feature interface and uses a map lookup to delegate to the active variant.

One proxy per @Feature (not a single registry class) because:

- Each proxy is self-contained and independently testable
- The proxy can live in the same package as the @Feature interface (package-private access)
- Generated code is small and focused per feature
- Avoids a god-class registry that grows with every feature

## Alternatives Considered

### Alternative 1: java.lang.reflect.Proxy (Dynamic Proxy)

Java's built-in `Proxy.newProxyInstance()` creates interface proxies at runtime.

- **Pro**: No compile-time code generation needed, simpler annotation processor
- **Pro**: Standard JDK mechanism, well understood
- **Con**: Violates zero-reflection constraint (uses `java.lang.reflect.InvocationHandler`)
- **Con**: Runtime cost per invocation (reflection overhead)
- **Con**: Not debuggable -- proxy source not visible in IDE
- **Con**: Boxing overhead for primitives

**Rejected**: Violates the hard zero-reflection constraint.

### Alternative 2: ByteBuddy Runtime Code Generation

ByteBuddy generates bytecode at runtime to create proxy classes.

- **Pro**: Flexible, powerful, widely used (Mockito, Hibernate)
- **Pro**: No source code generation step
- **Con**: Runtime dependency (~3MB JAR) on flagzen-core -- violates zero-dependency goal
- **Con**: Bytecode generation at class-load time, not compile time
- **Con**: Generated code not visible or debuggable as source
- **Con**: Technically still "runtime generation" even if not reflection

**Rejected**: Adds runtime dependency, bytecode not debuggable, not truly compile-time.

### Alternative 3: Single Registry Class

Generate one `FlagZenRegistry` class containing all dispatch logic for all features.

- **Pro**: Single generated file to manage
- **Con**: God class that grows with every @Feature in the project
- **Con**: Cannot be in the same package as each @Feature interface (package-private access lost)
- **Con**: Changes to any feature force re-generation of the entire registry
- **Con**: Harder to test individual feature dispatch in isolation

**Rejected**: Violates SRP, loses package-private access, poor incremental compilation behavior.

## Consequences

### Positive

- Zero runtime reflection -- all dispatch is compile-time generated Java source
- Generated proxies are visible in IDE, debuggable, and readable
- Each proxy is self-contained -- incremental compilation only regenerates changed features
- Proxy in same package as @Feature interface -- package-private access to variants
- No external runtime dependency for dispatch

### Negative

- Annotation processor complexity: must generate complete, valid Java source files
- Compile-time dependency on code generation library (JavaPoet)
- Generated source files add to project size (minor -- one file per feature)
- Annotation processing adds build time (expected <5 seconds per 100 features)
