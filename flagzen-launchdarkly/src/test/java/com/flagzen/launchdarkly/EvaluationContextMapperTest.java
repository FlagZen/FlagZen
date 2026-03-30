package com.flagzen.launchdarkly;

import com.flagzen.EvaluationContext;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link EvaluationContextMapper} attribute type mapping and edge cases.
 *
 * <p>This is an adapter-boundary utility class that maps FlagZen contexts
 * to LaunchDarkly contexts. Tested directly because it is the mapping layer
 * at the infrastructure boundary.
 */
class EvaluationContextMapperTest {

    @Test
    void mapsTargetingKeyToLDContextKey() {
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-789")
                .build();

        LDContext ldContext = EvaluationContextMapper.toLDContext(context);

        assertThat(ldContext.getKey()).isEqualTo("user-789");
        assertThat(ldContext.isAnonymous()).isFalse();
    }

    @Test
    void mapsNullTargetingKeyToAnonymousContext() {
        EvaluationContext context = EvaluationContext.builder()
                .build();

        LDContext ldContext = EvaluationContextMapper.toLDContext(context);

        assertThat(ldContext.isAnonymous()).isTrue();
        assertThat(ldContext.getKey()).isEqualTo("anonymous");
    }

    static Stream<Arguments> supportedAttributeTypeCases() {
        return Stream.of(
                Arguments.of("stringAttr", "hello", LDValue.of("hello")),
                Arguments.of("boolAttr", true, LDValue.of(true)),
                Arguments.of("intAttr", 42, LDValue.of(42)),
                Arguments.of("longAttr", 9_999_999_999L, LDValue.of(9_999_999_999L)),
                Arguments.of("doubleAttr", 3.14, LDValue.of(3.14))
        );
    }

    @ParameterizedTest(name = "maps {0} ({1}) to LDValue")
    @MethodSource("supportedAttributeTypeCases")
    void mapsSupportedAttributeTypesToLDValue(String key, Object value, LDValue expectedLdValue) {
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute(key, value)
                .build();

        LDContext ldContext = EvaluationContextMapper.toLDContext(context);

        assertThat(ldContext.getValue(key)).isEqualTo(expectedLdValue);
    }

    @Test
    void skipsUnsupportedAttributeTypeWithWarning() {
        Logger logger = Logger.getLogger(EvaluationContextMapper.class.getName());
        TestLogHandler logHandler = new TestLogHandler();
        logger.addHandler(logHandler);
        logger.setLevel(Level.WARNING);

        try {
            EvaluationContext context = EvaluationContext.builder()
                    .targetingKey("user-1")
                    .attribute("validAttr", "ok")
                    .attribute("unsupportedAttr", new Object())
                    .build();

            LDContext ldContext = EvaluationContextMapper.toLDContext(context);

            assertThat(ldContext.getValue("validAttr")).isEqualTo(LDValue.of("ok"));
            assertThat(ldContext.getValue("unsupportedAttr")).isEqualTo(LDValue.ofNull());
            assertThat(logHandler.lastRecord).isNotNull();
            assertThat(logHandler.lastRecord.getMessage()).contains("unsupportedAttr");
            assertThat(logHandler.lastRecord.getLevel()).isEqualTo(Level.WARNING);
        } finally {
            logger.removeHandler(logHandler);
        }
    }

    @Test
    void mapsEmptyAttributesWithoutError() {
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-1")
                .build();

        LDContext ldContext = EvaluationContextMapper.toLDContext(context);

        assertThat(ldContext.getKey()).isEqualTo("user-1");
    }

    private static class TestLogHandler extends Handler {
        LogRecord lastRecord;

        @Override
        public void publish(LogRecord record) {
            lastRecord = record;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
