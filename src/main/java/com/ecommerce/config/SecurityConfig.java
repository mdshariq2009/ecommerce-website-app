package com.ecommerce.config;

import com.ecommerce.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final AuthenticationProvider authenticationProvider;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Autowired
    public SecurityConfig(AuthenticationProvider authenticationProvider,
                         JwtService jwtService,
                         UserDetailsService userDetailsService) {
        this.authenticationProvider = authenticationProvider;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }
    
 /*   @Bean //H2 DB Config
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public web pages and static resources
                .requestMatchers("/", "/web/**", "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                
                // H2 Console (development only)
                .requestMatchers("/h2-console/**").permitAll()
                
                // Public API endpoints (no authentication required)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/contact/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/shipping").permitAll()
                .requestMatchers("/api/payment/stripe-publishable-key").permitAll()
                
                // Authenticated API endpoints (user must be logged in)
                .requestMatchers("/api/payment/**").authenticated()
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/profile/**").authenticated()
                
                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
            );*/
    
    @Bean 
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public web pages and static resources
                .requestMatchers("/", "/web/**", "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                
                // PostGred Console (development only)
                .requestMatchers("/api/auth/**").permitAll()
                
                // Public API endpoints (no authentication required)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/contact/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/shipping").permitAll()
                .requestMatchers("/api/payment/stripe-publishable-key").permitAll()
                
                // Authenticated API endpoints (user must be logged in)
                .requestMatchers("/api/payment/**").authenticated()
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/profile/**").authenticated()
                
                // Admin only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authenticationProvider(authenticationProvider)
                    .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
            
        
        return http.build();
    }
}