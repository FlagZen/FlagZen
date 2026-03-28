package com.flagzen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EvaluationContext equals, hashCode, and toString.
 */
class EvaluationContextTest {

    @Test
    void equalsReturnsTrueForIdenticalContexts() {
        EvaluationContext a = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute("plan", "enterprise")
                .build();
        EvaluationContext b = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute("plan", "enterprise")
                .build();

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsReturnsTrueForSameInstance() {
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user-1").build();

        assertThat(ctx).isEqualTo(ctx);
    }

    @Test
    void equalsReturnsFalseForDifferentTargetingKey() {
        EvaluationContext a = EvaluationContext.builder().targetingKey("user-1").build();
        EvaluationContext b = EvaluationContext.builder().targetingKey("user-2").build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsReturnsFalseForDifferentAttributes() {
        EvaluationContext a = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute("plan", "free")
                .build();
        EvaluationContext b = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute("plan", "enterprise")
                .build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsReturnsFalseForNull() {
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user-1").build();

        assertThat(ctx).isNotEqualTo(null);
    }

    @Test
    void equalsReturnsFalseForDifferentType() {
        EvaluationContext ctx = EvaluationContext.builder().targetingKey("user-1").build();

        assertThat(ctx).isNotEqualTo("not a context");
    }

    @Test
    void equalsHandlesNullTargetingKey() {
        EvaluationContext a = EvaluationContext.builder().attribute("plan", "free").build();
        EvaluationContext b = EvaluationContext.builder().attribute("plan", "free").build();
        EvaluationContext c = EvaluationContext.builder().targetingKey("user-1").attribute("plan", "free").build();

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void hashCodeIsConsistentForEqualContexts() {
        EvaluationContext a = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute("plan", "enterprise")
                .build();
        EvaluationContext b = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute("plan", "enterprise")
                .build();

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void hashCodeDiffersForDifferentContexts() {
        EvaluationContext a = EvaluationContext.builder().targetingKey("user-1").build();
        EvaluationContext b = EvaluationContext.builder().targetingKey("user-2").build();

        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    void toStringIncludesTargetingKeyAndAttributes() {
        EvaluationContext ctx = EvaluationContext.builder()
                .targetingKey("user-1")
                .attribute("plan", "enterprise")
                .build();

        assertThat(ctx.toString())
                .contains("user-1")
                .contains("plan")
                .contains("enterprise");
    }
}
