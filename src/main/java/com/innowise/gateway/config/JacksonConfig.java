package com.innowise.gateway.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;

@Configuration
public class JacksonConfig {

    @Bean
    public Module pageModule() {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(PageImpl.class, PageImplMixin.class);
        return module;
    }

    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(as = PageImpl.class)
    private abstract static class PageImplMixin {
    }
}