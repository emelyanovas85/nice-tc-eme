FROM eclipse-temurin:17-jdk

# Установка curl и unzip с retry логикой
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get update --fix-missing || true && \
    apt-get update && \
    apt-get install -y curl unzip && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копирование JAR файла
COPY build/libs/nice-tc-1.0.0.jar /app.jar

# Порт приложения
EXPOSE 8443

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app.jar"]
