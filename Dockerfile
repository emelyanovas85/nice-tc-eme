FROM eclipse-temurin:17-jdk

# Установка curl для healthcheck
RUN apt-get update && \
    apt-get install -y curl unzip && \
    rm -rf /var/lib/apt/lists/*

# Копирование JAR файла
COPY build/libs/nice-tc-1.0.0.jar /app.jar

# Порт приложения
EXPOSE 8443

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app.jar"]
