package com.practical.work.service;

import com.practical.work.dto.RegisterRequest;
import com.practical.work.dto.UserResponse;
import com.practical.work.model.User;
import com.practical.work.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(RegisterRequest registerRequest) {
        log.info("Создание нового пользователя: {}", registerRequest.getEmail());

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        User user = User.builder()
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .firstName(registerRequest.getFirstName())
            .lastName(registerRequest.getLastName())
            .role(User.Role.USER)
            .points(5) // Начальные поинты для новых пользователей
            .isActive(true)
            .build();

        User savedUser = userRepository.save(user);
        log.info("Пользователь создан успешно: {}", savedUser.getEmail());
        
        return savedUser;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public boolean usePoints(User user, int points) {
        if (user.getPoints() >= points) {
            user.setPoints(user.getPoints() - points);
            userRepository.save(user);
            log.info("Пользователь {} потратил {} поинтов. Остаток: {}", 
                user.getEmail(), points, user.getPoints());
            return true;
        }
        log.warn("У пользователя {} недостаточно поинтов. Требуется: {}, доступно: {}", 
            user.getEmail(), points, user.getPoints());
        return false;
    }

    public void addPoints(User user, int points) {
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
        log.info("Пользователю {} добавлено {} поинтов. Всего: {}", 
            user.getEmail(), points, user.getPoints());
    }

    public UserResponse convertToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .points(user.getPoints())
            .isActive(user.getIsActive())
            .build();
    }

    public boolean validatePassword(User user, String password) {
        return passwordEncoder.matches(password, user.getPassword());
    }
} 