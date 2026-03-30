package com.flagzen.launchdarkly;

import com.flagzen.spi.FlagProvider;
import com.launchdarkly.sdk.ContextKind;
import com.launchdarkly.sdk.EvaluationDetail;
import com.launchdarkly.sdk.EvaluationReason;
import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;

import java.util.Optional;

/**
 * A {@link FlagProvider} that delegates flag resolution to the LaunchDarkly Java Server SDK.
 *
 * <p>This adapter bridges FlagZen's typed flag resolution with LaunchDarkly's feature flag
 * service. Flag values are resolved using {@code stringVariationDetail} to access evaluation
 * reasons for absence detection.
 *
 * <p>Absence detection is reason-kind-based (ADR-021):
 * <ul>
 *   <li>{@code ERROR} — flag not found, malformed, or wrong type → empty</li>
 *   <li>{@code PREREQUISITE_FAILED} — prerequisite flag not satisfied → empty</li>
 *   <li>{@code OFF} — flag is off, returns the configured off-variation value (not empty)</li>
 *   <li>{@code FALLTHROUGH}, {@code TARGET_MATCH}, {@code RULE_MATCH} — resolved value</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * LDClient ldClient = new LDClient("sdk-key");
 * FlagProvider provider = LaunchDarklyFlagProvider.create(ldClient);
 * Optional<String> value = provider.getString("my-flag");
 * }</pre>
 */
public final class LaunchDarklyFlagProvider implements FlagProvider {

    private static final LDContext ANONYMOUS_CONTEXT = LDContext.builder(ContextKind.DEFAULT, "anonymous")
            .anonymous(true)
            .build();

    private final LDClientInterface client;

    private LaunchDarklyFlagProvider(LDClientInterface client) {
        this.client = client;
    }

    /**
     * Creates a provider using the given LaunchDarkly client.
     *
     * @param client the LaunchDarkly client to delegate to
     * @return a new {@link LaunchDarklyFlagProvider}
     */
    public static LaunchDarklyFlagProvider create(LDClientInterface client) {
        return new LaunchDarklyFlagProvider(client);
    }

    @Override
    public Optional<String> getString(String key) {
        EvaluationDetail<String> detail = client.stringVariationDetail(key, ANONYMOUS_CONTEXT, "");
        return isAbsent(detail) ? Optional.empty() : Optional.of(detail.getValue());
    }

    private boolean isAbsent(EvaluationDetail<?> detail) {
        EvaluationReason.Kind kind = detail.getReason().getKind();
        return kind == EvaluationReason.Kind.ERROR
                || kind == EvaluationReason.Kind.PREREQUISITE_FAILED;
    }
}
