package com.bca.rentora.rentora.config;

import com.bca.rentora.rentora.security.JWTAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig{
    @Autowired
    private JWTAuthFilter filter;
    private final AuthenticationSuccessHandler successHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(AuthenticationSuccessHandler successHandler, CorsConfigurationSource corsConfigurationSource) {
        this.successHandler = successHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain SecurityFilterChain(HttpSecurity http)  {
       http.csrf(AbstractHttpConfigurer::disable)
               .cors(cors -> cors.configurationSource(corsConfigurationSource))
               .sessionManagement(sm-> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
               .oauth2Login(oauth ->
                   oauth.successHandler(successHandler)
                           .failureHandler(null))
               .logout(AbstractHttpConfigurer::disable)
               .authorizeHttpRequests(authorizeRequests ->
                       authorizeRequests.requestMatchers("/api/v1/auth/register").permitAll()
                               .requestMatchers("/api/v1/auth/login").permitAll()
                               .requestMatchers("/api/v1/auth/refresh").permitAll()
                               .requestMatchers(HttpMethod.GET,"/api/v1/listings").permitAll()
                               .requestMatchers("/api/v1/auth/**").permitAll()
                               .requestMatchers("/ws/**").permitAll()
                               .requestMatchers("/error").permitAll()
                               .requestMatchers(
                                       "/api/v1/super-admin/auth/login",
                                       "/api/v1/super-admin/auth/refresh",
                                       "/api/v1/super-admin/auth/logout"
                               ).permitAll()
                               .requestMatchers(HttpMethod.GET, "/api/v1/listings/*").permitAll()
                               .anyRequest().authenticated())
               .exceptionHandling(ex-> ex.authenticationEntryPoint((req,
                                                                    res,
                                                                    exc)-> {
                   exc.printStackTrace();
//Setting the responses
                   res.setStatus(401);
                   res.setContentType("application/json");
                   String message = "Unauthorized access" + exc.getMessage();
                   Map<String,Object>erromsg = Map.of("message",message,"Status",401);
                   var mapper = new ObjectMapper();
                   res.getWriter().write(mapper.writeValueAsString(erromsg));
               }))
               .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        System.out.println("CORS bean in use: " + http.getSharedObject(CorsConfigurationSource.class));
       return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config){
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder  passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsconfigurationSource(@Value("${spring.cors.app}") String url) {
        System.out.println("### CORS allowed origins resolved to: [" + url + "]");
        CorsConfiguration config = new  CorsConfiguration();

        String[] corseurl = url.trim().split(",");

        //adding the cors config
        config.setAllowedOrigins(Arrays.asList(corseurl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
//        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
            @Qualifier("corsconfigurationSource") CorsConfigurationSource corsConfigurationSource) {
        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource));
        registration.setOrder(Integer.MIN_VALUE);  // guaranteed to run before Spring Security's chain
        return registration;
    }
}
