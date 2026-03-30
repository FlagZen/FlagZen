package com.flagzen.launchdarkly;

import com.flagzen.EvaluationContext;
import com.launchdarkly.sdk.ContextBuilder;
import com.launchdarkly.sdk.ContextKind;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDValue;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Maps a FlagZen {@link EvaluationContext} to a LaunchDarkly {@link LDContext}.
 *
 * <p>Supported attribute types: {@link String}, {@link Boolean}, {@link Integer},
 * {@link Long}, {@link Double}. Unsupported types are skipped with a warning log.
 */
final class EvaluationContextMapper {

    private static final Logger LOGGER = Logger.getLogger(EvaluationContextMapper.class.getName());

    private EvaluationContextMapper() {
    }

    /**
     * Converts a FlagZen evaluation context to a LaunchDarkly context.
     *
     * <p>If the targeting key is {@code null}, the resulting context is anonymous
     * with key "anonymous".
     *
     * @param context the FlagZen evaluation context, must not be null
     * @return an {@link LDContext} with mapped targeting key and attributes
     */
    static LDContext toLDContext(EvaluationContext context) {
        String targetingKey = context.targetingKey();
        var builder = (targetingKey != null)
                ? LDContext.builder(ContextKind.DEFAULT, targetingKey)
                : LDContext.builder(ContextKind.DEFAULT, "anonymous").anonymous(true);

        for (Map.Entry<String, Object> entry : context.attributes().entrySet()) {
            addAttribute(builder, entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    private static void addAttribute(ContextBuilder builder, String key, Object value) {
        if (value instanceof String stringValue) {
            builder.set(key, LDValue.of(stringValue));
        } else if (value instanceof Boolean booleanValue) {
            builder.set(key, LDValue.of(booleanValue));
        } else if (value instanceof Integer intValue) {
            builder.set(key, LDValue.of(intValue));
        } else if (value instanceof Long longValue) {
            builder.set(key, LDValue.of(longValue));
        } else if (value instanceof Double doubleValue) {
            builder.set(key, LDValue.of(doubleValue));
        } else {
            LOGGER.warning("Unsupported attribute type for key '" + key
                    + "': " + value.getClass().getName() + ". Attribute skipped.");
        }
    }
}
