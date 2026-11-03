package com.example.appcenter_project.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Value("${app.urls.production}")
    private String productionUrl;

    @Value("${app.urls.development}")
    private String developmentUrl;

    @Value("${app.urls.frontend-dev-url}")
    private String frontDevUrl;

    @Value("${app.urls.frontend-main-url}")
    private String frontMainUrl;

    @Value("${app.urls.ai-url}")
    private String aiUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 자격 증명을 포함한 요청을 위해 특정 오리진 설정
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("https://localhost:*");
        configuration.addAllowedOrigin(productionUrl);
        configuration.addAllowedOrigin(developmentUrl);
        configuration.addAllowedOriginPattern(frontDevUrl);
        configuration.addAllowedOrigin(frontMainUrl);
        configuration.addAllowedOrigin(aiUrl);

        // 모든 헤더와 메서드 허용
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        // 프리플라이트 요청 캐시 시간 설정 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
