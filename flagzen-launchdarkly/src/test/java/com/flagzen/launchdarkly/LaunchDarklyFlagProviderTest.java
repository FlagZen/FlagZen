package com.flagzen.launchdarkly;

import com.flagzen.EvaluationContext;
import com.flagzen.spi.FlagProvider;
import com.launchdarkly.sdk.EvaluationDetail;
import com.launchdarkly.sdk.EvaluationReason;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link LaunchDarklyFlagProvider} through the {@link FlagProvider} port.
 *
 * <p>LDClientInterface is the infrastructure boundary (driven port), so it is mocked.
 * Each case exercises reason-kind-based absence detection per ADR-021.
 */
class LaunchDarklyFlagProviderTest {

    static Stream<Arguments> stringFlagResolutionCases() {
        return Stream.of(
                Arguments.of(
                        "fallthrough-flag",
                        EvaluationDetail.fromValue("enabled", 0, EvaluationReason.fallthrough()),
                        Optional.of("enabled")
                ),
                Arguments.of(
                        "target-flag",
                        EvaluationDetail.fromValue("targeted-value", 1, EvaluationReason.targetMatch()),
                        Optional.of("targeted-value")
                ),
                Arguments.of(
                        "off-flag",
                        EvaluationDetail.fromValue("off-value", 2, EvaluationReason.off()),
                        Optional.of("off-value")
                ),
                Arguments.of(
                        "error-flag",
                        EvaluationDetail.fromValue("default", -1,
                                EvaluationReason.error(EvaluationReason.ErrorKind.FLAG_NOT_FOUND)),
                        Optional.empty()
                ),
                Arguments.of(
                        "prereq-flag",
                        EvaluationDetail.fromValue("default", -1,
                                EvaluationReason.prerequisiteFailed("other-flag")),
                        Optional.empty()
                ),
                Arguments.of(
                        "rule-flag",
                        EvaluationDetail.fromValue("rule-value", 3,
                                EvaluationReason.ruleMatch(0, "rule-id")),
                        Optional.of("rule-value")
                )
        );
    }

    @ParameterizedTest(name = "getString({0}) with reason {1} returns {2}")
    @MethodSource("stringFlagResolutionCases")
    void resolvesStringFlagBasedOnEvaluationReason(
            String key,
            EvaluationDetail<String> detail,
            Optional<String> expected) {

        LDClientInterface client = mock(LDClientInterface.class);
        when(client.stringVariationDetail(anyString(), any(LDContext.class), anyString()))
                .thenReturn(detail);

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);

