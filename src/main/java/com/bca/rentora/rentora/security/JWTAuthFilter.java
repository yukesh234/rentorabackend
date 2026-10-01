package com.bca.rentora.rentora.security;

import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.repo.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final UserRepo userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. no bearer token: nothing to do
        final String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String token = header.substring(7);

        // 2. parse and verify the token once; a bad or expired token just means "not authenticated"
        final Claims claims;
        try {
            claims = jwtService.parse(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response); // entry point returns the 401 where auth is required
            return;
        }

        // 3. already authenticated: skip
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. only access tokens authenticate requests (refresh tokens must not)
        if (!"access".equals(claims.get("type"))) {
            filterChain.doFilter(request, response);
            return;
        }

        Object emailClaim = claims.get("email");
        if (emailClaim == null) {
            filterChain.doFilter(request, response);
            return;
        }
        final String email = emailClaim.toString();

        // 5. super admin: not in the DB, so trust the signed token
        boolean isSuperAdmin = Boolean.TRUE.equals(claims.get("superAdmin", Boolean.class))
                && JWTService.SUPER_ADMIN_ID.toString().equals(claims.getSubject());

        if (isSuperAdmin) {
            authenticate(request, email, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
            filterChain.doFilter(request, response);
            return;
        }

        // 6. normal user: load from DB
        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            // unknown user: stay unauthenticated so the request gets a clean 401
            filterChain.doFilter(request, response);
            return;
        }
        User user = userOpt.get();

        List<GrantedAuthority> authorities = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        authenticate(request, email, authorities);
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String email, List<? extends GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("/api/v1/login");
    }
}