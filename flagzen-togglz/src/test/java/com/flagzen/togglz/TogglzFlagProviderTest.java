package com.flagzen.togglz;

import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;

import java.util.Optional;
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
}
