package com.flagzen.spring;

import com.flagzen.FeatureDispatcher;
import com.flagzen.spi.FeatureMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers {@link FeatureMetadata} via {@link ServiceLoader} and registers
 * each feature proxy as a Spring bean definition. The proxy bean resolves
 * the feature type through the {@link FeatureDispatcher} at runtime via
 * a {@link FeatureProxyFactoryBean}.
 */
public class FeatureProxyRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger log = LoggerFactory.getLogger(FeatureProxyRegistrar.class);

    /** Creates a new registrar instance. */
    public FeatureProxyRegistrar() { }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        @SuppressWarnings("rawtypes")
        ServiceLoader<FeatureMetadata> loader = ServiceLoader.load(FeatureMetadata.class);
        List<String> registeredFeatures = new ArrayList<>();
        for (FeatureMetadata<?> metadata : loader) {
            String beanName = registerProxy(registry, metadata);
            if (beanName != null) {
                registeredFeatures.add(metadata.featureType().getSimpleName());
                log.debug("Registered feature proxy bean '{}' for {} (flag key: {})",
                        beanName, metadata.featureType().getSimpleName(), metadata.flagKey());
            }
        }
        if (registeredFeatures.isEmpty()) {
            log.info("FlagZen: no feature metadata found on classpath; zero proxy beans registered");
        } else {
            log.info("FlagZen: registered {} feature proxy bean(s): {}",
                    registeredFeatures.size(), registeredFeatures);
        }
    }

    private String registerProxy(BeanDefinitionRegistry registry, FeatureMetadata<?> metadata) {
        Class<?> featureType = metadata.featureType();
        String beanName = deriveBeanName(featureType);

        if (registry.containsBeanDefinition(beanName)) {
            return null;
        }

        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(FeatureProxyFactoryBean.class);
        definition.setScope(BeanDefinition.SCOPE_SINGLETON);

        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addGenericArgumentValue(featureType);
        definition.setConstructorArgumentValues(args);

        definition.getPropertyValues().add("featureDispatcher",
                new RuntimeBeanReference("featureDispatcher"));

        registry.registerBeanDefinition(beanName, definition);
        return beanName;
    }

    private String deriveBeanName(Class<?> featureType) {
        String simpleName = featureType.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
