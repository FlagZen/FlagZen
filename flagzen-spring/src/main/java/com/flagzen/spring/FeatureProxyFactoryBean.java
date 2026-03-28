package com.flagzen.spring;

import com.flagzen.FeatureDispatcher;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring {@link FactoryBean} that produces a feature proxy by resolving
 * the feature type through the {@link FeatureDispatcher}.
 *
 * @param <T> the feature interface type
 */
class FeatureProxyFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> featureType;
    private FeatureDispatcher featureDispatcher;

    FeatureProxyFactoryBean(Class<T> featureType) {
        this.featureType = featureType;
    }

    /**
     * Sets the feature dispatcher used to resolve the proxy.
     *
     * @param featureDispatcher the dispatcher
     */
    public void setFeatureDispatcher(FeatureDispatcher featureDispatcher) {
        this.featureDispatcher = featureDispatcher;
    }

    @Override
    public T getObject() {
        return featureDispatcher.resolve(featureType);
    }

    @Override
    public Class<?> getObjectType() {
        return featureType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
