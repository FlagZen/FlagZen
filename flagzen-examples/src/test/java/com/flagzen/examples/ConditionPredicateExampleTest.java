package com.flagzen.examples;

import com.flagzen.examples.conditions.RetryStrategy;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionPredicateExampleTest {

    @Test
    void exactMatchDispatchesToConservativeRetry() {
        var provider = new InMemoryFlagProvider();
        provider.set("max-retries", "3");
        var dispatcher = new DefaultFeatureDispatcher(provider);

        RetryStrategy strategy = dispatcher.resolve(RetryStrategy.class);

        assertThat(strategy.description()).isEqualTo("conservative");
        assertThat(strategy.maxRetries()).isEqualTo(3);
    }

    @Test
    void highValueDispatchesToAggressiveRetry() {
        var provider = new InMemoryFlagProvider();
        provider.set("max-retries", "10");
        var dispatcher = new DefaultFeatureDispatcher(provider);

        RetryStrategy strategy = dispatcher.resolve(RetryStrategy.class);

        assertThat(strategy.description()).isEqualTo("aggressive");
    }

    @Test
    void unmatchedValueFallsBackToStandardRetry() {
        var provider = new InMemoryFlagProvider();
        provider.set("max-retries", "5");
        var dispatcher = new DefaultFeatureDispatcher(provider);

        RetryStrategy strategy = dispatcher.resolve(RetryStrategy.class);

        assertThat(strategy.description()).isEqualTo("standard");
        assertThat(strategy.maxRetries()).isEqualTo(5);
    }
}
