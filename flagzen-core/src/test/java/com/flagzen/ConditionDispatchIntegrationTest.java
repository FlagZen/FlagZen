package com.flagzen;

import com.flagzen.acceptance.fixtures.PricingTier;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test for condition-based dispatch through FeatureDispatcher.
 *
 * Port-to-port: FeatureDispatcher.resolve() (driving port) -> FlagProvider (driven port).
 *
 * <p>Feature: PricingTier with ordered dispatch:
 * <ul>
 *   <li>order=1: exact match "free" -> FreeTier</li>
 *   <li>order=2: condition IsEnterpriseTier -> EnterpriseTier</li>
 *   <li>default: StandardTier</li>
 * </ul>
 *
 * Test Budget: 3 behaviors x 2 = 6 max. Using 4 tests.
 *
 * Behaviors:
 * 1. Exact match dispatch (flag="free" -> FreeTier)
 * 2. Condition predicate dispatch (flag="enterprise" -> EnterpriseTier via predicate)
 * 3. Default fallback dispatch (flag="unknown" -> StandardTier)
 * 4. Unmatched variant exception lists predicates for condition features
 */
class ConditionDispatchIntegrationTest {

    @Test
    void dispatchesToExactMatchVariantByOrder() {
        // Given: flag value matches exact variant at order=1
        InMemoryFlagProvider flagProvider = new InMemoryFlagProvider();
        flagProvider.set("pricing-tier", "free");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider);

        // When: resolving the condition-based feature
        PricingTier tier = dispatcher.resolve(PricingTier.class);

        // Then: dispatches to FreeTier (exact match at order=1)
        assertThat(tier.tierName()).isEqualTo("Free");
    }

    @Test
    void dispatchesToConditionPredicateVariant() {
        // Given: flag value matches condition predicate at order=2
        InMemoryFlagProvider flagProvider = new InMemoryFlagProvider();
        flagProvider.set("pricing-tier", "enterprise");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider);

        // When: resolving the condition-based feature
        PricingTier tier = dispatcher.resolve(PricingTier.class);

        // Then: dispatches to EnterpriseTier (predicate match at order=2)
        assertThat(tier.tierName()).isEqualTo("Enterprise");
    }

    @Test
    void fallsBackToDefaultWhenNoPredicateMatches() {
        // Given: flag value matches neither exact nor predicate variants
        InMemoryFlagProvider flagProvider = new InMemoryFlagProvider();
        flagProvider.set("pricing-tier", "unknown-plan");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider);

        // When: resolving the condition-based feature
        PricingTier tier = dispatcher.resolve(PricingTier.class);

        // Then: falls back to StandardTier (default variant)
        assertThat(tier.tierName()).isEqualTo("Standard");
    }

    @Test
    void unmatchedVariantExceptionIncludesPredicateInfoForConditionFeatures() {
        // Given: a condition feature where no predicate matches and no default exists
        // When: the factory method creates the exception
        UnmatchedVariantException exception = UnmatchedVariantException.forConditionFeature(
                "pricing-tier", "unknown",
                "IsEnterpriseTier(order=2)");

        // Then: the message includes flag key, value, and predicate info
        assertThat(exception.getMessage()).contains("pricing-tier");
        assertThat(exception.getMessage()).contains("unknown");
        assertThat(exception.getMessage()).contains("IsEnterpriseTier");
        assertThat(exception.getMessage()).contains("Predicates evaluated");
    }
}
