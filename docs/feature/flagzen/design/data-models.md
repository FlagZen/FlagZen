# Data Models -- FlagZen

## Overview

FlagZen is a library, not a data-centric application. Its "data models" are:

1. **Annotation metadata** -- compile-time models extracted by the annotation processor
2. **Runtime models** -- lightweight objects used during flag resolution
3. **Generated proxy structure** -- the shape of generated code

There are no databases, persistence layers, or external data schemas.

## 1. Compile-Time Models (Annotation Processor Internal)

These models exist only during annotation processing. They are internal to the processor and not part of the public API.

### FeatureModel

Represents a processed `@Feature` annotation.

|       Field       |           Type            |                            Description                            |
| ----------------- | ------------------------- | ----------------------------------------------------------------- |
| interfaceName     | Qualified name            | Fully qualified name of the @Feature interface                    |
| flagKey           | String                    | Value from `@Feature("flag-key")`                                 |
| fallbackStrategy  | FallbackStrategy          | REQUIRED, EXCEPTION, or NOOP                                      |
| variantEnumValues | List of String (nullable) | Enum constant names from inner `Variant` enum, or null if no enum |
| packageName       | String                    | Package of the @Feature interface                                 |
| simpleName        | String                    | Simple class name of the @Feature interface                       |
| methods           | List of MethodModel       | Methods declared on the interface                                 |

### VariantModel

Represents a processed `@Variant` annotation.

|      Field       |      Type      |                  Description                   |
| ---------------- | -------------- | ---------------------------------------------- |
| className        | Qualified name | Fully qualified name of the @Variant class     |
| value            | String         | Variant value from `@Variant("VALUE")`         |
| featureInterface | Qualified name | The @Feature interface this variant implements |
| isDefault        | boolean        | True if annotated with `@DefaultVariant`       |

### MethodModel

Represents a method on the @Feature interface (needed for proxy generation).

|    Field    |          Type          |                  Description                   |
| ----------- | ---------------------- | ---------------------------------------------- |
| name        | String                 | Method name                                    |
| returnType  | TypeMirror             | Return type (void, primitive, object, generic) |
| parameters  | List of ParameterModel | Method parameters                              |
| thrownTypes | List of TypeMirror     | Checked exceptions                             |

## 2. Runtime Models

### EvaluationContext

Carries contextual information for flag evaluation (user ID, tenant, attributes). Used for targeted flag resolution (A/B testing, tenant-scoped flags).

|    Field     |          Type           |               Description                |
| ------------ | ----------------------- | ---------------------------------------- |
| targetingKey | String (nullable)       | Primary identifier (user ID, session ID) |
| attributes   | Map of String to Object | Custom attributes for targeting rules    |

EvaluationContext is immutable. Created via builder pattern.

### FlagZenConfiguration

Configuration object built via `FlagZen.configure()`.

|      Field       |             Type             |                       Description                        |
| ---------------- | ---------------------------- | -------------------------------------------------------- |
| provider         | FlagProvider                 | The configured flag source                               |
| contextAccessors | List of ContextAccessor      | Registered context accessors (SPI-discovered + explicit) |
| defaultContext   | EvaluationContext (nullable) | Default context if none provided or discovered           |

## 3. Generated Proxy Structure

The annotation processor generates one proxy class per @Feature interface. The generated code follows this structure (described as a model, not implementation):

### Proxy Class Shape

|    Aspect    |                   Value                    |
| ------------ | ------------------------------------------ |
| Class name   | `{FeatureSimpleName}_FlagZenProxy`         |
| Package      | Same as @Feature interface                 |
| Visibility   | Public class, package-private constructor  |
| Implements   | The @Feature interface                     |
| Dependencies | `FlagProvider`, variant instance suppliers |

### Proxy Internal State

|      Field       |                   Type                    |           Description            |
| ---------------- | ----------------------------------------- | -------------------------------- |
| flagKey          | String (final)                            | The flag key from @Feature       |
| flagProvider     | FlagProvider (final)                      | The configured provider          |
| variants         | Map of String to variant instance (final) | Maps variant value to instance   |
| defaultVariant   | Variant instance (nullable, final)        | @DefaultVariant instance, if any |
| fallbackStrategy | FallbackStrategy (final)                  | Configured strategy              |

### Proxy Dispatch Logic (per method)

For each method on the @Feature interface, the proxy generates a delegating method that:

