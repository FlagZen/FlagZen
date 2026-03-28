package com.flagzen.examples;

import com.flagzen.examples.typed.CacheStrategy;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TypedDispatchExampleTest {
    @Test
    void dispatchesOnIntegerFlagValue() {
        var provider = new InMemoryFlagProvider();
        provider.set("cache-ttl", "3600");
        var dispatcher = new DefaultFeatureDispatcher(provider);

        CacheStrategy cache = dispatcher.resolve(CacheStrategy.class);

        assertThat(cache.ttlSeconds()).isEqualTo(3600);
    }
}
