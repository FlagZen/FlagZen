# FlagZen

<!-- markdownlint-disable MD033 -->
<p align="center">
  <img src="assets/logo.png" alt="FlagZen" width="120">
</p>
<!-- markdownlint-enable MD033 -->

**Type-safe polymorphic dispatch layer for feature flags in Java 17+.**

FlagZen sits between your code and your flag provider. Define a `@Feature` interface, implement `@Variant` classes, and the generated proxy routes method calls to the active variant at runtime. No `if/else` chains, no string comparisons, no reflection.

Your flag provider (LaunchDarkly, OpenFeature, Togglz) handles A/B testing, rollouts, and targeting. FlagZen handles type-safe dispatch.

## Why FlagZen?

- **Type-safe**: feature flags are interfaces, not strings. The compiler catches wiring errors.
- **Zero reflection**: all dispatch code is generated at compile time by an annotation processor.
- **Dynamic**: proxies re-evaluate the flag on every method call. Change the flag, change the behavior -- no restart.
- **Testable**: pin flags in tests with `@PinFlag`, load from files with `@FlagSource`, or use `TestFlagContext` programmatically.
- **Provider-agnostic**: plug in LaunchDarkly, OpenFeature, Togglz, environment variables, or your own source via the `FlagProvider` SPI. FlagZen passes evaluation context through to your provider for targeting and A/B testing -- it doesn't reimplement what your provider already does.

## Quick Links

| | |
| --- | --- |
| **[Tutorials](tutorials/getting-started.md)** | New to FlagZen? Start with the getting started guide. |
| **[How-To Guides](how-to/typed-dispatch.md)** | Solve specific problems: typed dispatch, Spring Boot, env vars. |
| **[Reference](reference/annotations.md)** | Complete API reference for all annotations, interfaces, and modules. |
| **[Explanation](explanation/architecture.md)** | Understand the architecture and design decisions behind FlagZen. |

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

See [Getting Started](tutorials/getting-started.md) for the full walkthrough.

## Works With

| Provider | Module | A/B Testing | Targeting | Dashboard |
| --- | --- | --- | --- | --- |
| [LaunchDarkly](how-to/launchdarkly.md) | `flagzen-launchdarkly` | Yes | Yes | Yes |
| [OpenFeature](how-to/openfeature.md) | `flagzen-openfeature` | Depends on backend | Depends on backend | Depends on backend |
| [Togglz](how-to/togglz.md) | `flagzen-togglz` | No | Via UserProvider | Web console |
| [Environment variables](how-to/environment-variables.md) | `flagzen-env` | No | No | No |
| Custom | [Write your own](how-to/custom-provider.md) | You decide | You decide | You decide |
