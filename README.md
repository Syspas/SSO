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

Демо-пользователи (пароль `admin`):

- `admin@local` — ROLE_ADMIN, ROLE_USER
- `user@local` — ROLE_USER

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

## Как подключить webapp (двойной режим)

1. Подними SSO на `:8090`.
2. Задай одинаковые секреты: `SSO_JWT_SECRET` = `PORTAL_SSO_JWT_SECRET`,
   `SSO_CLIENT_WEBAPP_SECRET` = `PORTAL_SSO_CLIENT_SECRET`.
3. `PORTAL_SSO_ENABLED=true`.
4. На `/login` появится «Войти через SSO» → `/login/sso` → callback создаёт сессию портала.

Без флага локальный form-login как раньше.

Email в JWT должен совпасть с учёткой в БД портала.

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
