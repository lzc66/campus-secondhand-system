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
3. Start the application:

```bash
mvn spring-boot:run
```

## Default admin

- adminNo: `admin1001`
- password: `123456`

## Swagger

- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Example

### Login

```bash
curl -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"adminNo":"admin1001","password":"123456"}'
```

### Bootstrap

```bash
curl -X POST http://localhost:8080/api/v1/admin/init/bootstrap \
  -H "Authorization: Bearer <TOKEN>"
```

### Init status

```bash
curl http://localhost:8080/api/v1/admin/init/status \
  -H "Authorization: Bearer <TOKEN>"
```
