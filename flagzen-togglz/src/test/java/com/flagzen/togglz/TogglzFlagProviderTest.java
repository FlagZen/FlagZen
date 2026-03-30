package com.flagzen.togglz;

import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TogglzFlagProvider} through the {@link FlagProvider} port.
 *
 * <p>FeatureManager is the infrastructure boundary (driven port), so it is mocked.
 * Each case exercises boolean flag resolution, absence detection, and case-insensitive lookup.
 */
class TogglzFlagProviderTest {

    enum TestFeature implements Feature {
        DARK_MODE, CHECKOUT_FLOW
    }

    static Stream<Arguments> booleanFlagResolutionCases() {
        return Stream.of(
                // Enabled feature returns true
                Arguments.of("DARK_MODE", new FeatureState(TestFeature.DARK_MODE, true), Optional.of(true)),
                // Disabled feature returns false
                Arguments.of("DARK_MODE", new FeatureState(TestFeature.DARK_MODE, false), Optional.of(false)),
                // Unknown feature returns empty
                Arguments.of("NONEXISTENT", null, Optional.empty()),
                // Null state returns empty
                Arguments.of("CHECKOUT_FLOW", null, Optional.empty())
        );
    }

    @ParameterizedTest(name = "getBoolean(\"{0}\") with state {1} returns {2}")
    @MethodSource("booleanFlagResolutionCases")
    void resolvesBooleanFlagFromTogglzFeatureState(
            String key,
            FeatureState state,
            Optional<Boolean> expected) {

        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.getFeatures()).thenReturn(Set.of(TestFeature.values()));

        // For known features, configure getFeatureState; state may be null for null-state case
        if (!"NONEXISTENT".equals(key)) {
            Feature feature = TestFeature.valueOf(key);
            when(featureManager.getFeatureState(feature)).thenReturn(state);
        }

        FlagProvider provider = TogglzFlagProvider.create(featureManager);

        assertThat(provider.getBoolean(key)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "getBoolean(\"{0}\") resolves case-insensitively")
    @MethodSource("caseInsensitiveLookupCases")
    void resolvesBooleanFlagCaseInsensitively(String key, Optional<Boolean> expected) {
        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.getFeatures()).thenReturn(Set.of(TestFeature.values()));
        when(featureManager.getFeatureState(TestFeature.DARK_MODE))
                .thenReturn(new FeatureState(TestFeature.DARK_MODE, true));

        FlagProvider provider = TogglzFlagProvider.create(featureManager);

        assertThat(provider.getBoolean(key)).isEqualTo(expected);
    }

    static Stream<Arguments> caseInsensitiveLookupCases() {
        return Stream.of(
                Arguments.of("dark_mode", Optional.of(true)),
                Arguments.of("Dark_Mode", Optional.of(true)),
                Arguments.of("DARK_MODE", Optional.of(true)),
                Arguments.of("dark_MODE", Optional.of(true))
        );
    }

    // --- getString: strategy parameter resolution ---

    static Stream<Arguments> stringFlagResolutionCases() {
        return Stream.of(
                // Feature with "value" parameter returns the parameter value
                Arguments.of("DARK_MODE", featureWithParameter(TestFeature.DARK_MODE, true, "premium"),
                        Optional.of("premium")),
                // Feature without "value" parameter returns enabled state as string
                Arguments.of("DARK_MODE", new FeatureState(TestFeature.DARK_MODE, true),
                        Optional.of("true")),
                // Feature disabled without "value" parameter returns "false"
                Arguments.of("DARK_MODE", new FeatureState(TestFeature.DARK_MODE, false),
                        Optional.of("false")),
                // Feature with empty "value" parameter falls back to enabled state
                Arguments.of("DARK_MODE", featureWithParameter(TestFeature.DARK_MODE, true, ""),
                        Optional.of("true")),
                // Unknown feature returns empty
                Arguments.of("NONEXISTENT", null, Optional.empty())
        );
    }

    @ParameterizedTest(name = "getString(\"{0}\") with state {1} returns {2}")
    @MethodSource("stringFlagResolutionCases")
    void resolvesStringFlagFromTogglzStrategyParameter(
            String key,
            FeatureState state,
            Optional<String> expected) {

        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.getFeatures()).thenReturn(Set.of(TestFeature.values()));

        if (!"NONEXISTENT".equals(key)) {
            Feature feature = TestFeature.valueOf(key);
            when(featureManager.getFeatureState(feature)).thenReturn(state);
        }

        FlagProvider provider = TogglzFlagProvider.create(featureManager);

        assertThat(provider.getString(key)).isEqualTo(expected);
    }

    // --- Numeric types parse correctly from getString via FlagProvider defaults ---

    @ParameterizedTest(name = "getInt(\"{0}\") with value parameter \"{1}\" returns {2}")
    @CsvSource({
            "DARK_MODE, 42, 42",
            "DARK_MODE, -7, -7"
    })
    void resolvesIntFlagViaStringParsing(String key, String paramValue, int expected) {
        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.getFeatures()).thenReturn(Set.of(TestFeature.values()));
        when(featureManager.getFeatureState(TestFeature.DARK_MODE))
                .thenReturn(featureWithParameter(TestFeature.DARK_MODE, true, paramValue));

        FlagProvider provider = TogglzFlagProvider.create(featureManager);

        assertThat(provider.getInt(key)).isEqualTo(OptionalInt.of(expected));
    }

    @Test
    void resolvesDoubleFlagViaStringParsing() {
        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.getFeatures()).thenReturn(Set.of(TestFeature.values()));
        when(featureManager.getFeatureState(TestFeature.DARK_MODE))
                .thenReturn(featureWithParameter(TestFeature.DARK_MODE, true, "3.14"));

        FlagProvider provider = TogglzFlagProvider.create(featureManager);

        assertThat(provider.getDouble("DARK_MODE")).isEqualTo(OptionalDouble.of(3.14));
    }

    private static FeatureState featureWithParameter(Feature feature, boolean enabled, String value) {
        FeatureState state = new FeatureState(feature, enabled);
        state.setParameter("value", value);
        return state;
    }
}
