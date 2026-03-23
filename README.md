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
- Avatar upload path pattern: `avatars/yyyy/MM/{uuid}.{ext}`
- Item image upload path pattern: `item-images/yyyy/MM/{uuid}.{ext}`

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

### Get or update user profile

```bash
curl http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer <USER_TOKEN>"
```

```bash
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "realName":"Alice",
    "email":"alice@campus.local",
    "phone":"13800000000",
    "qqNo":"123456",
    "wechatNo":"alicewx",
    "collegeName":"Engineering",
    "majorName":"Software Engineering",
    "className":"Class 1",
    "dormitoryAddress":"Dorm 101"
  }'
```

### Upload avatar

```bash
curl -X POST http://localhost:8080/api/v1/user/profile/avatar \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -F "file=@C:/temp/avatar.png"
```

### List enabled item categories

```bash
curl http://localhost:8080/api/v1/public/item-categories
```

### Upload item image and create item

```bash
curl -X POST http://localhost:8080/api/v1/user/items/images \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -F "file=@C:/temp/item1.png"
```

```bash
curl -X POST http://localhost:8080/api/v1/user/items \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId":1,
    "title":"iPad Air",
    "brand":"Apple",
    "model":"Air 5",
    "description":"95 new, accessories included",
    "conditionLevel":"almost_new",
    "price":1999.00,
    "originalPrice":3999.00,
    "stock":1,
    "tradeMode":"both",
    "negotiable":true,
    "contactPhone":"13800000000",
    "contactQq":"123456",
    "contactWechat":"alicewx",
    "pickupAddress":"Dorm 101",
    "status":"on_sale",
    "imageFileIds":[2],
    "coverImageFileId":2
  }'
```

### List my items

```bash
curl "http://localhost:8080/api/v1/user/items?page=1&size=10" \
  -H "Authorization: Bearer <USER_TOKEN>"
```
### Public item browse and detail

```bash
curl "http://localhost:8080/api/v1/public/items?page=1&size=10&keyword=iPad&sortBy=latest"
```

```bash
curl "http://localhost:8080/api/v1/public/items?categoryId=1&priceMin=100&priceMax=3000&conditionLevel=almost_new&tradeMode=both&sortBy=price_asc"
```

```bash
curl http://localhost:8080/api/v1/public/items/1
```

### Public item comments

```bash
curl "http://localhost:8080/api/v1/public/items/1/comments?page=1&size=10"
```

```bash
curl -X POST http://localhost:8080/api/v1/user/items/1/comments \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"content":"Is this still available?"}'
```

```bash
curl -X POST http://localhost:8080/api/v1/user/comments/1/reply \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"content":"Yes, you can contact me tonight."}'
```

```bash
curl "http://localhost:8080/api/v1/user/comments/received?page=1&size=10" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

### Wanted posts

```bash
curl -X POST http://localhost:8080/api/v1/user/wanted-posts   -H "Authorization: Bearer <USER_TOKEN>"   -H "Content-Type: application/json"   -d '{
    "categoryId":2,
    "title":"Need Java Book",
    "brand":"Tsinghua",
    "description":"Looking for a clean copy of Java programming textbook",
    "expectedPriceMin":20,
    "expectedPriceMax":50,
    "status":"open",
    "expiresAt":"2026-04-30T18:00:00"
  }'
```

```bash
curl "http://localhost:8080/api/v1/user/wanted-posts?page=1&size=10"   -H "Authorization: Bearer <USER_TOKEN>"
```

```bash
curl "http://localhost:8080/api/v1/public/wanted-posts?page=1&size=10&keyword=Java&sortBy=latest"
```

```bash
curl http://localhost:8080/api/v1/public/wanted-posts/1
```

### Orders

```bash
curl -X POST http://localhost:8080/api/v1/user/orders   -H "Authorization: Bearer <USER_TOKEN>"   -H "Content-Type: application/json"   -d '{
    "itemId":1,
    "quantity":1,
    "receiverName":"Alice",
    "receiverPhone":"13800000000",
    "deliveryType":"dorm_delivery",
    "deliveryAddress":"Dorm 101",
    "buyerRemark":"please call first"
  }'
```

```bash
curl "http://localhost:8080/api/v1/user/orders?role=buyer&page=1&size=10"   -H "Authorization: Bearer <USER_TOKEN>"
```

```bash
curl -X POST http://localhost:8080/api/v1/user/orders/1/confirm   -H "Authorization: Bearer <SELLER_TOKEN>"   -H "Content-Type: application/json"   -d '{"actionNote":"I can deliver tonight"}'
```

```bash
curl -X POST http://localhost:8080/api/v1/user/orders/1/complete   -H "Authorization: Bearer <USER_TOKEN>"   -H "Content-Type: application/json"   -d '{"actionNote":"received"}'
```

```bash
curl -X POST http://localhost:8080/api/v1/user/orders/1/cancel   -H "Authorization: Bearer <USER_TOKEN>"   -H "Content-Type: application/json"   -d '{"cancelReason":"buyer changed mind"}'
```

### Announcements and notifications

```bash
curl -X POST http://localhost:8080/api/v1/admin/announcements   -H "Authorization: Bearer <ADMIN_TOKEN>"   -H "Content-Type: application/json"   -d '{
    "title":"System Notice",
    "content":"Dorm delivery starts at 7pm.",
    "pinned":true,
    "publishStatus":"published",
    "expireAt":"2026-04-30T23:59:59"
  }'
```

```bash
curl "http://localhost:8080/api/v1/public/announcements?page=1&size=5"
```

```bash
curl "http://localhost:8080/api/v1/user/notifications?readStatus=unread&page=1&size=10"   -H "Authorization: Bearer <USER_TOKEN>"
```

```bash
curl -X POST http://localhost:8080/api/v1/user/notifications/1/read   -H "Authorization: Bearer <USER_TOKEN>"
```


### Recommendations

```bash
curl "http://localhost:8080/api/v1/user/recommendations?page=1&size=10" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

```bash
curl -X POST "http://localhost:8080/api/v1/user/recommendations/refresh?limit=12" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

```bash
curl -X POST http://localhost:8080/api/v1/user/recommendations/1/click \
  -H "Authorization: Bearer <USER_TOKEN>"
```
