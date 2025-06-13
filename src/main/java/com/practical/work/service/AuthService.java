package com.practical.work.service;

import com.practical.work.dto.*;
import com.practical.work.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@Transactional
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    public AuthResponse login(AuthRequest authRequest) {
        long startTime = System.currentTimeMillis();
        String email = authRequest.getEmail();
        
        log.info("Попытка входа пользователя: {}", email);

        try {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Пользователь не найден: {}", email);
                throw new RuntimeException("Пользователь не найден");
            }

            User user = userOpt.get();
            log.debug("Пользователь найден: {} (ID: {}, активен: {})", 
                    user.getEmail(), user.getId(), user.getIsActive());

            if (!user.getIsActive()) {
                log.warn("Попытка входа заблокированного пользователя: {}", email);
                throw new RuntimeException("Аккаунт заблокирован");
            }

            if (!userService.validatePassword(user, authRequest.getPassword())) {
                log.warn("Неверный пароль для пользователя: {}", email);
                throw new RuntimeException("Неверный пароль");
            }

            log.debug("Пароль валиден для пользователя: {}", email);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Пользователь {} успешно авторизован ({}ms)", email, duration);

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userService.convertToResponse(user))
                .build();
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка аутентификации пользователя {} ({}ms): {}", 
                    email, duration, e.getMessage());
            throw e;
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Регистрация нового пользователя: {}", registerRequest.getEmail());

        User user = userService.createUser(registerRequest);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Пользователь {} успешно зарегистрирован", user.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .user(userService.convertToResponse(user))
            .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Некорректный refresh токен");
            }

            String email = jwtService.extractEmail(refreshToken);
            Optional<User> userOpt = userService.findByEmail(email);

            if (userOpt.isEmpty()) {
                throw new RuntimeException("Пользователь не найден");
            }

            User user = userOpt.get();

            if (!user.getIsActive()) {
                throw new RuntimeException("Аккаунт заблокирован");
            }

            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            log.info("Токены обновлены для пользователя: {}", user.getEmail());

            return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(userService.convertToResponse(user))
                .build();

        } catch (Exception e) {
            log.error("Ошибка обновления токена", e);
            throw new RuntimeException("Ошибка обновления токена: " + e.getMessage());
        }
    }

    public UserResponse getCurrentUser(String accessToken) {
        try {
            if (!jwtService.isTokenValid(accessToken) || !jwtService.isAccessToken(accessToken)) {
                throw new RuntimeException("Некорректный access токен");
            }

            String email = jwtService.extractEmail(accessToken);
            Optional<User> userOpt = userService.findByEmail(email);

            if (userOpt.isEmpty()) {
                throw new RuntimeException("Пользователь не найден");
            }

            User user = userOpt.get();

            if (!user.getIsActive()) {
                throw new RuntimeException("Аккаунт заблокирован");
            }

            return userService.convertToResponse(user);

        } catch (Exception e) {
            log.error("Ошибка получения информации о пользователе", e);
            throw new RuntimeException("Ошибка получения информации о пользователе: " + e.getMessage());
        }
    }

    public User getUserFromToken(String accessToken) {
        if (!jwtService.isTokenValid(accessToken) || !jwtService.isAccessToken(accessToken)) {
            throw new RuntimeException("Некорректный access токен");
        }

        String email = jwtService.extractEmail(accessToken);
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Пользователь не найден");
        }

        User user = userOpt.get();

        if (!user.getIsActive()) {
            throw new RuntimeException("Аккаунт заблокирован");
        }

        return user;
    }
} 