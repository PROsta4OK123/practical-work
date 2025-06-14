package com.practical.work.controller;

import com.practical.work.model.User;
import com.practical.work.service.AuthService;
import com.practical.work.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
// Префикс /api добавляется автоматически через context-path в application.yml
@RequestMapping("/subscription")
@CrossOrigin(origins = "*")
@Slf4j
public class SubscriptionController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseSubscription(
            @RequestHeader(value = "Authorization") String authHeader,
            @RequestBody Map<String, String> payload) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Пользователь не найден"));
            }

            String planId = payload.get("planId");
            if (planId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "planId не указан"));
            }

            // Здесь должна быть логика валидации planId и возможно интеграция с платежной системой
            // Для примера, просто добавляем поинты в зависимости от плана
            int pointsToAdd;
            switch (planId) {
                case "plan_1":
                    pointsToAdd = 100;
                    break;
                case "plan_2":
                    pointsToAdd = 500;
                    break;
                case "plan_3":
                    pointsToAdd = 1000;
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Неизвестный план подписки"));
            }

            User updatedUser = userService.addPoints(user, pointsToAdd);

            log.info("Пользователь {} приобрел {} баллов по плану {}", user.getEmail(), pointsToAdd, planId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Подписка успешно приобретена",
                "newPoints", updatedUser.getPoints()
            ));

        } catch (Exception e) {
            log.error("Ошибка при покупке подписки: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }
} 