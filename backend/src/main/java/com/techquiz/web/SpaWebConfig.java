package com.techquiz.web;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the Angular build from {@code classpath:/static/} (ADR 0003). A path that isn't a file
 * gets {@code index.html}, so a browser refresh on {@code /quiz} lands in the SPA's router. API
 * and actuator paths are never rewritten, so an unknown endpoint stays a 404.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    static class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/") || resourcePath.contains(".")) {
                return null;
            }
            Resource index = new ClassPathResource("/static/index.html");
            return index.exists() ? index : null;
        }
    }
}
