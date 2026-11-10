# Digital Library Management System

Hệ thống quản lý thư viện (sách in + sách số) theo kiến trúc microservices.

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Cloud 2025.0 (Eureka, Gateway, OpenFeign) |
| Messaging | RabbitMQ (event `loan.*`, transactional outbox) |
| Database | PostgreSQL – mỗi service một database |
| Frontend | ReactJS (JavaScript) + Vite, React Router, Axios |

## Cấu trúc thư mục

```
digital-library/
├── backend/
│   ├── pom.xml                         # Maven parent (multi-module)
│   ├── infrastructure/
│   │   ├── eureka-server/   :8761      # Service registry
│   │   └── api-gateway/     :8080      # Single entry point, CORS, kiểm tra JWT của Keycloak
│   └── services/
│       ├── book-catalog-service :8081  # catalog_db  - book, book_copy, reservation, processed_event
│       ├── member-service       :8082  # member_db   - member
│       ├── loan-service         :8083  # loan_db     - loan, outbox_event
│       ├── fine-service         :8084  # fine_db     - fine_policy, fine
│       └── notification-service :8085  # notification_db - notification
├── frontend/                    :5173
│   └── src/ api/ components/ config/ context/ hooks/ layouts/ pages/ routes/ styles/ utils/
├── docker/postgres/             # 00-create-databases, 01-schema, 02-seed (chạy theo thứ tự)
└── docker-compose.yml           # hạ tầng dev; --profile app: toàn bộ hệ thống
```

Mỗi service theo layer: `controller → service (interface + impl) → repository → entity`, cùng `dto`, `exception`,
`config`, `messaging` (RabbitMQ), `client` (Feign – loan-service, notification-service).

Gateway route và phân quyền: xem mục "API Gateway" bên dưới.

## Cấu hình & file nhạy cảm

Các file chứa mật khẩu **bị git-ignore**: `application.properties`, `.env`, `.env.local`.
Repo chỉ chứa file mẫu `*.example`. Sau khi clone, mỗi thành viên tạo file thật từ file mẫu:

```bash
# Backend: với mỗi module
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Frontend
cp frontend/.env.example frontend/.env.local
# Docker
cp .env.example .env
```

## Chạy local

