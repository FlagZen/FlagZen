# ADR-002: Code Generation Tooling

## Status

Accepted

## Context

The annotation processor in flagzen-core must generate Java source files (`{Feature}_FlagZenProxy`) during compilation. The generated code must be well-formatted, correct Java that compiles without warnings. The code generation tool is a compile-time-only dependency of the annotation processor and must NOT be transitive to library consumers.

### Quality Attributes

- **Maintainability**: generator code must be readable and maintainable
- **Correctness**: generated code must handle generics, checked exceptions, all return types
- **Developer experience**: generated source must be readable and well-formatted

## Decision

Use **JavaPoet** (com.squareup:javapoet, Apache 2.0) for Java source code generation.

JavaPoet provides a type-safe Java API for building `.java` files. It handles:

- Package declarations, imports (automatic management)
- Class, interface, method, field generation
- Type resolution (avoids import conflicts)
- Annotation generation
- Formatting and indentation

JavaPoet is declared as a `compileOnly` / annotation processor classpath dependency. It is NOT transitive to consumers of flagzen-core.

## Alternatives Considered

### Alternative 1: Raw String Templates / StringBuilder

Build Java source code as strings using `StringBuilder` or Java text blocks.

- **Pro**: Zero dependencies -- no external library needed
- **Pro**: Maximum flexibility -- can generate any text
- **Con**: Error-prone -- no type safety for generated code structure
- **Con**: Import management is manual and bug-prone (name collisions)
- **Con**: Formatting and indentation must be manually handled
- **Con**: Generics and type variables require careful string escaping
- **Con**: Maintenance burden grows with proxy complexity

**Rejected**: Too error-prone for the complexity of generated proxies (generics, exceptions, multiple return types). The risk of generating invalid Java is high.

### Alternative 2: Apache Velocity / FreeMarker Templates

Use a template engine with .vm/.ftl templates defining the proxy structure.

- **Pro**: Separation of template from logic
- **Pro**: Templates are readable
- **Con**: Templates are stringly-typed -- no compile-time validation of generated Java
- **Con**: Adds runtime (or compile-time) dependency on template engine
- **Con**: Import management still manual
- **Con**: Template syntax mixed with Java syntax reduces readability
- **Con**: Harder to handle complex type resolution (generics, nested types)

**Rejected**: Same stringly-typed problems as raw strings, plus a template engine dependency. JavaPoet's type-safe API is strictly superior for Java source generation.

### Alternative 3: JStachio / Roaster

- **JStachio**: Compile-time Mustache templates. Focused on text rendering, not Java source generation. Wrong tool.
- **Roaster** (JBoss Forge): Java source manipulation library. Can parse and modify Java source. Less popular than JavaPoet, primarily for source manipulation not generation.

**Rejected**: JStachio is wrong category. Roaster is viable but has smaller community, fewer users, and less mature API for generation (vs. manipulation).

## Consequences

### Positive

- Type-safe code generation reduces risk of producing invalid Java
- Automatic import management handles name collisions
- Generated code is consistently well-formatted
- JavaPoet is mature (Square, widely used in Android/Java ecosystem), Apache 2.0
- Compile-time only -- zero impact on consumer classpath

### Negative

- Compile-time dependency on JavaPoet (com.squareup:javapoet)
- Learning curve for JavaPoet API (moderate -- well documented)
- JavaPoet generates Java source, not bytecode -- requires subsequent compilation of generated source (standard annotation processor behavior)
