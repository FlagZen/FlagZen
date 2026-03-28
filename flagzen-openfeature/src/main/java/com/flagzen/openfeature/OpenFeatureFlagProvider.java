package com.flagzen.openfeature;

import com.flagzen.EvaluationContext;
import com.flagzen.spi.FlagProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.OpenFeatureAPI;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A {@link FlagProvider} that delegates flag resolution to the OpenFeature SDK.
 *
 * <p>This adapter bridges FlagZen's typed flag resolution with any flag management
 * service that has an OpenFeature provider. Flag values are resolved natively through
 * the OpenFeature Client API without string round-tripping.
 *
 * <p>Absence detection is reason-based: if the OpenFeature evaluation returns an error
 * code or the reason is {@code "DEFAULT"} (indicating the flag was not found), the
 * adapter returns an empty result.
 *
 * <p>Usage with the default OpenFeature client:
 * <pre>{@code
 * FlagProvider provider = OpenFeatureFlagProvider.create();
 * Optional<String> value = provider.getString("my-flag");
 * }</pre>
 *
 * <p>Usage with a named OpenFeature client:
 * <pre>{@code
 * Client client = OpenFeatureAPI.getInstance().getClient("payments");
 * FlagProvider provider = OpenFeatureFlagProvider.create(client);
 * }</pre>
 */
public final class OpenFeatureFlagProvider implements FlagProvider {

    private static final String DEFAULT_REASON = "DEFAULT";

    private final Client client;

    private OpenFeatureFlagProvider(Client client) {
        this.client = client;
    }

    /**
     * Creates an adapter using the default OpenFeature client.
     *
     * @return a new {@link OpenFeatureFlagProvider}
     */
    public static OpenFeatureFlagProvider create() {
        return new OpenFeatureFlagProvider(OpenFeatureAPI.getInstance().getClient());
    }

    /**
     * Creates an adapter using the given OpenFeature client.
     *
     * @param client the OpenFeature client to delegate to
     * @return a new {@link OpenFeatureFlagProvider}
     */
    public static OpenFeatureFlagProvider create(Client client) {
        return new OpenFeatureFlagProvider(client);
    }

    @Override
    public Optional<String> getString(String key) {
        var details = client.getStringDetails(key, "");
        return extractIfPresent(details);
    }

    @Override
    public Optional<String> getString(String key, EvaluationContext context) {
        var ofContext = EvaluationContextMapper.toOpenFeatureContext(context);
        var details = client.getStringDetails(key, "", ofContext);
        return extractIfPresent(details);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        var details = client.getBooleanDetails(key, false);
        return extractIfPresent(details);
    }

    @Override
    public Optional<Boolean> getBoolean(String key, EvaluationContext context) {
        var ofContext = EvaluationContextMapper.toOpenFeatureContext(context);
        var details = client.getBooleanDetails(key, false, ofContext);
        return extractIfPresent(details);
    }

    @Override
    public OptionalInt getInt(String key) {
        var details = client.getIntegerDetails(key, 0);
        return isAbsent(details) ? OptionalInt.empty() : OptionalInt.of(details.getValue());
    }

    @Override
    public OptionalInt getInt(String key, EvaluationContext context) {
        var ofContext = EvaluationContextMapper.toOpenFeatureContext(context);
        var details = client.getIntegerDetails(key, 0, ofContext);
        return isAbsent(details) ? OptionalInt.empty() : OptionalInt.of(details.getValue());
    }

    @Override
    public OptionalLong getLong(String key) {
        var details = client.getIntegerDetails(key, 0);
        return isAbsent(details) ? OptionalLong.empty() : OptionalLong.of(details.getValue().longValue());
    }

    @Override
    public OptionalLong getLong(String key, EvaluationContext context) {
        var ofContext = EvaluationContextMapper.toOpenFeatureContext(context);
        var details = client.getIntegerDetails(key, 0, ofContext);
        return isAbsent(details) ? OptionalLong.empty() : OptionalLong.of(details.getValue().longValue());
    }

    @Override
    public OptionalDouble getDouble(String key) {
        var details = client.getDoubleDetails(key, 0.0);
        return isAbsent(details) ? OptionalDouble.empty() : OptionalDouble.of(details.getValue());
    }

    @Override
    public OptionalDouble getDouble(String key, EvaluationContext context) {
        var ofContext = EvaluationContextMapper.toOpenFeatureContext(context);
        var details = client.getDoubleDetails(key, 0.0, ofContext);
        return isAbsent(details) ? OptionalDouble.empty() : OptionalDouble.of(details.getValue());
    }

    private <T> Optional<T> extractIfPresent(FlagEvaluationDetails<T> details) {
        return isAbsent(details) ? Optional.empty() : Optional.of(details.getValue());
    }

    private <T> boolean isAbsent(FlagEvaluationDetails<T> details) {
        return details.getErrorCode() != null || DEFAULT_REASON.equals(details.getReason());
    }
}
