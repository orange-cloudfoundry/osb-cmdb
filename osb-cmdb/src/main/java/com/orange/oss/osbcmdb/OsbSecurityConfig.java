package com.orange.oss.osbcmdb;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class OsbSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                    .antMatchers("/v2/**").authenticated()
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
                    .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                .and()
                .httpBasic();
    }
}
