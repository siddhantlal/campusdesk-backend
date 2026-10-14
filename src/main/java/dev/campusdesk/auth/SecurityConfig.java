package dev.campusdesk.auth;

import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.campusdesk.common.Problems;
import dev.campusdesk.users.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecretKeySpec signingKey(@Value("${app.jwt-secret}") String secret) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("JWT_SECRET must be base64 encoded"); }
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 random bytes");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKeySpec key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKeySpec key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var audience = (OAuth2TokenValidator<Jwt>) jwt -> jwt.getAudience().contains("campusdesk-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        var requiredExpiry = (OAuth2TokenValidator<Jwt>) jwt -> jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("campusdesk"),audience,requiredExpiry));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, UserRepository users, ObjectMapper mapper,
                                      @Value("${app.cors-origins}") String origins) throws Exception {
        var entry = (org.springframework.security.web.AuthenticationEntryPoint) (req,res,ex) -> {
            res.setStatus(401); res.setContentType("application/problem+json");
            res.setHeader("WWW-Authenticate", "Bearer");
            mapper.writeValue(res.getOutputStream(), Problems.create(401,"UNAUTHENTICATED","Valid authentication is required",req,Map.of()));
        };
        var denied = (org.springframework.security.web.access.AccessDeniedHandler) (req,res,ex) -> {
            res.setStatus(403); res.setContentType("application/problem+json");
            mapper.writeValue(res.getOutputStream(),Problems.create(403,"FORBIDDEN","You do not have permission for this operation",req,Map.of()));
        };
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET","POST","PATCH","OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization","Content-Type"));
        cors.setExposedHeaders(List.of("X-Correlation-ID")); cors.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",cors);
        http.csrf(csrf -> csrf.disable()).cors(c -> c.configurationSource(source))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/", "/index.html", "/app/**", "/favicon.svg").permitAll()
                .requestMatchers("/api/v1/auth/**","/actuator/health","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
            .oauth2ResourceServer(o -> o.authenticationEntryPoint(entry).accessDeniedHandler(denied)
                .jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                    long id;
                    try { id = Long.parseLong(jwt.getSubject()); }
                    catch (RuntimeException ex) { throw invalidToken(); }
                    User user = users.findById(id).filter(User::isActive).orElseThrow(SecurityConfig::invalidToken);
                    // Authorities come from the database, not a possibly stale claim in the JWT.
                    return UsernamePasswordAuthenticationToken.authenticated(new CurrentUser(id,user.getRole()),null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                })));
        return http.build();
    }
    private static OAuth2AuthenticationException invalidToken() { return new OAuth2AuthenticationException(new OAuth2Error("invalid_token")); }
}
