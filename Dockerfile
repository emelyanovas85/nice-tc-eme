FROM eclipse-temurin:17-jdk-alpine

# Установка curl для healthcheck
RUN apk add --no-cache curl

# Копирование JAR файла
ARG JAR_FILE=build/libs/nice-tc-1.0.0.jar
COPY ${JAR_FILE} app.jar

# Порт приложения
EXPOSE 8443

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app.jar"]
