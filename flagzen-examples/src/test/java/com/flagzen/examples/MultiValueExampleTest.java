package com.flagzen.examples;

import com.flagzen.examples.multivalue.Greeting;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MultiValueExampleTest {
    @Test
    void anyMappedValueResolvesToSameVariant() {
        var provider = new InMemoryFlagProvider();
        var dispatcher = new DefaultFeatureDispatcher(provider);
        Greeting greeting = dispatcher.resolve(Greeting.class);

        provider.set("greeting-style", "FORMAL");
        assertThat(greeting.greet("Alice")).isEqualTo("Dear Alice");

        provider.set("greeting-style", "BUSINESS");
        assertThat(greeting.greet("Alice")).isEqualTo("Dear Alice");

        provider.set("greeting-style", "CASUAL");
        assertThat(greeting.greet("Bob")).isEqualTo("Hey Bob!");
    }
}
