// package com.communication.communication_backend.config;

// // In SecurityTestConfig.java
// import org.springframework.boot.test.context.TestConfiguration;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Primary;
// import org.springframework.context.annotation.Profile;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.web.SecurityFilterChain;

// @TestConfiguration
// @Profile("test")
// public class SecurityTestConfig {
//     @Bean
//     @Primary 
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         http
//             .authorizeRequests()
//                 .anyRequest().permitAll()
//             .and()
//             .sessionManagement()
//                 .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS);
//         return http.build();
//     }
// }
