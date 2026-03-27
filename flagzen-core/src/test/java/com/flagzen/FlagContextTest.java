package com.flagzen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for FlagContext block-scoped run methods.
 * Test Budget: 4 behaviors x 2 = 8 max. Using 4.
 *
 * Tests through FlagContext public API (driving port for context scoping).
 */
class FlagContextTest {

    @AfterEach
    void cleanup() {
        FlagContext.clear();
    }

    /**
     * Behavior 1: run(Runnable) makes context available inside block via current().
     */
    @Test
    void runnableBlockSeesContextViaCurrent() {
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-42")
                .build();

        AtomicReference<EvaluationContext> captured = new AtomicReference<>();

        FlagContext.run(context, () -> captured.set(FlagContext.current()));

        assertThat(captured.get()).isSameAs(context);
    }

    /**
     * Behavior 2: Context is cleared after run() completes (both Runnable and Supplier).
     */
    @Test
    void contextClearedAfterBlockCompletes() {
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-42")
                .build();

        FlagContext.run(context, () -> { /* no-op */ });
        assertThat(FlagContext.current()).isNull();

        String result = FlagContext.run(context, () -> "done");
        assertThat(result).isEqualTo("done");
        assertThat(FlagContext.current()).isNull();
    }

    /**
     * Behavior 3: Context is cleared even if block throws an exception.
     */
    @Test
    void contextClearedWhenBlockThrowsException() {
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-42")
                .build();

        assertThatThrownBy(() ->
                FlagContext.run(context, () -> { throw new RuntimeException("boom"); })
        ).isInstanceOf(RuntimeException.class).hasMessage("boom");

        assertThat(FlagContext.current()).isNull();
    }

    /**
     * Behavior 4: Nested run() overrides context; outer context restored after inner exits.
     */
    @Test
    void clearRemovesCurrentContext() {
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-42")
                .build();

        FlagContext.set(context);
        assertThat(FlagContext.current()).isSameAs(context);

        FlagContext.clear();
        assertThat(FlagContext.current()).isNull();
    }

    @Test
    void supplierRunRestoresPreviousContextWhenNested() {
        EvaluationContext outer = EvaluationContext.builder()
                .targetingKey("outer")
                .build();
        EvaluationContext inner = EvaluationContext.builder()
                .targetingKey("inner")
                .build();

        AtomicReference<String> innerResult = new AtomicReference<>();

        FlagContext.run(outer, () -> {
            String result = FlagContext.run(inner, () -> {
                assertThat(FlagContext.current()).isSameAs(inner);
                return "from-inner";
            });
            innerResult.set(result);
            assertThat(FlagContext.current()).isSameAs(outer);
        });

        assertThat(innerResult.get()).isEqualTo("from-inner");
        assertThat(FlagContext.current()).isNull();
    }

    @Test
    void nestedRunRestoresOuterContext() {
        EvaluationContext outer = EvaluationContext.builder()
                .targetingKey("outer-user")
                .build();
        EvaluationContext inner = EvaluationContext.builder()
                .targetingKey("inner-user")
                .build();

        AtomicReference<EvaluationContext> innerSeen = new AtomicReference<>();
        AtomicReference<EvaluationContext> outerAfterInner = new AtomicReference<>();

        FlagContext.run(outer, () -> {
            FlagContext.run(inner, () -> innerSeen.set(FlagContext.current()));
            outerAfterInner.set(FlagContext.current());
        });

        assertThat(innerSeen.get()).isSameAs(inner);
        assertThat(outerAfterInner.get()).isSameAs(outer);
        assertThat(FlagContext.current()).isNull();
    }
}
