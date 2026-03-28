# How to Use Evaluation Context for Targeting

Pass user or segment attributes to your flag provider for personalized variant resolution.

## Goal

Enable context-aware flag dispatch: resolve different variants based on user ID, plan, locale, or other attributes.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`
- A flag provider that supports context-aware resolution (e.g., OpenFeature, custom provider)

## Steps

### 1. Build an EvaluationContext

Create a context with a targeting key and attributes:

```java
EvaluationContext context = EvaluationContext.builder()
    .targetingKey("user-7291")
    .attribute("plan", "enterprise")
    .attribute("locale", "en-US")
    .attribute("country", "US")
    .build();
```

- `targetingKey`: Primary identifier (user ID, account ID, etc.)
- `attribute`: Arbitrary targeting metadata

### 2. Resolve flags using the context

Pass the context to your flag provider:

```java
FlagProvider provider = new OpenFeatureFlagProvider(client);
Optional<String> value = provider.getString("checkout-flow", context);
```

The provider can now use the context to decide which variant to activate based on user attributes.

### 3. Use FlagContext for thread-scoped context (optional)

For request-scoped targeting in web applications, use `FlagContext.run()`:

```java
@RestController
public class CheckoutController {
    @Autowired
    private CheckoutFlow checkoutFlow;

    @PostMapping("/checkout")
    public String checkout(@RequestParam String userId) {
        EvaluationContext context = EvaluationContext.builder()
            .targetingKey(userId)
            .attribute("plan", getUserPlan(userId))
            .build();

        return FlagContext.run(context, () -> {
            // All flag resolutions within this block use the context
            return checkoutFlow.execute();
        });
    }
}
```

`FlagContext.run()` sets the context in thread-local storage for the duration of the block.

### 4. Access context from within your flag provider

If you implement a custom provider, retrieve the context:

```java
public class MyFlagProvider implements FlagProvider {
    @Override
    public Optional<String> getString(String key, EvaluationContext context) {
        String userId = context.targetingKey();
        String plan = (String) context.attributes().get("plan");

        // Use userId and plan to decide variant
        if ("enterprise".equals(plan)) {
            return Optional.of("PREMIUM");
        }
        return Optional.of("STANDARD");
    }
}
```

### 5. (Optional) Use with Spring Boot

Combine with Spring auto-configuration for dependency injection:

```java
@Service
public class OrderService {
    @Autowired
    private CheckoutFlow checkoutFlow;

    @Autowired
    private UserRepository users;

    public String processOrder(String userId) {
        User user = users.findById(userId).orElseThrow();

        EvaluationContext context = EvaluationContext.builder()
            .targetingKey(userId)
            .attribute("plan", user.getPlan())
            .attribute("signup-date", user.getCreatedAt())
            .build();

        return FlagContext.run(context, () -> {
            return checkoutFlow.execute();
        });
    }
}
```

### 6. (Optional) Use with nested context blocks

Stack contexts for hierarchical resolution:

```java
EvaluationContext userContext = EvaluationContext.builder()
    .targetingKey("user-123")
    .attribute("plan", "enterprise")
    .build();

EvaluationContext requestContext = EvaluationContext.builder()
    .targetingKey("request-456")
    .attribute("region", "eu")
    .build();

FlagContext.run(userContext, () -> {
    // user-123, enterprise plan applies here

    FlagContext.run(requestContext, () -> {
        // requestContext overwrites userContext
        // This block sees request-456, eu region
    });

    // Back to userContext (user-123, enterprise)
});
```

## Result

Flag resolution respects user attributes and context, enabling personalized feature rollouts, A/B testing, and segment-based feature control.

## See Also

- [Reference: EvaluationContext](../reference/flag-provider.md) — builder API
- [Reference: FlagContext](../reference/feature-dispatcher.md) — thread-scoped context
- [How-to: OpenFeature Integration](openfeature.md) — context-aware external provider
- [How-to: Custom Provider](custom-provider.md) — implement context-aware resolution
