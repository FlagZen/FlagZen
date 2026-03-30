package com.flagzen.processor;

import com.flagzen.FeatureType;
import com.flagzen.FallbackStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConditionModel, extended VariantModel (with condition/order),
 * and FeatureModel.hasOrderedDispatch.
 *
 * Port-to-port: these are processor-internal records with stable public interfaces.
 * Test Budget: 3 behaviors x 2 = 6 max unit tests. Using 4 tests.
 */
class ConditionModelCollectionTest {

    @Test
    void variantModelCarriesNonNullConditionAndOrder() {
        // Given: a ConditionModel representing matches = SomePredicate.class
        ConditionModel condition = new ConditionModel("com.example.IsEnterprise", false);

        // When: a VariantModel is created with condition and order
        VariantModel variant = new VariantModel(
                "com.example.EnterpriseRate", "high", condition, 1);

        // Then: the variant carries the condition and order
        assertThat(variant.condition()).isNotNull();
        assertThat(variant.condition().predicateClassName()).isEqualTo("com.example.IsEnterprise");
        assertThat(variant.condition().negated()).isFalse();
        assertThat(variant.order()).isEqualTo(1);
    }

    @Test
    void variantModelWithoutConditionHasNullConditionAndMaxOrder() {
        // Given/When: a VariantModel created with the existing constructor (backward compat)
        VariantModel variant = new VariantModel("com.example.DarkTheme", "dark");

        // Then: condition is null and order is MAX_VALUE
        assertThat(variant.condition()).isNull();
        assertThat(variant.order()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void hasOrderedDispatchTrueWhenAnyVariantHasExplicitOrder() {
        // Given: variants where one has explicit order
        ConditionModel condition = new ConditionModel("com.example.IsEnterprise", false);
        VariantModel ordered = new VariantModel(
                "com.example.EnterpriseRate", "high", condition, 1);
        VariantModel unordered = new VariantModel("com.example.DefaultRate", "low");

        // When: FeatureModel is created with these variants
        FeatureModel model = new FeatureModel(
                "com.example", "RateLimiter", "rate-limiter",
                FallbackStrategy.EXCEPTION, FeatureType.STRING,
                List.of(), List.of(ordered, unordered), null);

        // Then: hasOrderedDispatch is true
        assertThat(model.hasOrderedDispatch()).isTrue();
    }

    @Test
    void hasOrderedDispatchFalseWhenNoVariantsHaveExplicitOrder() {
        // Given: variants with no explicit order (all MAX_VALUE)
        VariantModel v1 = new VariantModel("com.example.DarkTheme", "dark");
        VariantModel v2 = new VariantModel("com.example.LightTheme", "light");

        // When: FeatureModel is created with these variants
        FeatureModel model = new FeatureModel(
                "com.example", "Theme", "theme",
                FallbackStrategy.EXCEPTION, FeatureType.STRING,
                List.of(), List.of(v1, v2), null);

        // Then: hasOrderedDispatch is false
        assertThat(model.hasOrderedDispatch()).isFalse();
    }
}
