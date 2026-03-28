package com.flagzen.acceptance.spring;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.CheckoutFlowMetadata;
import com.flagzen.acceptance.fixtures.DefaultCheckout;
import com.flagzen.spi.FlagProvider;
import com.flagzen.spring.FlagZenAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for flagzen-spring auto-configuration.
 * Implements the scenarios from walking-skeleton.feature as JUnit 5 tests.
 */
class SpringAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlagZenAutoConfiguration.class));

    @AfterEach
    void resetFixtures() {
        CheckoutFlowMetadata.reset();
    }

    // -- Scenario 1: Developer injects a feature proxy and dispatches to the active variant --

    @Test
    @DisplayName("Developer injects a feature proxy and dispatches to the active variant")
    void injectsFeatureProxyAndDispatchesToActiveVariant() {
        contextRunner
                .withUserConfiguration(ClassicFlagProviderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(FeatureDispatcher.class);
                    assertThat(context).hasBean("checkoutFlow");
                    CheckoutFlow checkoutFlow = context.getBean(CheckoutFlow.class);
                    assertThat(checkoutFlow.execute()).isEqualTo("ClassicCheckout");
                });
    }

    // -- Scenario 2: Application starts with fallback provider when no explicit provider is defined --

    @Test
    @DisplayName("Application starts with fallback provider when no explicit provider is defined")
    void startsWithFallbackProviderWhenNoExplicitProviderDefined() {
        CheckoutFlowMetadata.setDefaultVariant(DefaultCheckout::new);
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(FeatureDispatcher.class);
                    assertThat(context).hasBean("checkoutFlow");
                    CheckoutFlow checkoutFlow = context.getBean(CheckoutFlow.class);
                    assertThat(checkoutFlow.execute()).isEqualTo("DefaultCheckout");
                });
    }

    // -- Scenario 3: Custom dispatcher takes precedence over auto-configured one --

    @Test
    @DisplayName("Custom dispatcher takes precedence over auto-configured one")
    void customDispatcherTakesPrecedence() {
        contextRunner
                .withUserConfiguration(CustomDispatcherConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(FeatureDispatcher.class);
                    FeatureDispatcher dispatcher = context.getBean(FeatureDispatcher.class);
                    assertThat(dispatcher).isSameAs(CustomDispatcherConfig.CUSTOM_DISPATCHER);
                });
    }

    @Configuration
    static class ClassicFlagProviderConfig {
        @Bean
        FlagProvider flagProvider() {
            return key -> Optional.of("CLASSIC");
        }
    }

    @Configuration
    static class CustomDispatcherConfig {
        static final FeatureDispatcher CUSTOM_DISPATCHER = new FeatureDispatcher() {
            @Override
            public <T> T resolve(Class<T> featureType) {
                throw new UnsupportedOperationException("Custom dispatcher");
            }

            @Override
            public <T> T resolve(Class<T> featureType, EvaluationContext context) {
                throw new UnsupportedOperationException("Custom dispatcher");
            }
        };

        @Bean
        FeatureDispatcher featureDispatcher() {
            return CUSTOM_DISPATCHER;
        }
    }
}
