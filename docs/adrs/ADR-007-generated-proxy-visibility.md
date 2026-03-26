# ADR-007: Generated Proxy Visibility

## Status

Accepted

## Context

The annotation processor generates `{Feature}_FlagZenProxy` classes for each `@Feature` interface. The visibility (public vs. package-private class, public vs. package-private constructor) affects:

1. Whether DI frameworks (Spring, CDI) can instantiate the proxy
2. Whether users can directly construct proxies (bypassing FeatureDispatcher)
3. Whether the proxy can be in the same package as the @Feature interface

### Design Decision (Pre-confirmed)

Generated proxies: public class with package-private constructor.

## Decision

Generated proxy classes are:

- **Public class**: visible to DI frameworks, testing tools, and the FeatureDispatcher (which may be in a different package)
- **Package-private constructor**: prevents direct instantiation by users. Construction goes through `FeatureDispatcher.resolve()` or DI injection.
- **Same package** as the `@Feature` interface: enables package-private access to variants if needed

The proxy class is public so that:

- `FeatureDispatcher` (in `com.flagzen`) can reference it
- Spring `FactoryBean` (in `com.flagzen.spring`) can instantiate it
- Reflection-based DI (Spring, CDI) can access it

The constructor is package-private so that:

- Users cannot `new CheckoutFlow_FlagZenProxy(...)` directly
- Construction is controlled by the framework (dispatcher or DI)

## Alternatives Considered

### Alternative 1: Package-Private Class, Package-Private Constructor

Both class and constructor are package-private.

- **Pro**: Maximum encapsulation -- proxy is invisible outside its package
- **Pro**: Users cannot reference the proxy type at all
- **Con**: `FeatureDispatcher` (in `com.flagzen.internal`) cannot access the proxy class (different package)
- **Con**: Spring `FactoryBean` cannot instantiate it without reflection
- **Con**: Testing the generated proxy in isolation becomes harder

**Rejected**: The dispatcher and DI frameworks are in different packages. Package-private class prevents them from referencing the proxy type. Would require the dispatcher to use reflection, violating the zero-reflection constraint.

### Alternative 2: Public Class, Public Constructor

Both class and constructor are public.

- **Pro**: Maximum flexibility -- anything can instantiate the proxy
- **Pro**: Simplest generated code
- **Con**: Users can bypass `FeatureDispatcher.resolve()` and construct proxies directly
- **Con**: Direct construction requires passing internal dependencies (FlagProvider, variant map)
- **Con**: API surface expands with implementation details in the constructor signature

**Rejected**: Exposing the constructor leaks implementation details (FlagProvider, variant map) into the public API. Users should go through `FeatureDispatcher.resolve()` or DI injection.

### Alternative 3: Public Class, Private Constructor, Static Factory

Public class with a private constructor and a `static` factory method.

- **Pro**: Controlled construction without exposing constructor parameters
- **Con**: Static factory method is another API surface to maintain
- **Con**: DI frameworks cannot call private constructors without reflection
- **Con**: Static methods complicate testing

**Rejected**: DI frameworks need constructor access. Private constructor + static factory forces reflection-based instantiation in DI contexts.

## Consequences

### Positive

- DI frameworks can instantiate via package-private constructor (using generated code within the same package or reflection in Spring/CDI -- acceptable for DI modules, not core)
- Users cannot directly construct proxies -- forced through proper channels
- Generated proxy is public for type referencing (e.g., in test assertions)
- FeatureDispatcher can reference the proxy type from its package

### Negative

- Package-private constructor means the FeatureDispatcher needs a mechanism to instantiate the proxy across packages. Solved by: the dispatcher discovers proxy instances via ServiceLoader or a generated registry (a `FeatureMetadata` interface generated alongside the proxy that the dispatcher consumes)
- DI modules (flagzen-spring) will use reflection to call the package-private constructor -- acceptable because reflection is in the DI module, not flagzen-core
