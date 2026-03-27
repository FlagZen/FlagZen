# Journey Visual: Evaluation Context Integration

## Persona

**Kenji Tanaka** -- Senior Java developer at a SaaS company. Has already integrated FlagZen for polymorphic dispatch (M0). Now needs to resolve flags differently per user/tenant for A/B testing and tenant-scoped features. Comfortable with Java 17+, familiar with SPI patterns and builder APIs.

## Journey Goal

Kenji wants to resolve feature flags based on who the current user is (targeting key + attributes), so that different users see different variants -- enabling A/B testing, tenant-scoped rollouts, and user segmentation.

## Emotional Arc

```
Curious -----> Confident -----> Competent -----> Satisfied
  |               |                |                |
"Can FlagZen    "The API feels   "Block-scoped    "This handles all
 do targeting?"  familiar and     context saves    my targeting
                 ergonomic"       boilerplate"     use cases cleanly"
```

**Arc Pattern**: Discovery Joy -- curious start, progressive confidence, satisfied end.

## Journey Flow

```
[1. Discover]     [2. Model]        [3. Pass Explicit]   [4. Block Scope]    [5. SPI Extend]     [6. Verify]
 Context API       Context            Context to           Context via          Custom               Resolution
                   Creation           resolve()            FlagContext.run()    ContextAccessor      Order
     |                |                   |                    |                    |                   |
     v                v                   v                    v                    v                   v
Reads Javadoc    Builds an           Passes context       Scopes context       Implements SPI      Tests all
or docs for      EvaluationContext    to dispatcher        to a block of        for framework-      paths with
resolve() with   with targeting key   .resolve() call      code without         specific context    @PinFlag
context param    + attributes                              threading it         sources             and manual
                                                           through call                             verification
                                                           stack
     |                |                   |                    |                    |                   |
 Feels:           Feels:              Feels:              Feels:              Feels:              Feels:
 Curious,         Confident --        Competent --        Relieved --         Empowered --        Satisfied --
 exploratory      builder pattern     minimal change      no boilerplate      extensibility       predictable
                  is familiar         to existing code     passing context     built in            behavior
```

## Step Details

### Step 1: Discover Context API

Kenji sees the new `resolve(Class<T>, EvaluationContext)` overload in `FeatureDispatcher` Javadoc. He recognizes the pattern from OpenFeature and LaunchDarkly.

```
FeatureDispatcher API (Javadoc or IDE autocomplete)
+-----------------------------------------------------------------+
| interface FeatureDispatcher {                                   |
|                                                                 |
| <T> T resolve(Class<T> featureType);                            |
| <T> T resolve(Class<T> featureType, EvaluationContext context); |
|                                                                 |
| }                                                               |
+-----------------------------------------------------------------+
  Kenji sees: "Ah, same resolve() I know, plus a context overload."
```

**Emotional state**: Curious turning to confident -- the API is a natural extension of what he already knows.

### Step 2: Model an Evaluation Context

Kenji creates an `EvaluationContext` using the builder pattern. Immutable, thread-safe.

```java
EvaluationContext context = EvaluationContext.builder()
    .targetingKey("user-7291")
    .attribute("plan", "enterprise")
    .attribute("region", "eu-west")
    .attribute("beta-tester", true)
    .build();
```

**Emotional state**: Confident -- builder pattern is standard Java idiom. No surprises.

### Step 3: Pass Explicit Context to resolve()

Kenji passes the context directly to the dispatcher. The flag provider receives it for targeted evaluation.

```java
CheckoutFlow checkout = dispatcher.resolve(CheckoutFlow.class, context);
checkout.execute(cart);  // dispatches to variant based on context + flag rules
```

**Emotional state**: Competent -- one-line change from contextless resolution. Existing code stays untouched.

### Step 4: Block-Scoped Context via FlagContext.run()

For request-handling code where threading context through every call is impractical, Kenji uses block-scoped context.

