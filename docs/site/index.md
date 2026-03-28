# FlagZen

**Compile-time, zero-reflection feature flag framework for Java 17+.**

FlagZen turns feature flags into polymorphic dispatch: define a `@Feature` interface, implement `@Variant` classes, and the generated proxy routes method calls to the active variant at runtime. No `if/else` chains, no string comparisons, no reflection.

## Why FlagZen?

- **Type-safe**: feature flags are interfaces, not strings. The compiler catches wiring errors.
- **Zero reflection**: all dispatch code is generated at compile time by an annotation processor.
- **Dynamic**: proxies re-evaluate the flag on every method call. Change the flag, change the behavior -- no restart.
- **Testable**: pin flags in tests with `@PinFlag`, load from files with `@FlagSource`, or use `TestFlagContext` programmatically.
- **Extensible**: plug in any flag source via the `FlagProvider` SPI -- environment variables, OpenFeature, or your own.

## Quick Links

<div class="grid cards" markdown>

- :material-school: **[Getting Started](tutorials/getting-started.md)**

    New to FlagZen? Start here.

- :material-book-open-variant: **[How-To Guides](how-to/typed-dispatch.md)**

    Solve specific problems: typed dispatch, Spring Boot, env vars.

- :material-code-tags: **[Reference](reference/annotations.md)**

    Complete API reference for all annotations, interfaces, and modules.

- :material-head-lightbulb: **[Explanation](explanation/architecture.md)**

    Understand the architecture and design decisions behind FlagZen.

</div>

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

See [Getting Started](tutorials/getting-started.md) for the full walkthrough.
