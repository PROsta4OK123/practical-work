-- Начальные данные для приложения Word Editor
-- Этот файл автоматически выполняется Hibernate после создания схемы

-- Вставка тестового пользователя (пароль: admin123)
INSERT INTO users (email, password, first_name, last_name, role, points, is_active, created_at, updated_at) 
VALUES ('admin@example.com', '$2a$10$FYq7Y.XcJZ9ZjE4H0FJ2xO6sxE5ZhZ0QY.MZ8MX9Y8Z7X5.Y9Z8Z7X', 'Admin', 'User', 'ADMIN', 100, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE email = email;

-- Начальная конфигурация очереди (если нужно)
-- INSERT INTO ... другие начальные данные 