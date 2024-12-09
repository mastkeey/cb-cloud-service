# cb-cloud-service

Этот репозиторий содержит основной микросервис **CB Cloud Service**, который реализует основную бизнес-логику хранения файлов. Микросервис разработан с использованием Java и Spring Boot, а для управления сборкой и зависимостями используется Maven.

## Содержание

- [Описание проекта](#описание-проекта)
- [API контракт](#api-контракт-сервиса)
- [Структура репозитория](#структура-репозитория)
- [Сборка и запуск](#сборка-и-запуск)
    - [Предварительные требования](#предварительные-требования)
    - [Локальный запуск](#локальный-запуск)
    - [Запуск через Docker Compose](#запуск-через-docker-compose-из-server-configuration)
- [CI/CD](#cicd)

---

## Описание проекта

**CB Cloud Service** выполняет следующие задачи:
- Управление бизнес-логикой приложения.
- Обеспечение API для взаимодействия с другими микросервисами.
- Обработка и хранение данных с использованием PostgreSQL.
- Обработка и хранение файлов в MinIO.

Основные технологии:
- **Java 17**
- **Spring Boot**
- **PostgreSQL**
- **Minio**
- **Docker**
- **Maven** 

---

## API контракт сервиса

[API контракт](https://github.com/mastkeey/cb-cloud-service-open-api)

## Структура репозитория

```text
cb-cloud-service/
│
├── .github/
│   ├── workflows/
│       ├── buildTestPublish.yml   # CI/CD Workflow для сборки, тестирования и публикации
│       ├── deploy.yml             # Workflow для деплоя
│
├── src/
│   ├── main/                      # Исходный код приложения
│   ├── test/                      # Тесты приложения
│
├── .gitignore                     # Исключения для Git
├── Dockerfile                     # Dockerfile для сборки образа
├── mvnw, mvnw.cmd                 # Maven Wrapper
├── pom.xml                        # Maven файл для сборки
└── README.md                      # Документация
```
## Сборка и запуск

### Предварительные требования

Для корректной работы приложения требуются:
1. **PostgreSQL**: Используется для хранения данных.
    - Убедитесь, что база данных запущена и доступна.
    - Укажите следующие переменные окружения:
        - `POSTGRES_HOST`: Хост базы данных.
        - `POSTGRES_PORT`: Порт базы данных (по умолчанию 5432).
        - `POSTGRES_DB`: Имя базы данных.
        - `POSTGRES_USER`: Пользователь базы данных.
        - `POSTGRES_PASSWORD`: Пароль для подключения.
2. **MinIO**: Объектное хранилище для файлов.
    - Настройте доступ к MinIO, указав:
        - `MINIO_URL`: URL MinIO.
        - `MINIO_ACCESS_KEY`: Ключ доступа.
        - `MINIO_SECRET_KEY`: Секретный ключ.

### Локальный запуск

1. Убедитесь, что PostgreSQL и MinIO запущены и доступны.
2. Задайте переменные окружения:
   ```bash
   export POSTGRES_HOST=localhost
   export POSTGRES_PORT=5432
   export POSTGRES_DB=cb_cloud_service
   export POSTGRES_USER=admin
   export POSTGRES_PASSWORD=secret
   export MINIO_URL=http://localhost:9000
   export MINIO_ACCESS_KEY=minioadmin
   export MINIO_SECRET_KEY=minioadmin
   export TOKEN_TTL=10
   export TOKEN_SECRET=secret
   ```
3. Выполните команду для запуска приложения:
   ```bash
   mvn spring-boot:run
   ```
### Запуск через Docker Compose из [Server configuration](https://github.com/mastkeey/cb-server-config)

1. Настройте файл `.env` с необходимыми параметрами:
   ```env
   POSTGRES_HOST=localhost
   POSTGRES_PORT=5432
   POSTGRES_DB=cb_cloud_service
   POSTGRES_USER=admin
   POSTGRES_PASSWORD=secret
   MINIO_URL=http://localhost:9000
   MINIO_ACCESS_KEY=minioadmin
   MINIO_SECRET_KEY=minioadmin
   TOKEN_TTL=10
   TOKEN_SECRET=secret
   ```
2. Запустите приложение:
   ```bash
   docker-compose up -d cb-cloud-service
   ```
## CI/CD

Для автоматизации процессов сборки, тестирования и публикации используется **GitHub Actions**.

### Workflow: Build, Test, and Publish

#### Триггеры
- **Ручной запуск** через `workflow_dispatch` с параметрами:
    - `branch`: Ветка для сборки (по умолчанию `main`).
    - `skip_tests`: Пропустить тесты (по умолчанию `false`).
- **Автоматический запуск** при пуше в ветку `main`.

#### Основные этапы

1. **Сборка проекта**:
    - Настройка Java среды (JDK 17).
    - Кэширование зависимостей Maven.
    - Сборка проекта с использованием Maven:
      ```bash
      mvn clean package -DskipTests
      ```
    - Сохранение артефактов сборки (`*.jar`) в GitHub Actions.

2. **Запуск тестов**:
    - Скачивание ранее собранных артефактов.
    - Выполнение модульных тестов (если `skip_tests` не установлен в `true`):
      ```bash
      mvn test
      ```
    - Сохранение отчётов о тестах (`surefire-reports`).

3. **Публикация Docker-образа**:
    - Сборка Docker-образа с использованием последнего SHA коммита:
      ```bash
      docker build -t <docker_hub_username>/cb-cloud-service:<commit_sha> .
      ```
    - Публикация образа в Docker Hub с тегами:
        - `<commit_sha>`
        - `latest`

---

### Workflow: Deploy

#### Триггеры
- **Ручной запуск** через `workflow_dispatch` с параметром:
    - `image_tag`: Тег Docker-образа для деплоя (по умолчанию `latest`).

#### Основные этапы

1. Настройка SSH-доступа:
    - Использование приватного ключа SSH для подключения к серверу.
    - Добавление сервера в `known_hosts` для безопасного подключения.

2. Остановка текущего контейнера:
    - Остановка и удаление запущенного контейнера `cb-cloud-service`.

3. Обновление переменных окружения:
    - Обновление значения `CB_CLOUD_SERVICE_DOCKER_TAG` в файле `.env`.

4. Запуск нового контейнера:
    - Подтягивание нового Docker-образа из Docker Hub:
      ```bash
      docker pull <docker_hub_username>/cb-cloud-service:<image_tag>
      ```
    - Перезапуск сервиса с использованием `docker-compose`:
      ```bash
      docker-compose up -d --build --no-deps cb-cloud-service --remove-orphans
      ```

### Требования

Для корректной работы Workflows необходимо настроить следующие GitHub Secrets:

- **`DOCKER_HUB_USERNAME`**: Имя пользователя Docker Hub.
- **`DOCKER_HUB_PASSWORD`**: Пароль для Docker Hub.
- **`GH_TOKEN`**: Личный токен GitHub для работы с пакетами и API.
- **`GH_USERNAME`**: Имя пользователя GitHub.
- **`SERVER_IP`**: IP-адрес удалённого сервера для деплоя.
- **`SSH_PRIVATE_KEY`**: Приватный ключ для доступа к серверу через SSH.
- **`SSH_KNOWN_HOSTS`**: Список известных хостов для SSH.

И сервер как в [Server configuration](https://github.com/mastkeey/cb-server-config)
