package com.checkers.config;

import com.checkers.variants.GameFactory;
import com.checkers.variants.VariantRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfiguration {

    @Bean
    public VariantRegistry variantRegistry() {
        return VariantRegistry.defaults();
    }

    @Bean
    public GameFactory gameFactory(VariantRegistry variantRegistry) {
        return new GameFactory(variantRegistry);
    }
}