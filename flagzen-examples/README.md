# flagzen-examples

Runnable examples demonstrating FlagZen features. **This module is not published to Maven Central.**

## Examples

| Package | Demonstrates | Key Module |
| --- | --- | --- |
| `basic` | `@Feature` / `@Variant` string dispatch | [flagzen-core](../flagzen-core/README.md) |
| `typed` | `INT`-typed feature with `@Variant(intValue = ...)` | [flagzen-core](../flagzen-core/README.md) |
| `multivalue` | Multi-value variants (`@Variant(value = {...})`) | [flagzen-core](../flagzen-core/README.md) |
| `context` | `EvaluationContext` propagation | [flagzen-core](../flagzen-core/README.md) |
| `testing` | `@PinFlag`, `@FlagSource`, `FlagZenExtension` | [flagzen-test](../flagzen-test/README.md) |
| `env` | Environment variable flag provider | [flagzen-env](../flagzen-env/README.md) |
| `spring` | Spring Boot auto-wired feature proxies | [flagzen-spring](../flagzen-spring/README.md) |
| `openfeature` | OpenFeature SDK adapter | [flagzen-openfeature](../flagzen-openfeature/README.md) |

## Running

```bash
./gradlew :flagzen-examples:run
```

Or open individual example classes in your IDE and run them directly.

## See Also

- [flagzen-core](../flagzen-core/README.md) -- annotations, processor, dispatcher
- [flagzen-test](../flagzen-test/README.md) -- JUnit 5 testing extension
- [flagzen-key-mapping](../flagzen-key-mapping/README.md) -- key format parsing
- [flagzen-env](../flagzen-env/README.md) -- environment variable provider
- [flagzen-spring](../flagzen-spring/README.md) -- Spring Boot integration
- [flagzen-openfeature](../flagzen-openfeature/README.md) -- OpenFeature adapter
- [Root README](../README.md) -- project overview and quick start
