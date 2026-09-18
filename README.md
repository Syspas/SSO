# SSO — отдельный сервис единого входа

Отдельное Spring Boot-приложение. **Не** часть `webapp` и **не** часть `excel-loader`.

Портал и Excel остаются своими JAR. Сюда ходят за логином и коротким JWT.

## Зачем третий процесс

| Процесс | Порт (локально) | Роль |
|---------|-----------------|------|
| **sso** | `8090`, context `/sso` | Логин, сессия `SSOSESSIONID`, выдача JWT |
| **webapp** | `8080` | Портал, разделы, админка |
| **excel-loader** | `8081`, context `/excel` | Excel за шлюзом |

Шлюз (nginx) ведёт `/sso/**` на SSO, `/excel/**` на Excel, остальное — на портал.

## Быстрый старт

```bash
cd /home/master/IdeaProjects/sso
./mvnw spring-boot:run
# или: mvn spring-boot:run
```

Открой: http://127.0.0.1:8090/sso/login

Демо-пользователи (пароль тот же, что сиды портала: `PortalSeed9!Change`):

| Email | ФИО |
|-------|-----|
| `admin@mail.ru` | Иванов Иван Иванович |
| `mod@mail.ru` | Петров Пётр |
| `user@mail.ru` | Сидоров Сидор Сидорович |
| `admin@local` | Локальный Админ |
| `user@local` | Локальный Пользователь |

Роли в JWT для портала **не** используются: доступ в webapp берётся из таблицы `users`.
При `PORTAL_SSO_ENABLED=true` портал принимает вход **только** через SSO.
Стык: **email + ФИО** из `/userinfo` должны совпасть с учёткой портала.
Смена пароля в админке портала **не** меняет пароль на `/sso/login` (две БД).

## Протокол v1 (упрощённый OAuth)

1. Клиент открывает браузер на:

   `GET /sso/authorize?client_id=webapp&redirect_uri=...&state=...`

2. Если нет cookie SSO — форма `/sso/login`.
3. После входа SSO редиректит на `redirect_uri?code=...&state=...`.
4. Бэкенд клиента меняет code на JWT:

   `POST /sso/token` (JSON или form)

```json
{
  "grant_type": "authorization_code",
  "code": "...",
  "client_id": "webapp",
  "client_secret": "webapp-secret-local",
  "redirect_uri": "http://127.0.0.1:8080/login/sso/callback"
}
```

5. `GET /sso/userinfo` с заголовком `Authorization: Bearer <jwt>`.

JWT: HMAC-SHA, claims `sub`=email, `name`, `roles`, TTL **5 минут** (handoff).
Долгая сессия — только cookie на SSO.

Клиенты в `application.yml`: `webapp`, `excel` (redirect URI + secret).

## Пользователи

Своя таблица `sso_users` (H2 in-memory по умолчанию).

**Не** читаем БД портала в v1 (чтобы не связывать деплой).  
**TODO:** синхронизация с `webapp` users или общий IdP / LDAP.

Риск «общей БД»: общий schema + пароли портала удобны, но SSO и портал начинают делить миграции и инциденты. Пока раздельно.

## Как подключить webapp (SSO-only)

1. Подними SSO на `:8090`.
2. Задай одинаковые секреты: `SSO_JWT_SECRET` = `PORTAL_SSO_JWT_SECRET`,
   `SSO_CLIENT_WEBAPP_SECRET` = `PORTAL_SSO_CLIENT_SECRET`.
3. `PORTAL_SSO_ENABLED=true`.
4. На `/login` остаётся только «Войти через SSO» (локальная форма скрыта,
   `POST /login` не открывает сессию).
5. Callback сверяет email **и** ФИО с таблицей `users` портала.

Без флага — обычный локальный form-login портала.

## Как подключить excel

Excel по-прежнему доверяет заголовкам шлюза (`X-Gateway-Secret`, `X-User-Email`).

Если `EXCEL_SSO_ENABLED=true` и задан тот же HMAC (`EXCEL_SSO_JWT_SECRET` = `SSO_JWT_SECRET`),
Excel также принимает `Authorization: Bearer` (когда шлюз не передал заголовки).

См. `Test_BD_xl/docs/gateway-seamless.md`.

## Секреты (локально)

| Переменная | Назначение |
|------------|------------|
| `SSO_JWT_SECRET` | HMAC для JWT (≥32 символа), общий с клиентами |
| `SSO_CLIENT_WEBAPP_SECRET` | secret клиента webapp |
| `SSO_CLIENT_EXCEL_SECRET` | secret клиента excel |
| `SSO_PORT` | порт (по умолчанию 8090) |
| `SPRING_PROFILES_ACTIVE=postgres` | своя Postgres вместо H2 |
| `SSO_DB_URL` / `SSO_DB_USER` / `SSO_DB_PASS` | JDBC SSO (не БД портала) |

## Тесты и CI

Те же двери, что у webapp: флаги Maven живут в `scripts/`, не в `Jenkinsfile`.
Локально и в Jenkins зовутся одни скрипты. Между слоями не делай `mvn clean` — сотрёшь Allure.

| Слой | Команда | Что проверяет |
|------|---------|---------------|
| Юнит | `./scripts/test-unit.sh` | MockMvc, JWT, сиды, клиенты. Без HtmlUnit и ArchUnit |
| Безопасность | `./scripts/test-security.sh` | `@Tag("security")`: чужой redirect, reuse code, битый JWT |
| UI | `./scripts/test-ui.sh` | HtmlUnit, форма логина, authorize после сессии |
| Архитектура | `./scripts/test-architecture.sh` | ArchUnit слоёв и запрет опасных API |
| Все слои | `./scripts/test-all.sh` | Четыре слоя подряд |
| Смоук | `./scripts/test-smoke-boot.sh` | Поднимает `target/sso.jar`, curl health/login |
| Пакет | `./scripts/package-dist.sh` | zip `target/sso-*.zip` (`-Pdist`, тесты уже прошли) |

Отчёт: `./mvnw allure:serve`.

Jenkins (тот же контейнер, что webapp, вкладка **sso**): `sso-build`, `sso-build-package`, однослойные задачи и `sso-smoke`. После правки `createSsoJobs.groovy` нужен restart Jenkins.

## Структура пакетов

```text
com.example.sso
  config/     — Security, properties, сид демо-юзеров
  user/       — сущность, репозиторий, UserDetailsService
  client/     — реестр клиентов из yml
  sso/        — authorize / token / userinfo (web, dto, service)
  exception/  — @RestControllerAdvice
```

Controller → валидация/маппинг; Service → логика; Repository → только CRUD.

## Что сделано / что дальше

**Сделано:** отдельный проект, логин+сессия, authorize/token/userinfo, клиенты webapp/excel,
health, профиль `postgres`, маршрут `/sso/` в nginx шлюза.

**Follow-up:** общая БД пользователей или sync, Redis для code, HTTPS/cookie Secure.