1. Queries `flagProvider.getString(flagKey)`
2. Looks up the variant instance from the map
3. If found: delegates the method call to that instance
4. If not found and defaultVariant exists: delegates to defaultVariant
5. If not found and no defaultVariant:
   - EXCEPTION: throws `UnmatchedVariantException`
   - NOOP: returns safe default (false, 0, null, Optional.empty(), List.of(), etc.)

## 4. Annotation Definitions

### @Feature

| Attribute |       Type       |  Default   |            Description             |
| --------- | ---------------- | ---------- | ---------------------------------- |
| value     | String           | (required) | Flag key                           |
| fallback  | FallbackStrategy | REQUIRED   | What to do when no variant matches |

Retention: CLASS. Target: TYPE (interfaces only, enforced by processor).

### @Variant

| Attribute |  Type  |        Default        |                          Description                           |
| --------- | ------ | --------------------- | -------------------------------------------------------------- |
| value     | String | (required)            | Variant value to match against flag provider                   |
| of        | Class  | void.class (sentinel) | Target @Feature interface (required for multi-feature classes) |

Retention: CLASS. Target: TYPE (classes only). @Repeatable (for multi-feature implementations).

### @DefaultVariant

| Attribute |  Type  |        Default        |                          Description                           |
| --------- | ------ | --------------------- | -------------------------------------------------------------- |
| of        | Class  | void.class (sentinel) | Target @Feature interface (required for multi-feature classes) |

Retention: CLASS. Target: TYPE (classes only). @Repeatable (for multi-feature implementations).

Marks a class as the fallback variant for its @Feature interface.

### @PinFlag

| Attribute |  Type  |  Default   |     Description      |
| --------- | ------ | ---------- | -------------------- |
| feature   | String | (required) | Flag key to pin      |
| variant   | String | (required) | Variant value to pin |

Retention: RUNTIME (read by JUnit extension). Target: METHOD. @Repeatable.

### @FlagSource

| Attribute |  Type  |  Default   |                     Description                      |
| --------- | ------ | ---------- | ---------------------------------------------------- |
| value     | String | (required) | Classpath resource path to properties/JSON/YAML file |

Retention: RUNTIME. Target: TYPE and METHOD.

## 5. Generated Metadata

### FeatureMetadata (Generated)

Each `@Feature` interface gets a generated `{Feature}_FlagZenMetadata` class that implements `com.flagzen.spi.FeatureMetadata`. This metadata class is registered via `META-INF/services/com.flagzen.spi.FeatureMetadata` and discovered by `FeatureDispatcher` via `ServiceLoader`.

|                 Method                 |   Return Type    |                         Description                         |
| -------------------------------------- | ---------------- | ----------------------------------------------------------- |
| featureType()                          | Class            | The @Feature interface class                                |
| flagKey()                              | String           | The flag key                                                |
| fallbackStrategy()                     | FallbackStrategy | Configured strategy                                         |
| createProxy(FlagProvider, Map, Object) | Object           | Factory method that constructs the proxy within its package |

This solves the cross-package instantiation problem: the metadata class is generated in the same package as the proxy, so it can call the package-private constructor.

## 6. SPI Interface Contracts

### FlagProvider

```
package com.flagzen.spi;

public interface FlagProvider {
    Optional<String> getString(String key);
}
```

Single method. String-only for Release 1. Implementations must be thread-safe.

### FeatureMetadata

```
package com.flagzen.spi;

public interface FeatureMetadata<T> {
    Class<T> featureType();
    String flagKey();
    FallbackStrategy fallbackStrategy();
    T createProxy(FlagProvider provider, Map<String, Supplier<T>> variants, Supplier<T> defaultVariant);
}
```

Generated per @Feature. Discovered via `META-INF/services/com.flagzen.spi.FeatureMetadata`.

### ContextAccessor

```
package com.flagzen.spi;

public interface ContextAccessor {
    Optional<EvaluationContext> getContext();
    int priority();  // lower = higher priority
}
```

Multiple accessors can be registered. Resolution order:

1. Explicit parameter (always wins)
2. ContextAccessor with lowest priority() value
3. Default context (if configured)

## 6. Exception Hierarchy

```
FlagZenException (RuntimeException)
  | -- UnmatchedVariantException  -- flag value has no matching @Variant |
  | -- NoProviderException        -- no FlagProvider configured          |
```

All exceptions include:

- The flag key involved
- The flag value (if applicable)
- Known variant values (if applicable)
- Actionable fix suggestions in the message
