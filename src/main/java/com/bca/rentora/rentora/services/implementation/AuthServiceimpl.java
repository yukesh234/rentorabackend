package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.auth.LoginReq;
import com.bca.rentora.rentora.dtos.auth.TokenResponse;
import com.bca.rentora.rentora.dtos.auth.UserDto;
import com.bca.rentora.rentora.dtos.auth.UserRequestDto;
import com.bca.rentora.rentora.entity.RefreshToken;
import com.bca.rentora.rentora.entity.User;
import com.bca.rentora.rentora.helpers.AuthHelper;
import com.bca.rentora.rentora.repo.RefToken;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.security.CookieService;
import com.bca.rentora.rentora.security.JWTService;
import com.bca.rentora.rentora.services.AuthService;
import com.bca.rentora.rentora.services.Userservice;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthServiceimpl implements AuthService {
    private final Userservice userservice;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final UserRepo userRepo;
    private ModelMapper modelMapper;
    private AuthHelper authHelper;
    private RefToken refresh;
    private final CookieService cookieService;
    @Override
    public UserDto Register(UserRequestDto userRequestDto) {
//        encrypting the password
         userRequestDto.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
         return userservice.createUser(userRequestDto);
    }

    @Override
    public TokenResponse Login(LoginReq loginReq, HttpServletResponse response) {
//        authenticate the user
        Authentication authentication = authHelper.authenicate(loginReq);
        User user = userRepo.findByEmail(loginReq.getEmail()).orElseThrow(()-> new BadCredentialsException("Invalid username or email"));


        //generating refresh token before the access token and saving in the database
        String jti = UUID.randomUUID().toString();
        var refTokenDB = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenttl()))
                .revoked(false)
                .build();
        refresh.save(refTokenDB);
        //creating the jwt token
        String token = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefereshToken(user,jti);
        //attaching refresh token in the cookies and nostore headers tell browser not to cache
        cookieService.attachCookie(response,refreshToken,(int) jwtService.getRefreshTokenttl());
        cookieService.addNoStoreHeader(response);
        //creating token response
        TokenResponse resp = TokenResponse.of(token,refreshToken,"Bearer",modelMapper.map(user, UserDto.class),jwtService.getAccessTokenttl());
        return resp;
    }

    @Override
    public TokenResponse refresh(HttpServletRequest request, HttpServletResponse resp) {
        if (request.getCookies() == null) {
            throw new BadCredentialsException("No refresh token found");
        }

        String refreshToken = Arrays.stream(request.getCookies())
                .filter(c -> cookieService.getCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("No refresh token found"));

        //checking if its refreshtoken
        if(!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        String jti = jwtService.getJti(refreshToken);
        UUID userid = jwtService.getUserIdFromJwt(refreshToken);
        RefreshToken storedtoken = refresh.findByJti(jti).orElseThrow(()-> new BadCredentialsException("Invalid refresh token"));
        if(storedtoken.isRevoked()){
            throw new BadCredentialsException("Invalid refresh token");
        }
        if(storedtoken.getExpiresAt().isBefore(Instant.now())){
            throw new BadCredentialsException("Invalid refresh token");
        }
        if(!storedtoken.getUser().getUserid().equals(userid)){
            throw new BadCredentialsException("Invalid refresh token");
        }

        //rotating the refresh token
        storedtoken.setRevoked(true);
        String newjti = UUID.randomUUID().toString();
        storedtoken.setReplacedByToken(newjti);
        User user = storedtoken.getUser();

        //building the new refresh token
        var reftoken = RefreshToken.builder()
                .jti(newjti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenttl()))
                .revoked(false)
                .build();
        refresh.save(reftoken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefereshToken(user,newjti);
        cookieService.attachCookie(resp,newRefreshToken,(int) jwtService.getRefreshTokenttl());
        cookieService.addNoStoreHeader(resp);
        return TokenResponse.of(newAccessToken,newRefreshToken,"Bearer",modelMapper.map(user, UserDto.class),jwtService.getAccessTokenttl());
    }

    @Override
    public void logout(HttpServletRequest req, HttpServletResponse resp) {
        if (req.getCookies() == null) {
            throw new BadCredentialsException("No refresh token found");
        }

        String refreshToken = Arrays.stream(req.getCookies())
                .filter(c -> cookieService.getCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("No refresh token found"));
        if(!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        String jti = jwtService.getJti(refreshToken);
        RefreshToken token = refresh.findByJti(jti).orElseThrow(()-> new BadCredentialsException("Invalid refresh token"));
        token.setRevoked(true);
        refresh.save(token);

        //removing from cookies
        cookieService.clearCookie(resp);
        cookieService.addNoStoreHeader(resp);
        SecurityContextHolder.clearContext();
    }


}
