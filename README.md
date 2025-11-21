docker run -d -p 3000:8080 \
--add-host=qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru:10.1.6.100 \
-v open-webui:/app/backend/data \
-v ~/certCBR/combined-ca-cert.pem:/tmp/combined-ca-cert.pem \
-e SSL_CERT_FILE=/tmp/combined-ca-cert.pem \
-e PYTHONHTTPSVERIFY=1 \
--name open-webui --restart always \
ghcr.io/open-webui/open-webui:main



docker run -d -p 3000:8080 \
--add-host=qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru:10.1.6.100 \
-v open-webui:/app/backend/data \
-v ~/certCBR/combined-ca-cert.pem:/tmp/combined-ca-cert.pem \
-e SSL_CERT_FILE=/tmp/combined-ca-cert.pem \
-e PYTHONHTTPSVERIFY=1 \
--name open-webui --restart always \
ghcr.io/open-webui/open-webui:main


docker run -d -p 3000:8080 \
--add-host=qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru:10.1.6.100 \
-v open-webui:/app/backend/data \
-v ~/certCBR/combined-ca-cert.pem:/tmp/combined-ca-cert.pem \
-e SSL_CERT_FILE=/tmp/combined-ca-cert.pem \
--name open-webui --restart always \
ghcr.io/open-webui/open-webui:main



docker exec -it open-webui /bin/sh
echo $SSL_CERT_FILE
ls -l $SSL_CERT_FILE

curl --cacert $SSL_CERT_FILE https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru/v1/models


docker logs open-webui --tail 100


echo | openssl s_client -showcerts -servername qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru -connect qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru:443 > certs_QWEN.p

docker stop open-webui
docker rm open-webui

curl --cacert $SSL_CERT_FILE https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru/v1/models
curl https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru/v1/models
curl https://10.1.6.100/v1/models

curl --cacert $SSL_CERT_FILE
https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru/v1/chat/completions \
-d '{"prompt":"Hello, world!", "max_tokens":5}' -H "Content-Type: application/json"



curl --cacert $SSL_CERT_FILE https://qwen3-32b-awq.apps.k8s.ehd-zr.cbr.ru/v1/chat/completions -H "Content-Type: application/json" -d '{"model": "qwen3-32b-awq", "messages": [{"role": "user", "content": "2+2"}], "temperature": 0.0}'


cp /tmp/combined-ca-cert.pem /usr/local/share/ca-certificates/combined-ca-cert.crt
update-ca-certificates



docker run -it --network=host -v /home/emelyanov_as@mail.spb.cbr.ru/IdeaProjects/nice-tc/mcpo-config.json:/app/mcpo-config.json ghcr.io/open-webui/mcpo:main serve --config /app/mcpo-config.json

docker run -it --network=host -v /home/emelyanov_as@mail.spb.cbr.ru/IdeaProjects/nice-tc/mcpo-config.json:/app/mcpo-config.json ghcr.io/open-webui/mcpo:main serve --config /app/mcpo-config.json
Unable to find image 'ghcr.io/open-webui/mcpo:main' locally

docker run -it --network=host ghcr.io/open-webui/mcpo:main --server-type "streamable_http" -- http://localhost:8090/mcp
Starting MCP OpenAPI Proxy on 0.0.0.0:8000 with command: http://localhost:8090/mcp


# Пересобрать и запустить одной командой
docker-compose up -d --build


# Жесткая пересборка без использования кэша
docker-compose build --no-cache && docker-compose up -d

# Пересобрать только сервис nice-tc
docker-compose build --no-cache nice-tc
docker-compose up -d nice-tc


# 1. Остановить и удалить контейнеры
docker-compose down

# 2. Удалить образы (опционально)
docker-compose down --rmi all

# 3. Очистить кэш сборки
docker builder prune -f

# 4. Пересобрать все заново
docker-compose build --no-cache --pull
docker-compose up -d


#логи mcpo
docker logs -f jira-mcp-server

# Удалить только "висящие" образы (без тегов)
docker image prune -f

# Удалить все неиспользуемые образы
docker image prune -a -f


