package com.wlh.smartbi.config;


import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.wlh.smartbi.aop.ReFreshTokenInterceptor;
import com.wlh.smartbi.service.JwtTokensService;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @author wlh
 * @className MvcConfig
 * @date : 2023/01/07/ 14:55
 **/
@Configuration
@EnableKnife4j
public class MvcConfig implements WebMvcConfigurer {


    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    JwtTokensService jwtTokensService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注意不要拦截knife4j的接口文档
        registry.addInterceptor(new ReFreshTokenInterceptor(stringRedisTemplate,jwtTokensService)).addPathPatterns("/**")
                .excludePathPatterns(
                        //排除所有的接口
//                        "/**/user/{id}",
//                        "/**/user/list",
//                        "/**/chart/list/chart/all",
//                        "/**/update/role",
                        "/**/login/**",
                        "/**/send/code",
                        "/**/register/**",
                        "/**/doc.html/**",
                        "/static/**",
                        "/**/swagger-ui.html/**",
                        "/**/error",
                        "/**/test/**",
                        "/**/favicon.ico",
                        "/**/swagger-resources/**",
                        "/**/webjars/**"
                );
        WebMvcConfigurer.super.addInterceptors(registry);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

}
