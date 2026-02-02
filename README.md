# Система управления тестами (БЕЗ Thymeleaf)

Веб-приложение для управления тестами с AI интеграцией на **чистом Spring Boot REST API + статическом HTML**.

## Архитектура без Thymeleaf

### Frontend
- **Статический HTML** в `/static/`
- **Vanilla JavaScript** (ES6+) без фреймворков
- **REST API** взаимодействие через fetch()
- **Server-Sent Events** для real-time обновлений

### Backend
- **Spring Boot 3.2** только с `spring-boot-starter-web`
- **@RestController** для всех API endpoints
- **Статические ресурсы** из `/static/`
- **Без template engines** (Thymeleaf отключен)

## Преимущества подхода

✅ **Простота**: Нет сложности серверных шаблонов  
✅ **Производительность**: Статические ресурсы отдаются быстрее  
✅ **Разделение**: Четкое разделение frontend/backend  
✅ **API-first**: Готово для мобильных приложений  
✅ **Кеширование**: Лучшие возможности кеширования  

## Структура проекта

```
test-system/
├── src/main/java/com/testsystem/
│   ├── controller/          # REST API контроллеры
│   │   ├── JiraController   # /api/jira/**
│   │   ├── AiController     # /api/ai/**
│   │   └── SseController    # /api/sse
│   ├── service/            # Бизнес логика
│   ├── model/              # DTO модели
│   └── config/             # Конфигурация (без Thymeleaf)
├── src/main/resources/
│   ├── static/             # Статические ресурсы
│   │   ├── index.html     # Главная страница
│   │   ├── css/styles.css # Стили
│   │   └── js/            # JavaScript модули
│   └── application.properties  # Конфиг без Thymeleaf
└── build.gradle           # БЕЗ thymeleaf dependency
```

## REST API Endpoints

### Jira Integration
```
GET  /api/jira/tests/{testKey}     # Получить версии теста
GET  /api/jira/runs/{runId}        # Получить тесты из прогона  
GET  /api/jira/tests/{id}          # Получить данные версии
GET  /api/jira/status              # Статус подключения к Jira
```

### AI Integration  
```
POST /api/ai/batch/{testKey}       # Пакетная обработка промптов
POST /api/ai/chat                  # Отправить сообщение в чат
POST /api/ai/stop                  # Остановить обработку
GET  /api/ai/status                # Статус AI сервиса
```

### Real-time Events
```
GET  /api/sse                      # Server-Sent Events подключение
POST /api/sse/test                 # Тестовое событие
```

## Быстрый старт

### Установка и запуск
```bash
# Клонировать и запустить
./gradlew bootRun

# Приложение доступно на
http://localhost:8080
```

### Структура запросов

**Отправка сообщения в чат:**
```javascript
fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        testKey: 'T123',
        checkId: '1.0', 
        message: 'Проверить требование',
        placeholders: { requirement: 'Функция X' }
    })
});
```

**Получение обновлений через SSE:**
```javascript
const eventSource = new EventSource('/api/sse');
eventSource.addEventListener('chat_message', (event) => {
    const data = JSON.parse(event.data);
    console.log('Новое сообщение:', data);
});
```

## Конфигурация

### Spring Boot без Thymeleaf
```properties
# application.properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration
spring.web.resources.static-locations=classpath:/static/
```

### Gradle dependencies
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    // БЕЗ spring-boot-starter-thymeleaf
}
```

## Отличия от Thymeleaf версии

| Thymeleaf версия | Статическая версия |
|------------------|--------------------|
| Серверные шаблоны | Статический HTML |
| @Controller | @RestController |
| Model + View | JSON API |
| templates/ | static/ |
| Серверный рендеринг | Клиентский рендеринг |

## Развертывание

### Production готовность
- Статические ресурсы можно отдавать через CDN
- REST API легко масштабируется
- Возможность добавления фронтенд фреймворка позже
- API готово для мобильных приложений

### Docker
```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/test-system-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

## Использование

1. **Откройте** http://localhost:8080
2. **Введите** ID теста (T777) или прогона (C777) 
3. **Используйте** REST API для интеграций
4. **Наблюдайте** real-time обновления через SSE

---

**Примечание**: Эта версия идеально подходит для:
- API-first архитектуры
- Микросервисных приложений  
- Интеграции с фронтенд фреймворками
- Мобильных приложений
- Высоконагруженных систем
