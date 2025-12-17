package com.baccosoft.baccosoft.Configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RoleValidationInterceptor roleValidationInterceptor;

    public WebConfig(RoleValidationInterceptor roleValidationInterceptor) {
        this.roleValidationInterceptor = roleValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleValidationInterceptor)
                .addPathPatterns("/api/**");
    }
}