        assertThat(provider.getString(key)).isEqualTo(expected);
    }

    // -- Boolean resolution --

    static Stream<Arguments> booleanFlagResolutionCases() {
        return Stream.of(
                Arguments.of(
                        "bool-flag",
                        EvaluationDetail.fromValue(true, 0, EvaluationReason.fallthrough()),
                        Optional.of(true)
                ),
                Arguments.of(
                        "bool-off",
                        EvaluationDetail.fromValue(false, 1, EvaluationReason.off()),
                        Optional.of(false)
                ),
                Arguments.of(
                        "bool-error",
                        EvaluationDetail.fromValue(false, -1,
                                EvaluationReason.error(EvaluationReason.ErrorKind.FLAG_NOT_FOUND)),
                        Optional.empty()
                ),
                Arguments.of(
                        "bool-prereq",
                        EvaluationDetail.fromValue(false, -1,
                                EvaluationReason.prerequisiteFailed("other-flag")),
                        Optional.empty()
                )
        );
    }

    @ParameterizedTest(name = "getBoolean({0}) with reason {1} returns {2}")
    @MethodSource("booleanFlagResolutionCases")
    void resolvesBooleanFlagBasedOnEvaluationReason(
            String key,
            EvaluationDetail<Boolean> detail,
            Optional<Boolean> expected) {

        LDClientInterface client = mock(LDClientInterface.class);
        when(client.boolVariationDetail(anyString(), any(LDContext.class), anyBoolean()))
                .thenReturn(detail);

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);

        assertThat(provider.getBoolean(key)).isEqualTo(expected);
    }

    // -- Int resolution --

    static Stream<Arguments> intFlagResolutionCases() {
        return Stream.of(
                Arguments.of(
                        "int-flag",
                        EvaluationDetail.fromValue(42, 0, EvaluationReason.fallthrough()),
                        OptionalInt.of(42)
                ),
                Arguments.of(
                        "int-error",
                        EvaluationDetail.fromValue(0, -1,
                                EvaluationReason.error(EvaluationReason.ErrorKind.FLAG_NOT_FOUND)),
                        OptionalInt.empty()
                ),
                Arguments.of(
                        "int-prereq",
                        EvaluationDetail.fromValue(0, -1,
                                EvaluationReason.prerequisiteFailed("other-flag")),
                        OptionalInt.empty()
                )
        );
    }

    @ParameterizedTest(name = "getInt({0}) with reason {1} returns {2}")
    @MethodSource("intFlagResolutionCases")
    void resolvesIntFlagBasedOnEvaluationReason(
            String key,
            EvaluationDetail<Integer> detail,
            OptionalInt expected) {

        LDClientInterface client = mock(LDClientInterface.class);
        when(client.intVariationDetail(anyString(), any(LDContext.class), anyInt()))
                .thenReturn(detail);

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);

        assertThat(provider.getInt(key)).isEqualTo(expected);
    }

    // -- Double resolution --

    static Stream<Arguments> doubleFlagResolutionCases() {
        return Stream.of(
                Arguments.of(
                        "double-flag",
                        EvaluationDetail.fromValue(3.14, 0, EvaluationReason.fallthrough()),
                        OptionalDouble.of(3.14)
                ),
                Arguments.of(
                        "double-error",
                        EvaluationDetail.fromValue(0.0, -1,
                                EvaluationReason.error(EvaluationReason.ErrorKind.FLAG_NOT_FOUND)),
                        OptionalDouble.empty()
                ),
                Arguments.of(
                        "double-prereq",
                        EvaluationDetail.fromValue(0.0, -1,
                                EvaluationReason.prerequisiteFailed("other-flag")),
                        OptionalDouble.empty()
                )
        );
    }

    @ParameterizedTest(name = "getDouble({0}) with reason {1} returns {2}")
    @MethodSource("doubleFlagResolutionCases")
    void resolvesDoubleFlagBasedOnEvaluationReason(
            String key,
            EvaluationDetail<Double> detail,
            OptionalDouble expected) {

        LDClientInterface client = mock(LDClientInterface.class);
        when(client.doubleVariationDetail(anyString(), any(LDContext.class), anyDouble()))
                .thenReturn(detail);

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);

        assertThat(provider.getDouble(key)).isEqualTo(expected);
    }

    // -- Long resolution (via jsonValueVariationDetail) --

    static Stream<Arguments> longFlagResolutionCases() {
        return Stream.of(
                Arguments.of(
                        "long-flag",
                        EvaluationDetail.fromValue(LDValue.of(9_999_999_999L), 0, EvaluationReason.fallthrough()),
                        OptionalLong.of(9_999_999_999L)
                ),
                Arguments.of(
                        "long-error",
                        EvaluationDetail.fromValue(LDValue.ofNull(), -1,
                                EvaluationReason.error(EvaluationReason.ErrorKind.FLAG_NOT_FOUND)),
                        OptionalLong.empty()
                ),
                Arguments.of(
                        "long-prereq",
                        EvaluationDetail.fromValue(LDValue.ofNull(), -1,
                                EvaluationReason.prerequisiteFailed("other-flag")),
                        OptionalLong.empty()
                ),
                // Non-numeric JSON value returns empty even with valid reason
                Arguments.of(
                        "long-non-numeric",
                        EvaluationDetail.fromValue(LDValue.of("not-a-number"), 0, EvaluationReason.fallthrough()),
                        OptionalLong.empty()
                )
        );
    }

    @ParameterizedTest(name = "getLong({0}) with reason {1} returns {2}")
    @MethodSource("longFlagResolutionCases")
    void resolvesLongFlagBasedOnEvaluationReason(
            String key,
            EvaluationDetail<LDValue> detail,
            OptionalLong expected) {

        LDClientInterface client = mock(LDClientInterface.class);
        when(client.jsonValueVariationDetail(anyString(), any(LDContext.class), any(LDValue.class)))
                .thenReturn(detail);

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);

        assertThat(provider.getLong(key)).isEqualTo(expected);
    }

    // -- Context-aware resolution --

    @Test
    void resolvesStringFlagWithEvaluationContext() {
        LDClientInterface client = mock(LDClientInterface.class);
        when(client.stringVariationDetail(anyString(), any(LDContext.class), anyString()))
                .thenReturn(EvaluationDetail.fromValue("premium-value", 0, EvaluationReason.targetMatch()));

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-123")
                .attribute("plan", "premium")
                .build();

        Optional<String> result = provider.getString("feature-flag", context);

        assertThat(result).contains("premium-value");

        ArgumentCaptor<LDContext> ldContextCaptor = ArgumentCaptor.forClass(LDContext.class);
        verify(client).stringVariationDetail(anyString(), ldContextCaptor.capture(), anyString());
        LDContext capturedContext = ldContextCaptor.getValue();
        assertThat(capturedContext.getKey()).isEqualTo("user-123");
        assertThat(capturedContext.getValue("plan")).isEqualTo(LDValue.of("premium"));
    }

    static Stream<Arguments> contextAwareTypedResolutionCases() {
        return Stream.of(
                Arguments.of("boolean", true),
                Arguments.of("int", 42),
                Arguments.of("double", 3.14),
                Arguments.of("long", 9_999_999_999L)
        );
    }

    @ParameterizedTest(name = "context-aware {0} resolution passes context to LD client")
    @MethodSource("contextAwareTypedResolutionCases")
    void resolvesTypedFlagsWithEvaluationContext(String type, Object value) {
        LDClientInterface client = mock(LDClientInterface.class);
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-456")
                .build();

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);

        switch (type) {
            case "boolean" -> {
                when(client.boolVariationDetail(anyString(), any(LDContext.class), anyBoolean()))
                        .thenReturn(EvaluationDetail.fromValue((Boolean) value, 0, EvaluationReason.fallthrough()));
                assertThat(provider.getBoolean("flag", context)).contains((Boolean) value);
            }
            case "int" -> {
                when(client.intVariationDetail(anyString(), any(LDContext.class), anyInt()))
                        .thenReturn(EvaluationDetail.fromValue((Integer) value, 0, EvaluationReason.fallthrough()));
                assertThat(provider.getInt("flag", context)).hasValue((Integer) value);
            }
            case "double" -> {
                when(client.doubleVariationDetail(anyString(), any(LDContext.class), anyDouble()))
                        .thenReturn(EvaluationDetail.fromValue((Double) value, 0, EvaluationReason.fallthrough()));
                assertThat(provider.getDouble("flag", context)).hasValue((Double) value);
            }
            case "long" -> {
                when(client.jsonValueVariationDetail(anyString(), any(LDContext.class), any(LDValue.class)))
                        .thenReturn(EvaluationDetail.fromValue(LDValue.of((Long) value), 0, EvaluationReason.fallthrough()));
                assertThat(provider.getLong("flag", context)).hasValue((Long) value);
            }
        }
    }

    @Test
    void usesAnonymousContextWhenNoEvaluationContextProvided() {
        LDClientInterface client = mock(LDClientInterface.class);
        when(client.stringVariationDetail(anyString(), any(LDContext.class), anyString()))
                .thenReturn(EvaluationDetail.fromValue("value", 0, EvaluationReason.fallthrough()));

        FlagProvider provider = LaunchDarklyFlagProvider.create(client);
        provider.getString("flag");

        ArgumentCaptor<LDContext> ldContextCaptor = ArgumentCaptor.forClass(LDContext.class);
        verify(client).stringVariationDetail(anyString(), ldContextCaptor.capture(), anyString());
        LDContext capturedContext = ldContextCaptor.getValue();
        assertThat(capturedContext.isAnonymous()).isTrue();
        assertThat(capturedContext.getKey()).isEqualTo("anonymous");
    }
}
