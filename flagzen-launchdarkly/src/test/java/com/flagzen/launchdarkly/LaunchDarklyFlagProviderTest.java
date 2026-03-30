package com.flagzen.launchdarkly;

import com.flagzen.spi.FlagProvider;
import com.launchdarkly.sdk.EvaluationDetail;
import com.launchdarkly.sdk.EvaluationReason;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link LaunchDarklyFlagProvider} through the {@link FlagProvider} port.
 *
 * <p>LDClientInterface is the infrastructure boundary (driven port), so it is mocked.
 * Each case exercises reason-kind-based absence detection per ADR-021.
 */
class LaunchDarklyFlagProviderTest {

    static Stream<Arguments> flagResolutionCases() {
        return Stream.of(
                // Happy path: FALLTHROUGH reason returns the resolved value
                Arguments.of(
                        "fallthrough-flag",
                        EvaluationDetail.fromValue("enabled", 0, EvaluationReason.fallthrough()),
                        Optional.of("enabled")
                ),
                // TARGET_MATCH reason returns the resolved value
                Arguments.of(
                        "target-flag",
                        EvaluationDetail.fromValue("targeted-value", 1, EvaluationReason.targetMatch()),
                        Optional.of("targeted-value")
                ),
                // OFF reason returns the off-variation value (NOT empty)
                Arguments.of(
                        "off-flag",
                        EvaluationDetail.fromValue("off-value", 2, EvaluationReason.off()),
                        Optional.of("off-value")
                ),
                // ERROR reason returns empty
                Arguments.of(
                        "error-flag",
                        EvaluationDetail.fromValue("default", -1,
                                EvaluationReason.error(EvaluationReason.ErrorKind.FLAG_NOT_FOUND)),
                        Optional.empty()
                ),
                // PREREQUISITE_FAILED reason returns empty
                Arguments.of(
                        "prereq-flag",
                        EvaluationDetail.fromValue("default", -1,
                                EvaluationReason.prerequisiteFailed("other-flag")),
                        Optional.empty()
                ),
                // RULE_MATCH reason returns the resolved value
                Arguments.of(
                        "rule-flag",
                        EvaluationDetail.fromValue("rule-value", 3,
                                EvaluationReason.ruleMatch(0, "rule-id")),
                        Optional.of("rule-value")
                )
        );
    }

    @ParameterizedTest(name = "getString({0}) with reason {1} returns {2}")
    @MethodSource("flagResolutionCases")
    void resolvesStringFlagBasedOnEvaluationReason(
            String key,
            EvaluationDetail<String> detail,
            Optional<String> expected) {

        LDClientInterface client = mock(LDClientInterface.class);
        when(client.stringVariationDetail(anyString(), any(LDContext.class), anyString()))
                .thenReturn(detail);

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);

        Optional<String> result = provider.getString(key);

        assertThat(result).isEqualTo(expected);
    }
}
