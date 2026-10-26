package org.example.backend.config;

import org.example.backend.model.Role;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = Role.ROLE_ADMIN.name();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorHandler securityErrorHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, SecurityErrorHandler securityErrorHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)   // 401 sin token válido
                        .accessDeniedHandler(securityErrorHandler)        // 403 sin permiso
                )
                // El orden importa: gana la primera regla que coincide
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/register").permitAll()
                        .requestMatchers("/api/health", "/error").permitAll()

                        // Copiar una plantilla (incrementa su uso) está permitido a cualquier usuario autenticado
                        .requestMatchers(HttpMethod.POST, "/api/templates/*/increment-usage").authenticated()

                        // Escritura del catálogo e imágenes: solo administradores
                        .requestMatchers(HttpMethod.POST, "/api/templates", "/api/templates/**",
                                "/api/brands", "/api/brands/**", "/api/categories", "/api/categories/**",
                                "/api/images/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/templates/**", "/api/brands/**", "/api/categories/**")
                                .hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/templates/**", "/api/brands/**", "/api/categories/**")
                                .hasAuthority(ADMIN)

                        // Perfil propio para cualquier autenticado; datos de otros usuarios solo para admin
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers("/api/users/**").hasAuthority(ADMIN)

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Evita que Spring Boot registre el filtro JWT también como filtro de servlet global:
    // solo debe ejecutarse dentro de la cadena de Spring Security.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Agregamos la URL pública del frontend de Render
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://templates-frontend.onrender.com" // <-- Verifica que esta sea exactamente tu URL (sin / al final)
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}