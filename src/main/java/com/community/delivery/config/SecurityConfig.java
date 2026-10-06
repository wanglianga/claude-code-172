package com.community.delivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置：四类端侧角色，HTTP Basic 鉴权（测试账号见 README）。
 * property 物业 / operator 运营 / support 客服 / robot 机器人。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("property").password(encoder.encode("property123"))
                        .roles("PROPERTY").build(),
                User.withUsername("operator").password(encoder.encode("operator123"))
                        .roles("OPERATOR").build(),
                User.withUsername("support").password(encoder.encode("support123"))
                        .roles("SUPPORT").build(),
                User.withUsername("robot").password(encoder.encode("robot123"))
                        .roles("ROBOT").build()
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                "/swagger-resources/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/property/**").hasRole("PROPERTY")
                        .requestMatchers("/api/operator/**").hasRole("OPERATOR")
                        .requestMatchers("/api/support/**").hasRole("SUPPORT")
                        .requestMatchers("/api/robot/**").hasRole("ROBOT")
                        // 订单事件流对所有登录角色开放（物业/运营/客服都需要核查）
                        .requestMatchers("/api/orders/**", "/api/orders").authenticated()
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> {
                });
        return http.build();
    }
}
