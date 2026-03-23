# Campus Second-hand Backend

Spring Boot 3.3 + MyBatis-Plus + MySQL + JWT backend for the campus second-hand trading system.

## Run

1. Ensure MySQL is running and `campus_secondhand` already exists.
2. Adjust `src/main/resources/application.yml` or override env vars:
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `JWT_SECRET`
   - `STORAGE_ROOT_DIR`
   - `STORAGE_PUBLIC_BASE_URL`
   - optional `spring.mail.*` SMTP properties
3. Start the application:

```bash
mvn -gs .mvn-settings.xml spring-boot:run
```

## Default admin

- adminNo: `admin1001`
- password: `123456`

## Local storage

- Default upload root: `${user.dir}/storage`
- Public file base URL: `/uploads`
- Student card upload path pattern: `student-cards/yyyy/MM/{uuid}.{ext}`

## Swagger

- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Core APIs

### Admin login

```bash
curl -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"adminNo":"admin1001","password":"123456"}'
```

### Init bootstrap

```bash
curl -X POST http://localhost:8080/api/v1/admin/init/bootstrap \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

### Upload student card

```bash
curl -X POST http://localhost:8080/api/v1/public/files/student-card \
  -F "file=@C:/temp/student-card.png"
```

### Submit registration application

```bash
curl -X POST http://localhost:8080/api/v1/public/registration-applications \
  -H "Content-Type: application/json" \
  -d '{
    "studentNo":"20240001",
    "realName":"Alice",
    "gender":"female",
    "email":"alice@campus.local",
    "phone":"13800000000",
    "password":"123456",
    "collegeName":"Engineering",
    "majorName":"Software Engineering",
    "className":"Class 1",
    "studentCardFileId":1
  }'
```

### Review registration application

```bash
curl -X POST http://localhost:8080/api/v1/admin/registration-applications/1/approve \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"reviewRemark":"approved"}'
```

### User login

```bash
curl -X POST http://localhost:8080/api/v1/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"studentNo":"20240001","password":"123456","captcha":"","captchaKey":""}'
```