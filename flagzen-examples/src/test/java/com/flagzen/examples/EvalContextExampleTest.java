package com.flagzen.examples;

import com.flagzen.EvaluationContext;
import com.flagzen.FlagContext;
import com.flagzen.examples.context.PricingTier;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class EvalContextExampleTest {
    @Test
    void contextDrivenDispatch() {
        // Provider that returns different values based on context
        FlagProvider provider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.of("STANDARD");
            }
            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                if (context != null && "enterprise".equals(context.attributes().get("plan"))) {
                    return Optional.of("PREMIUM");
                }
                return getString(key);
            }
        };

        var dispatcher = new DefaultFeatureDispatcher(provider);
        PricingTier pricing = dispatcher.resolve(PricingTier.class);

        // Without context: standard pricing
        assertThat(pricing.discount()).isEqualTo(0.0);

        // With context: premium pricing
        EvaluationContext ctx = EvaluationContext.builder()
                .attribute("plan", "enterprise")
                .build();
        FlagContext.run(ctx, () -> {
            assertThat(pricing.discount()).isEqualTo(0.20);
        });
    }
}
