# BIBO - Breath In, Breath Out

Android-приложение для дневника настроения и медитаций.

## Что внутри

- Kotlin + Jetpack Compose
- Clean Architecture
- Koin, Room, DataStore, WorkManager
- Ktor Client для работы с серверным API
- Локальная очередь синхронизации при временной недоступности сервера

## Настройка сервера

Адрес API задается в `local.properties`:

```properties
API_BASE_URL=http://localhost:8080
```

Если приложение запускается на Android Emulator, обычно нужен адрес:

```properties
API_BASE_URL=http://10.0.2.2:8080
```

## Команды

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```
