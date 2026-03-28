package com.flagzen.acceptance;

import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.RetryStrategy;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance test for step 01-02: Integer-typed feature runtime dispatch.
 *
 * Scenario: Developer resolves an integer-typed feature to the matching variant at runtime.
 * Port-to-port: FeatureDispatcher.resolve() (driving port) -> FlagProvider.getInt() (driven port).
 */
class IntDispatchAcceptanceTest {

    @Test
    void resolvesIntegerTypedFeatureToMatchingVariantAtRuntime() {
        // Given: a compiled feature "RetryStrategy" with INT variants 3 and 10
        // (provided by RetryStrategyMetadata registered via ServiceLoader)

        // And: an in-memory flag provider with "max-retries" set to "3"
        InMemoryFlagProvider flagProvider = new InMemoryFlagProvider();
        flagProvider.set("max-retries", "3");

        // And: the dispatcher is configured with this provider
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider);

        // When: the developer resolves "RetryStrategy" through the dispatcher
        RetryStrategy retryStrategy = dispatcher.resolve(RetryStrategy.class);

        // And: calls "maxRetries" on the resolved proxy
        int result = retryStrategy.maxRetries();

        // Then: the call is handled by the "ConservativeRetry" variant (intValue=3)
        assertThat(result).isEqualTo(3);
    }
}
