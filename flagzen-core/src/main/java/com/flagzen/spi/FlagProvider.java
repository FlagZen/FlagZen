package com.flagzen.spi;

import com.flagzen.EvaluationContext;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * SPI for providing flag values at runtime.
 * Implementations supply the current value for a given flag key.
 */
public interface FlagProvider {

    /**
     * Returns the current value for the given flag key.
     *
     * @param key the flag key to look up
     * @return the flag value, or empty if not set
     */
    Optional<String> getString(String key);

    /**
     * Returns the current value for the given flag key using the provided evaluation context.
     * The default implementation ignores the context and delegates to {@link #getString(String)},
     * ensuring backward compatibility for existing providers.
     *
     * @param key the flag key to look up
     * @param context the evaluation context for targeted resolution
     * @return the flag value, or empty if not set
     */
    default Optional<String> getString(String key, EvaluationContext context) {
        return getString(key);
    }

    /**
     * Returns the current boolean value for the given flag key.
     * The default implementation parses the string value from {@link #getString(String)}.
     * Only exact "true" or "false" (case-insensitive) are recognized; anything else returns empty.
     *
     * @param key the flag key to look up
     * @return the boolean flag value, or empty if not set or not parseable as boolean
     */
    default Optional<Boolean> getBoolean(String key) {
        return getString(key)
                .filter(v -> v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false"))
                .map(Boolean::parseBoolean);
    }

    /**
     * Returns the current boolean value for the given flag key using the provided evaluation context.
     * The default implementation delegates to {@link #getBoolean(String)}.
     *
     * @param key the flag key to look up
     * @param context the evaluation context for targeted resolution
     * @return the boolean flag value, or empty if not set or not parseable as boolean
     */
    default Optional<Boolean> getBoolean(String key, EvaluationContext context) {
        return getBoolean(key);
    }

    /**
     * Returns the current integer value for the given flag key.
     * The default implementation parses the string value from {@link #getString(String)}.
     * Returns empty if the key is not set or the value is not a valid integer.
     *
     * @param key the flag key to look up
     * @return the integer flag value, or empty if not set or not parseable
     */
    default OptionalInt getInt(String key) {
        try {
            return getString(key)
                    .map(Integer::parseInt)
                    .map(OptionalInt::of)
                    .orElse(OptionalInt.empty());
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * Returns the current integer value for the given flag key using the provided evaluation context.
     * The default implementation delegates to {@link #getInt(String)}.
     *
     * @param key the flag key to look up
     * @param context the evaluation context for targeted resolution
     * @return the integer flag value, or empty if not set or not parseable
     */
    default OptionalInt getInt(String key, EvaluationContext context) {
        return getInt(key);
    }

    /**
     * Returns the current long value for the given flag key.
     * The default implementation parses the string value from {@link #getString(String)}.
     * Returns empty if the key is not set or the value is not a valid long.
     *
     * @param key the flag key to look up
     * @return the long flag value, or empty if not set or not parseable
     */
    default OptionalLong getLong(String key) {
        try {
            return getString(key)
                    .map(Long::parseLong)
                    .map(OptionalLong::of)
                    .orElse(OptionalLong.empty());
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    /**
     * Returns the current long value for the given flag key using the provided evaluation context.
     * The default implementation delegates to {@link #getLong(String)}.
     *
     * @param key the flag key to look up
     * @param context the evaluation context for targeted resolution
     * @return the long flag value, or empty if not set or not parseable
     */
    default OptionalLong getLong(String key, EvaluationContext context) {
        return getLong(key);
    }

    /**
     * Returns the current double value for the given flag key.
     * The default implementation parses the string value from {@link #getString(String)}.
     * Returns empty if the key is not set or the value is not a valid double.
     *
     * @param key the flag key to look up
     * @return the double flag value, or empty if not set or not parseable
     */
    default OptionalDouble getDouble(String key) {
        try {
            return getString(key)
                    .map(Double::parseDouble)
                    .map(OptionalDouble::of)
                    .orElse(OptionalDouble.empty());
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Returns the current double value for the given flag key using the provided evaluation context.
     * The default implementation delegates to {@link #getDouble(String)}.
     *
     * @param key the flag key to look up
     * @param context the evaluation context for targeted resolution
     * @return the double flag value, or empty if not set or not parseable
     */
    default OptionalDouble getDouble(String key, EvaluationContext context) {
        return getDouble(key);
    }
}