```java
FlagContext.run(context, () -> {
    // All resolve() calls inside this block use the scoped context
    // unless an explicit context is passed
    CheckoutFlow checkout = dispatcher.resolve(CheckoutFlow.class);
    PaymentMethod payment = dispatcher.resolve(PaymentMethod.class);
    // ...
});
```

**Emotional state**: Relieved -- eliminates context-passing boilerplate. Familiar pattern from security contexts and MDC.

### Step 5: SPI Extension via ContextAccessor

For framework integration (e.g., reactive pipelines in M6), Kenji or framework authors implement `ContextAccessor`.

```java
public class RequestContextAccessor implements ContextAccessor {
    @Override
    public Optional<EvaluationContext> getContext() {
        // Read from framework-specific context (e.g., Reactor, Servlet)
        return Optional.ofNullable(RequestContext.current())
            .map(rc -> EvaluationContext.builder()
                .targetingKey(rc.getUserId())
                .attribute("tenant", rc.getTenantId())
                .build());
    }

    @Override
    public int priority() { return 100; }
}
```

**Emotional state**: Empowered -- SPI extensibility means FlagZen adapts to his framework, not the other way around.

### Step 6: Verify Resolution Order

Kenji tests the resolution order: explicit parameter beats ContextAccessor beats block-scoped beats default.

```java
@Test
@PinFlag(feature = "checkout-flow", variant = "PREMIUM")
void explicitContextTakesPrecedence() {
    EvaluationContext vipContext = EvaluationContext.builder()
        .targetingKey("user-vip-42")
        .attribute("plan", "enterprise")
        .build();

    CheckoutFlow checkout = dispatcher.resolve(CheckoutFlow.class, vipContext);
    // Provider receives vipContext, can resolve differently than default
}
```

**Emotional state**: Satisfied -- behavior is predictable, testable, and well-documented.

## Error Paths

### Error 1: Null Targeting Key

```
Kenji creates EvaluationContext without a targeting key.
Result: Context is valid (targeting key is nullable).
         FlagProvider decides how to handle missing key -- FlagZen is not a rules engine.
```

### Error 2: FlagProvider Ignores Context

```
Kenji passes context to resolve(), but the provider (e.g., InMemoryFlagProvider)
ignores it and returns the same value regardless.
Result: Works correctly -- non-context-aware providers fall back to their normal behavior.
         The getString(key, context) default method delegates to getString(key).
```

### Error 3: No Context Available (block-scoped + no accessor + no default)

```
Kenji calls resolve(Class) outside any FlagContext.run() block, with no
ContextAccessor registered and no default context configured.
Result: Resolution proceeds with null context -- provider receives getString(key) only.
         This is the M0 behavior, fully backward-compatible.
```

### Error 4: Multiple ContextAccessors with Same Priority

```
Two ContextAccessors are registered with the same priority() value.
Result: Deterministic but unspecified order. Document that equal-priority
         accessors should be avoided. No exception thrown.
```

## Integration Points

|    From Step    |       To Step       |                    Shared Artifact                     |                       Risk                       |
| --------------- | ------------------- | ------------------------------------------------------ | ------------------------------------------------ |
| 2 (Model)       | 3 (Explicit)        | `EvaluationContext` instance                           | LOW -- same object passed directly               |
| 2 (Model)       | 4 (Block Scope)     | `EvaluationContext` instance                           | LOW -- same object, different delivery mechanism |
| 3 (Explicit)    | FlagProvider SPI    | `getString(key, context)` overload                     | MEDIUM -- providers must handle new overload     |
| 4 (Block Scope) | Internal resolution | ScopedValue/ThreadLocal storage                        | MEDIUM -- Java version detection needed          |
| 5 (SPI Extend)  | Internal resolution | ContextAccessor priority ordering                      | LOW -- ServiceLoader + priority sort             |
| All steps       | Resolution order    | Priority chain: explicit > accessor > scoped > default | HIGH -- must be consistent and documented        |