1. **Hạ tầng** – Docker: `docker compose up -d` → PostgreSQL, RabbitMQ (UI http://localhost:15672),
   Eureka (dashboard http://localhost:8761), Mailpit (hộp thư giả, http://localhost:8025).
   Lần đầu khởi tạo volume, PostgreSQL tự chạy các script trong `docker/postgres/` theo thứ tự:
   `00-create-databases.sql` (5 database) → `01-schema.sql` (bảng, khớp entity JPA) → `02-seed.sql` (dữ liệu mẫu).
   Script chỉ chạy khi volume trống; muốn tạo lại từ đầu (xoá hết dữ liệu): `docker compose down -v && docker compose up -d`.
   Máy đã cài sẵn PostgreSQL ở cổng 5432: đặt `POSTGRES_PORT=5433` trong `.env`.

   Không dùng Docker: chạy 3 file trên bằng `psql` trên PostgreSQL cài sẵn, đúng thứ tự:
   ```bash
   psql -U postgres -f docker/postgres/00-create-databases.sql
   psql -U postgres -f docker/postgres/01-schema.sql
   psql -U postgres -f docker/postgres/02-seed.sql
   ```
   Cả 3 file chạy lại được nhiều lần mà không lỗi và không nhân đôi dữ liệu.
   Database tạo từ bản script cũ: chạy thêm `03-upgrade-existing.sql` để bổ sung cột mới.
2. **Backend** (JDK 21): build `cd backend && mvn clean package -DskipTests`, rồi chạy theo thứ tự
   `eureka-server` → các service → `api-gateway`. Service vẫn khởi động được khi Eureka chưa lên và tự
   đăng ký lại sau.
3. **Frontend**: `cd frontend && npm install && npm run dev` → http://localhost:5173

## Chạy toàn bộ hệ thống bằng Docker

```bash
cp .env.example .env              # điền POSTGRES_*, KEYCLOAK_ADMIN_PASSWORD
docker compose --profile app up -d --build
docker compose --profile app ps   # chờ tất cả "healthy" (lần đầu build mất vài phút)
cd frontend && npm run dev        # frontend vẫn chạy trên máy, gọi Gateway ở http://localhost:8080/api
```

- Đăng nhập thủ thư: `TT-0001` / `Admin@12345` (tạo sẵn trong `docker/keycloak/library-realm.json`, chỉ dùng khi phát triển).
- Chỉ Gateway (8080) mở ra ngoài cho API; các service 8081–8085 chỉ nói chuyện trong network `dlms-network`
  và gọi nhau bằng **tên service** (`postgres:5432`, `rabbitmq:5672`, `eureka-server:8761`), không phải `localhost`.
- Cấu hình không bí mật của mỗi service nằm trong `ENV` của Dockerfile; mật khẩu và địa chỉ lấy từ `.env`
  qua `docker-compose.yml`. File `application.properties` không bao giờ được đưa vào image (`backend/.dockerignore`).
- Demo nhắc hạn nhanh: đặt `LOAN_DUE_SCAN_CRON=0 */2 * * * *` trong `.env` (loan-service quét hạn trả mỗi 2 phút),
  email nhắc hiện ở Mailpit http://localhost:8025.
- Xem log: `docker compose logs -f notification-service`. Dừng: `docker compose --profile app down`
  (thêm `-v` để xoá cả dữ liệu).

## Đăng nhập bằng Keycloak

Đăng nhập, mật khẩu, quên mật khẩu (gửi email) và cấp token do **Keycloak** (http://localhost:8180) làm; code của dự án
không còn tự cấp token hay lưu mật khẩu.

- `docker compose up -d` chạy Keycloak và tự nạp realm `library` từ `docker/keycloak/library-realm.json`:
  role `LIBRARIAN` / `MEMBER`, client `library-web` (frontend, PKCE) và client `member-service` (Admin API).
- Tài khoản dev: thủ thư `TT-0001` / `Admin@12345`; bạn đọc mẫu `TV-0412`, `TV-0087`, `TV-1130`, `TV-0256`, `TV-0901`
  / `Reader@12345`. Trang quản trị Keycloak: http://localhost:8180 (`KEYCLOAK_ADMIN` trong `.env`, mặc định admin/admin).
- Tên đăng nhập của bạn đọc là **số thẻ** (hoặc email). Bảng `member` chỉ còn hồ sơ; mật khẩu nằm ở Keycloak.
- Token do Keycloak ký **RS256**: Gateway chỉ cần khóa công khai (`app.security.jwt.jwk-set-uri`, `issuer-uri`),
  không còn secret dùng chung. Role đọc từ `realm_access.roles`; `mid` (id bạn đọc) là thuộc tính người dùng do
  member-service ghi khi cấp thẻ.
- Thủ thư cấp thẻ / nhập Excel / cấp lại mật khẩu: member-service tạo hoặc đổi tài khoản qua Keycloak Admin API
  (mật khẩu tạm, bạn đọc phải đổi ở lần đăng nhập đầu). Khóa thẻ (`LOCKED`) sẽ tắt tài khoản Keycloak.
- Email quên mật khẩu mặc định đi qua Mailpit (http://localhost:8025). Muốn gửi email thật, đặt SMTP trong Keycloak:
  Realm settings → Email.
- Nâng cấp database cũ: chạy `docker/postgres/03-upgrade-existing.sql` (xóa cột mật khẩu của `member`) **sau khi** Keycloak
  đã chạy với realm `library`.

## API Gateway: route và phân quyền

Route khai báo trong code `GatewayRoutesConfig` (không còn trong `application.properties`; ai còn các dòng
`spring.cloud.gateway.server.webflux.routes[...]` trong file local của mình thì xoá đi).

| Đường dẫn | Ai được gọi |
|---|---|
| `POST /api/auth/register`, `GET /api/books/**`, `/actuator/health` | Công khai |
| `GET /api/notifications/me`, `PATCH /api/notifications/{id}/read` | MEMBER (dữ liệu của chính mình) |
| Mọi `/api/**` còn lại | LIBRARIAN |

Lỗi trả về dạng JSON `{status, message, path}`: **401** khi thiếu token, token sai chữ ký hoặc hết hạn;
**403** khi role không đủ quyền. Gateway xoá các header `X-Username`, `X-User-Role`, `X-Member-Id` do client gửi
lên rồi điền lại từ JWT đã xác minh, để service phía sau biết ai đang gọi.

## Việc tiếp theo (TODO trong code)

- Loan: luồng Borrow/Return, `OutboxRelay`, `DueDateScheduler`
- Catalog: endpoint reserve availability, xử lý `loan.returned`
- Fine: xong. `loan.overdue` mở khoản phạt "đang tăng mỗi ngày", `loan.returned` chốt số tiền.
  Event lỗi sau 3 lần thử nằm ở queue `fine.loan-events.dlq`. Loan-service cần gửi thêm `loanType`
  (PHYSICAL/DIGITAL) trong payload để sách điện tử không bị phạt.
- Notification: xong. Nhận `loan.due-soon` / `loan.overdue`, chống trùng bằng `idempotency_key`, lấy email từ
  member-service, gửi email (Mailpit khi chạy Docker) và thử lại tối đa 3 lần; event lỗi nằm ở queue
  `notification.loan-reminders.dlq`. Lần đầu chạy bản này: xoá queue cũ `notification.loan-reminders` trên
  RabbitMQ UI (queue đã tồn tại không đổi được tham số dead-letter).
- Thay `ddl-auto=update` bằng Flyway migration trước khi release
