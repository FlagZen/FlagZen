package com.flagzen.openfeature;

import com.flagzen.EvaluationContext;
import dev.openfeature.sdk.MutableContext;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Maps a FlagZen {@link EvaluationContext} to an OpenFeature {@link MutableContext}.
 *
 * <p>Supported attribute types: {@link String}, {@link Boolean}, {@link Integer},
 * {@link Long}, {@link Double}. Unsupported types are skipped with a warning log.
 */
final class EvaluationContextMapper {

    private static final Logger LOGGER = Logger.getLogger(EvaluationContextMapper.class.getName());

    private EvaluationContextMapper() {
    }

    /**
     * Converts a FlagZen evaluation context to an OpenFeature mutable context.
     *
     * @param context the FlagZen evaluation context, must not be null
     * @return an OpenFeature {@link MutableContext} with mapped targeting key and attributes
     */
    static MutableContext toOpenFeatureContext(EvaluationContext context) {
        var ofContext = new MutableContext();

        if (context.targetingKey() != null) {
            ofContext.setTargetingKey(context.targetingKey());
        }

        for (Map.Entry<String, Object> entry : context.attributes().entrySet()) {
            addAttribute(ofContext, entry.getKey(), entry.getValue());
        }

        return ofContext;
    }

    private static void addAttribute(MutableContext context, String key, Object value) {
        if (value instanceof String stringValue) {
            context.add(key, stringValue);
        } else if (value instanceof Boolean booleanValue) {
            context.add(key, booleanValue);
        } else if (value instanceof Integer intValue) {
            context.add(key, intValue);
        } else if (value instanceof Long longValue) {
            context.add(key, longValue.intValue());
        } else if (value instanceof Double doubleValue) {
            context.add(key, doubleValue);
        } else {
            LOGGER.warning("Unsupported attribute type for key '" + key
                    + "': " + value.getClass().getName() + ". Attribute skipped.");
        }
    }
}
