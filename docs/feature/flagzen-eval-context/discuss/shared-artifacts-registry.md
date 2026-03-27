# Shared Artifacts Registry: Evaluation Context (flagzen-eval-context)

## Artifacts

### EvaluationContext

```yaml
source_of_truth: "com.flagzen.EvaluationContext"
consumers:
  - "FeatureDispatcher.resolve(Class, EvaluationContext)"
  - "FlagProvider.getString(String, EvaluationContext)"
  - "FlagContext.run(EvaluationContext, Runnable/Supplier)"
  - "ContextAccessor.getContext() return type"
  - "FlagZenConfiguration.defaultContext"
  - "Generated proxy dispatch logic"
  - "TestFlagContext (future enhancement)"
owner: "flagzen-core"
integration_risk: "HIGH -- central type used by all resolution paths. Any change ripples everywhere."
validation: "Type is immutable. Builder validates nothing (flexibility by design). Must have equals/hashCode/toString."
```

### EvaluationContext.Builder

```yaml
source_of_truth: "com.flagzen.EvaluationContext.Builder (nested class or static method)"
consumers:
  - "User code creating contexts"
  - "ContextAccessor implementations"
  - "Test code"
owner: "flagzen-core"
integration_risk: "LOW -- builder is a construction API, not a shared runtime artifact."
validation: "Builder methods are fluent. build() produces immutable EvaluationContext."
```

### FeatureDispatcher.resolve(Class, EvaluationContext)

```yaml
source_of_truth: "com.flagzen.FeatureDispatcher"
consumers:
  - "User code (explicit context passing)"
  - "Generated proxies (context forwarding)"
  - "DefaultFeatureDispatcher (resolution logic)"
owner: "flagzen-core"
integration_risk: "HIGH -- public API surface. Must be backward compatible. Overload must not break existing resolve(Class) calls."
validation: "Existing resolve(Class) still compiles and works. New overload passes context to FlagProvider."
```

### FlagProvider.getString(String, EvaluationContext)

```yaml
source_of_truth: "com.flagzen.spi.FlagProvider"
consumers:
  - "Generated proxies (call site)"
  - "All FlagProvider implementations (LaunchDarkly, Togglz, OpenFeature, InMemory, Env)"
  - "DefaultFeatureDispatcher"
owner: "flagzen-core"
integration_risk: "HIGH -- SPI contract change. Default method required for backward compatibility with existing providers."
validation: "Default method delegates to getString(String). Existing providers compile and work without changes."
```

### FlagContext

```yaml
source_of_truth: "com.flagzen.FlagContext"
consumers:
  - "User code (FlagContext.run())"
  - "DefaultFeatureDispatcher (reads current scoped context)"
owner: "flagzen-core"
integration_risk: "MEDIUM -- internal scoping mechanism. ThreadLocal/ScopedValue choice is internal detail."
validation: "Context is scoped to block. Nested contexts use innermost. Context cleared on exit. Thread-safe."
```

### ContextAccessor SPI

```yaml
source_of_truth: "com.flagzen.spi.ContextAccessor"
consumers:
  - "flagzen-reactor (M6)"
  - "flagzen-mutiny (M6)"
  - "Custom user implementations"
  - "DefaultFeatureDispatcher (ServiceLoader discovery)"
owner: "flagzen-core"
integration_risk: "MEDIUM -- SPI defined now, implementations in M6. Must be stable."
validation: "Interface has getContext() and priority(). ServiceLoader discovery works. Priority ordering is deterministic."
```

### Resolution Order

```yaml
source_of_truth: "com.flagzen.internal.DefaultFeatureDispatcher"
consumers:
  - "Documentation"
  - "User mental model"
  - "Test assertions"
  - "Javadoc on FeatureDispatcher.resolve()"
owner: "flagzen-core"
integration_risk: "HIGH -- must be consistent, documented, and match user expectations."
validation: "Order is: explicit > accessor > scoped > default. Documented in Javadoc. Tested exhaustively."
```

### ScopedValue/ThreadLocal Storage

```yaml
source_of_truth: "com.flagzen.internal (implementation detail)"
consumers:
  - "FlagContext.run()"
  - "DefaultFeatureDispatcher context resolution"
owner: "flagzen-core"
integration_risk: "MEDIUM -- Java version detection (21+ vs 17-20) adds complexity. Must be invisible to users."
validation: "Correct behavior on both Java 17 and Java 21+. No API surface difference between the two."
```

## Integration Checkpoints

|                 Checkpoint                  |                                        Validates                                         |      Stories       |
| ------------------------------------------- | ---------------------------------------------------------------------------------------- | ------------------ |
| FlagProvider default method backward compat | Existing providers compile and work without changes after adding getString(key, context) | US-EC-03           |
| Generated proxy context forwarding          | Annotation processor generates proxies that pass EvaluationContext to FlagProvider       | US-EC-04           |
| Resolution order determinism                | All four context sources are tried in documented order                                   | US-EC-07           |
| Thread safety under concurrency             | FlagContext.run() isolates context per thread/virtual-thread                             | US-EC-05, US-EC-08 |
| Zero-reflection compliance                  | No java.lang.reflect imports in flagzen-core after changes                               | All stories        |
