package com.easygql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * @author ：fenyorome
 * @date ：Created in 2019/3/6/006 14:01
 * @description：${description}
 * @modified By：
 * @version: $version$
 */
@Configuration
public class CustomWebFluxConfig implements WebFluxConfigurer {

    /**
     * 全局跨域配置，根据各自需求定义
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOrigins("*")
                .allowedHeaders("*")
                .allowedMethods("*")
                .maxAge(3600)
                .exposedHeaders(HttpHeaders.SET_COOKIE);
    }

    /**
     * 也可以继承CorsWebFilter使用@Component注解，效果是一样的
     * @return
     */
    @Bean
    CorsWebFilter corsWebFilter(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(false);
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setMaxAge(8000L);
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addExposedHeader(HttpHeaders.SET_COOKIE);
        CorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        ((UrlBasedCorsConfigurationSource) corsConfigurationSource).registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(corsConfigurationSource);
    }
}
