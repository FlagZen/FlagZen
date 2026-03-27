package com.flagzen;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable evaluation context for targeted flag resolution.
 * Contains a targeting key and arbitrary attributes that flag providers
 * use to resolve flags differently per user or segment.
 *
 * <p>Create instances via the builder:
 * <pre>{@code
 * EvaluationContext context = EvaluationContext.builder()
 *     .targetingKey("user-7291")
 *     .attribute("plan", "enterprise")
 *     .build();
 * }</pre>
 */
public final class EvaluationContext {

    private final String targetingKey;
    private final Map<String, Object> attributes;

    private EvaluationContext(String targetingKey, Map<String, Object> attributes) {
        this.targetingKey = targetingKey;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    /**
     * Returns the targeting key, or {@code null} if not set.
     *
     * @return the targeting key
     */
    public String targetingKey() {
        return targetingKey;
    }

    /**
     * Returns an unmodifiable view of the attributes.
     *
     * @return the attributes map, never null
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    /**
     * Creates a new builder for {@link EvaluationContext}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EvaluationContext other)) return false;
        return Objects.equals(targetingKey, other.targetingKey)
                && Objects.equals(attributes, other.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetingKey, attributes);
    }

    @Override
    public String toString() {
        return "EvaluationContext[targetingKey=" + targetingKey + ", attributes=" + attributes + "]";
    }

    /**
     * Builder for {@link EvaluationContext}.
     */
    public static final class Builder {

        private String targetingKey;
        private final Map<String, Object> attributes = new HashMap<>();

        Builder() {
        }

        /**
         * Sets the targeting key.
         *
         * @param targetingKey the targeting key
         * @return this builder
         */
        public Builder targetingKey(String targetingKey) {
            this.targetingKey = targetingKey;
            return this;
        }

        /**
         * Adds an attribute.
         *
         * @param key the attribute key
         * @param value the attribute value
         * @return this builder
         */
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        /**
         * Builds the evaluation context.
         *
         * @return a new immutable {@link EvaluationContext}
         */
        public EvaluationContext build() {
            return new EvaluationContext(targetingKey, attributes);
        }
    }
}
