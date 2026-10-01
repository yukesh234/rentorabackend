package com.bca.rentora.rentora.security;

import com.bca.rentora.rentora.entity.Role;
import com.bca.rentora.rentora.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.Instant;

@Service
@Getter
@Setter
public class JWTService {
    private final SecretKey secretKey;
    private final long accessTokenttl;
    private final long refreshTokenttl;
    private final String issuer;

    public JWTService(
            @Value("${security.jwt.secret}") String secretKey,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTokenttl,
            @Value("${security.jwt.access-ttl-seconds}") long accessTokenttl){
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.refreshTokenttl = refreshTokenttl;
        this.accessTokenttl = accessTokenttl;
    }

    public String generateAccessToken(User user){
        Instant now = Instant.now();
        List<String> roles = user.getRoles() == null? List.of()
                : user.getRoles().stream().map(Role::getName).toList();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("email", user.getEmail());
        claims.put("id", user.getUserid());
        claims.put("type","access");
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getUserid().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenttl)))
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

//    generating refresh tokens
    public String generateRefereshToken(User user,  String jti){
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("type","refresh");
        return Jwts.builder()
                .id(jti)
                .subject(user.getUserid().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenttl)))
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    //parsing jwt
    public Jws<Claims> parse(String token){
       return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }
    public boolean isAccessToken(String token){
        Claims c = parse(token).getPayload();
        return "access".equals(c.get("type"));
    }
    //is refresh token
    public boolean isRefreshToken(String token){
        Claims c = parse(token).getPayload();
        return "refresh".equals(c.get("type"));
    }

    //for user id
    public UUID getUserIdFromJwt(String token){
        return UUID.fromString(parse(token).getPayload().getSubject());
    }

    public String getJti(String token){
        return parse(token).getPayload().getId();
    }

    public boolean isExpired(String token){
        Instant now = Instant.now();
        //checks if expiration date is earlier then current date
        return parse(token).getPayload().getExpiration().before(Date.from(now));
    }

    public String getEmailFromJWT(String token) throws Exception{
        Claims c = parse(token).getPayload();
        System.out.println(c);
        return c.get("email").toString();
    }
    public static final UUID SUPER_ADMIN_ID =
            UUID.nameUUIDFromBytes("rentora-super-admin".getBytes(StandardCharsets.UTF_8));
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN"; // match your Role names (e.g. "ROLE_SUPER_ADMIN") if you prefix

    public String generateSuperAdminAccessToken(String email) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(SUPER_ADMIN_ROLE));
        claims.put("email", email);
        claims.put("id", SUPER_ADMIN_ID);
        claims.put("type", "access");
        claims.put("superAdmin", true);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(SUPER_ADMIN_ID.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenttl)))
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    public String generateSuperAdminRefreshToken(String email) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("email", email);
        claims.put("superAdmin", true);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(SUPER_ADMIN_ID.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenttl)))
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    public boolean isSuperAdminToken(String token) {
        Claims c = parse(token).getPayload();
        return Boolean.TRUE.equals(c.get("superAdmin", Boolean.class))
                && SUPER_ADMIN_ID.toString().equals(c.getSubject());
    }
}
