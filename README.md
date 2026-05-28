# Psychology Meditation Ktor Backend

Ktor backend для клиентского приложения по психологии и медитациям.

## Стек

- Kotlin
- Ktor Server
- Koin
- kotlinx.serialization
- Google Cloud Firestore Java client
- Gradle Kotlin DSL
- JVM / Java 21+

## Настройка

### Адрес слушателя

```bash
export HOST="localhost"
export PORT="8080"
```

`HOST` по умолчанию `localhost`, `PORT` по умолчанию `8080`.

### Аутентификация

```bash
export JWT_SECRET="change-me-to-a-long-random-secret"
export FIRESTORE_CREDENTIALS_PATH="/absolute/path/to/firebase-adminsdk-key.json"
```

Без `JWT_SECRET` и `FIRESTORE_CREDENTIALS_PATH` приложение стартует, но выпуск токенов и запросы к бд недоступны.

## API

Актуальный контракт находится в [contract/API_CONTRACT.md](contract/API_CONTRACT.md).

Основные endpoints:

- `GET /health`
- `POST /auth/register`
- `POST /auth/login`
- `GET /diary-entries?query=`
- `POST /diary-entries`
- `GET /diary-entries/{id}`
- `PUT /diary-entries/{id}`
- `DELETE /diary-entries/{id}`
- `GET /meditation-sessions?query=`
- `POST /meditation-sessions`
- `GET /meditation-sessions/{id}`
- `PUT /meditation-sessions/{id}`
- `DELETE /meditation-sessions/{id}`
- `GET /journal?query=`

Все endpoints дневниковых записей, сессий медитации и журнала защищены JWT Bearer auth.

## Сборка и запуск

```bash
./gradlew :app:build
```

```bash
./gradlew run
```

### Fat jar

Сборка исполняемого jar со всеми runtime-зависимостями через Ktor Gradle plugin:

```bash
./gradlew :app:buildFatJar
```

Файл будет создан здесь:

```text
app/build/libs/app-all.jar
```

Запуск:

```bash
java -jar app/build/libs/app-all.jar
```
