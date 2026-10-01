package com.bca.rentora.rentora.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@Getter
public class CookieService {
    private final String cookieName;
    private final boolean cookieHttponly;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String cookieDomain;

    public CookieService( @Value("${security.jwt.refresh-token-cookie-name}") String cookieName,
                         @Value("${security.jwt.cookie-http-only}") boolean cookieHttponly,
                         @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
                         @Value("${security.jwt.cookie-same-site}") String cookieSameSite,
                          @Value("${security.jwt.cookie-domain:localhost}")
                          String cookieDomain) {
        this.cookieName = cookieName;
        this.cookieHttponly = cookieHttponly;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookieDomain = cookieDomain;
    }

    public void attachCookie(HttpServletResponse resp, String value,int MaxAge){
        var responseBuilder = ResponseCookie.from(cookieName, value)
                .httpOnly(cookieHttponly)
                .sameSite(cookieSameSite)
                .maxAge(MaxAge)
                .secure(cookieSecure)
                .path("/");
        if(cookieDomain != null && !cookieDomain.isEmpty()) {
            responseBuilder.domain(cookieDomain);
        }
        ResponseCookie cookie = responseBuilder.build();
        resp.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }
    public void clearCookie(HttpServletResponse resp){
        var responseBuilder = ResponseCookie.from(cookieName,"")
                .maxAge(0)
                .secure(cookieSecure)
                .httpOnly(cookieHttponly)
                .path("/");
        if(cookieDomain != null && !cookieDomain.isEmpty()) {
            responseBuilder.domain(cookieDomain);
        }
        ResponseCookie cookie = responseBuilder.build();
        resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    public void addNoStoreHeader(HttpServletResponse resp){
        resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        resp.setHeader(HttpHeaders.PRAGMA, "no-cache");
    }
}
