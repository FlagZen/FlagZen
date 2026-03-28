package com.flagzen.spring;

import com.flagzen.FeatureDispatcher;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.spi.FlagProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration for FlagZen.
 * Creates a {@link FeatureDispatcher} bean from the available {@link FlagProvider}
 * and registers all discovered feature proxies as Spring beans.
 */
@AutoConfiguration
@ConditionalOnClass(FeatureDispatcher.class)
@Import(FeatureProxyRegistrar.class)
public class FlagZenAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FlagZenAutoConfiguration.class);

    /** Creates a new auto-configuration instance. */
    public FlagZenAutoConfiguration() { }

    /**
     * Creates a {@link FeatureDispatcher} from the available {@link FlagProvider}.
     * If no {@code FlagProvider} bean is defined, uses an {@link InMemoryFlagProvider}
     * that always returns empty, allowing default variants to activate.
     * Backs off if the user defines their own {@code FeatureDispatcher} bean.
     *
     * @param flagProvider the flag provider, or null if none defined
     * @return the auto-configured feature dispatcher
     */
    @Bean
    @ConditionalOnMissingBean
    public FeatureDispatcher featureDispatcher(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            org.springframework.beans.factory.ObjectProvider<FlagProvider> flagProvider) {
        FlagProvider provider = flagProvider.getIfAvailable(() -> {
            log.warn("No FlagProvider bean found; activating InMemoryFlagProvider fallback (dev/test only)");
            return new InMemoryFlagProvider();
        });
        return new DefaultFeatureDispatcher(provider);
    }
}
