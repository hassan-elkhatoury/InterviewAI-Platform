package com.interviewai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.dto.ApiResponse;
import com.interviewai.repository.UserRepository;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, UserRepository users, ObjectMapper mapper) throws Exception {
    var unauthorized =
        (org.springframework.security.web.AuthenticationEntryPoint)
            (req, res, e) -> {
              res.setStatus(401);
              res.setContentType("application/json");
              res.setHeader("WWW-Authenticate", "Bearer");
              mapper.writeValue(
                  res.getOutputStream(), ApiResponse.error("Authentication required"));
            };
    var forbidden =
        (org.springframework.security.web.access.AccessDeniedHandler)
            (req, res, e) -> {
              res.setStatus(403);
              res.setContentType("application/json");
              mapper.writeValue(res.getOutputStream(), ApiResponse.error("Access denied"));
            };
    return http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC)
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e -> e.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
        .oauth2ResourceServer(
            o ->
                o.authenticationEntryPoint(unauthorized)
                    .accessDeniedHandler(forbidden)
                    .jwt(
                        jwt ->
                            jwt.jwtAuthenticationConverter(
                                token -> {
                                  try {
                                    var user =
                                        users
                                            .findById(Integer.valueOf(token.getSubject()))
                                            .orElseThrow();
                                    Number version = token.getClaim("version");
                                    if (!user.isActive()
                                        || version == null
                                        || user.getTokenVersion() != version.intValue())
                                      throw new IllegalArgumentException();
                                    return new JwtAuthenticationToken(
                                        token,
                                        List.of(new SimpleGrantedAuthority(user.getRole())),
                                        token.getSubject());
                                  } catch (RuntimeException e) {
                                    throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_token"));
                                  }
                                })))
        .build();
  }
}
