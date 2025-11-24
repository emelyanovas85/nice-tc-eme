# Пересобрать и запустить одной командой
docker-compose up -d --build

# Жесткая пересборка без использования кэша
docker-compose build --no-cache && docker-compose up -d

# Пересобрать только сервис nice-tc
docker-compose build --no-cache nice-tc
docker-compose up -d nice-tc

# Остановить и удалить контейнеры
docker-compose down

# Удалить образы (опционально)
docker-compose down --rmi all

# Очистить кэш сборки
docker builder prune -f

# Пересобрать все заново
docker-compose build --no-cache --pull
docker-compose up -d

# логи mcp
docker logs -f jira-mcp-server
docker logs -f open-webui

# Удалить только "висящие" образы (без тегов)
docker image prune -f

# Удалить все неиспользуемые образы
docker image prune -a -f
