package com.flagzen.spi;

import com.flagzen.EvaluationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FlagProvider default typed accessor methods (getBoolean, getInt, getLong, getDouble)
 * and their context-aware overloads.
 */
class FlagProviderTypedMethodsTest {

    private static FlagProvider providerReturning(String value) {
        return key -> Optional.ofNullable(value);
    }

    private static FlagProvider emptyProvider() {
        return key -> Optional.empty();
    }

    private static FlagProvider contextAwareProvider(String defaultValue, String contextValue, String targetKey) {
        return new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.ofNullable(defaultValue);
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                if (context != null && targetKey.equals(context.targetingKey())) {
                    return Optional.ofNullable(contextValue);
                }
                return getString(key);
            }
        };
    }

    // --- getBoolean ---

    @ParameterizedTest
    @CsvSource({"true,true", "false,false", "TRUE,true", "False,false"})
    void getBooleanParsesValidValues(String input, boolean expected) {
        assertThat(providerReturning(input).getBoolean("key")).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"maybe", "1", "yes", "0", ""})
    void getBooleanReturnsEmptyForInvalidValues(String input) {
        assertThat(providerReturning(input).getBoolean("key")).isEmpty();
    }

    @Test
    void getBooleanReturnsEmptyForAbsentFlag() {
        assertThat(emptyProvider().getBoolean("key")).isEmpty();
    }

    // --- getBoolean with context ---

    @Test
    void getBooleanWithContextDelegatesToGetStringWithContext() {
        FlagProvider provider = contextAwareProvider("false", "true", "user-1");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user-1").build();

        assertThat(provider.getBoolean("key", ctx)).contains(true);
    }

    @Test
    void getBooleanWithContextFallsBackWithoutMatch() {
        FlagProvider provider = contextAwareProvider("true", "false", "user-1");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("other").build();

        assertThat(provider.getBoolean("key", ctx)).contains(true);
    }

    @Test
    void getBooleanWithContextReturnsEmptyForNonBoolean() {
        FlagProvider provider = contextAwareProvider(null, "maybe", "user-1");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user-1").build();

        assertThat(provider.getBoolean("key", ctx)).isEmpty();
    }

    // --- getInt ---

    @Test
    void getIntParsesValidInteger() {
        assertThat(providerReturning("42").getInt("key")).hasValue(42);
    }

    @Test
    void getIntReturnsEmptyForNonNumeric() {
        assertThat(providerReturning("abc").getInt("key")).isEmpty();
    }

    @Test
    void getIntReturnsEmptyForAbsentFlag() {
        assertThat(emptyProvider().getInt("key")).isEmpty();
    }

    @Test
    void getIntReturnsEmptyForOverflow() {
        assertThat(providerReturning("99999999999").getInt("key")).isEmpty();
    }

    // --- getInt with context ---

    @Test
    void getIntWithContextDelegatesToGetStringWithContext() {
        FlagProvider provider = contextAwareProvider("10", "500", "premium");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("premium").build();

        assertThat(provider.getInt("key", ctx)).hasValue(500);
    }

    @Test
    void getIntWithContextFallsBackWithoutMatch() {
        FlagProvider provider = contextAwareProvider("10", "500", "premium");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("free").build();

        assertThat(provider.getInt("key", ctx)).hasValue(10);
    }

    @Test
    void getIntWithContextReturnsEmptyForNonNumeric() {
        FlagProvider provider = contextAwareProvider(null, "abc", "user");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user").build();

        assertThat(provider.getInt("key", ctx)).isEmpty();
    }

    // --- getLong ---

    @Test
    void getLongParsesValidLong() {
        assertThat(providerReturning("5000000000").getLong("key")).hasValue(5000000000L);
    }

    @Test
    void getLongReturnsEmptyForNonNumeric() {
        assertThat(providerReturning("unlimited").getLong("key")).isEmpty();
    }

    @Test
    void getLongReturnsEmptyForAbsentFlag() {
        assertThat(emptyProvider().getLong("key")).isEmpty();
    }

    @Test
    void getLongReturnsEmptyForOverflow() {
        assertThat(providerReturning("999999999999999999999").getLong("key")).isEmpty();
    }

    // --- getLong with context ---

    @Test
    void getLongWithContextDelegatesToGetStringWithContext() {
        FlagProvider provider = contextAwareProvider("1000", "10000000", "enterprise");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("enterprise").build();

        assertThat(provider.getLong("key", ctx)).hasValue(10000000L);
    }

    @Test
    void getLongWithContextFallsBackWithoutMatch() {
        FlagProvider provider = contextAwareProvider("1000", "10000000", "enterprise");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("free").build();

        assertThat(provider.getLong("key", ctx)).hasValue(1000L);
    }

    @Test
    void getLongWithContextReturnsEmptyForNonNumeric() {
        FlagProvider provider = contextAwareProvider(null, "nope", "user");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user").build();

        assertThat(provider.getLong("key", ctx)).isEmpty();
    }

    // --- getDouble ---

    @Test
    void getDoubleParsesValidDouble() {
        assertThat(providerReturning("0.75").getDouble("key")).hasValue(0.75);
    }

    @Test
    void getDoubleReturnsEmptyForNonNumeric() {
        assertThat(providerReturning("high").getDouble("key")).isEmpty();
    }

    @Test
    void getDoubleReturnsEmptyForAbsentFlag() {
        assertThat(emptyProvider().getDouble("key")).isEmpty();
    }

    // --- getDouble with context ---

    @Test
    void getDoubleWithContextDelegatesToGetStringWithContext() {
        FlagProvider provider = contextAwareProvider("0.1", "1.0", "debug");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("debug").build();

        assertThat(provider.getDouble("key", ctx)).hasValue(1.0);
    }

    @Test
    void getDoubleWithContextFallsBackWithoutMatch() {
        FlagProvider provider = contextAwareProvider("0.1", "1.0", "debug");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("prod").build();

        assertThat(provider.getDouble("key", ctx)).hasValue(0.1);
    }

    @Test
    void getDoubleWithContextReturnsEmptyForNonNumeric() {
        FlagProvider provider = contextAwareProvider(null, "nope", "user");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user").build();

        assertThat(provider.getDouble("key", ctx)).isEmpty();
    }

    // --- getString with context default ---

    @Test
    void getStringWithContextDefaultsDelegatesToGetString() {
        FlagProvider provider = providerReturning("value");
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("any").build();

        assertThat(provider.getString("key", ctx)).contains("value");
    }
}
