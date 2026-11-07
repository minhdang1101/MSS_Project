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
│   │   └── api-gateway/     :8080      # Single entry point, CORS, JWT
│   └── services/
│       ├── book-catalog-service :8081  # catalog_db  - book, book_copy, reservation, processed_event
│       ├── member-service       :8082  # member_db   - member
│       ├── loan-service         :8083  # loan_db     - loan, outbox_event
│       ├── fine-service         :8084  # fine_db     - fine_policy, fine
│       └── notification-service :8085  # notification_db - notification
├── frontend/                    :5173
│   └── src/ api/ components/ config/ context/ hooks/ layouts/ pages/ routes/ styles/ utils/
├── docker/postgres/init-databases.sql
└── docker-compose.yml           # PostgreSQL + RabbitMQ cho môi trường dev
```

Mỗi service theo layer: `controller → service (interface + impl) → repository → entity`, cùng `dto`, `exception`,
`config`, `messaging` (RabbitMQ), `client` (Feign – chỉ loan-service).

Gateway route: `/api/books/**`, `/api/members/**`, `/api/loans/**`, `/api/fines/**`, `/api/notifications/**`.

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

1. **Hạ tầng** – dùng PostgreSQL cài sẵn (tạo 5 database bằng `docker/postgres/init-databases.sql`)
   hoặc Docker: `docker compose up -d` (RabbitMQ UI: http://localhost:15672).
   Eureka cũng có trong compose: `docker compose up -d eureka-server` → dashboard http://localhost:8761.
2. **Backend** (JDK 21): build `cd backend && mvn clean package -DskipTests`, rồi chạy theo thứ tự
   `eureka-server` → các service → `api-gateway`. Service vẫn khởi động được khi Eureka chưa lên và tự
   đăng ký lại sau. Địa chỉ registry đọc từ biến `EUREKA_URL` (mặc định `http://localhost:8761/eureka/`;
   service chạy trong container dùng `http://eureka-server:8761/eureka/`).
3. **Frontend**: `cd frontend && npm install && npm run dev` → http://localhost:5173

## Việc tiếp theo (TODO trong code)

- Loan: luồng Borrow/Return, `OutboxRelay`, `DueDateScheduler`
- Catalog: endpoint reserve availability, xử lý `loan.returned`
- Fine: xong. `loan.overdue` mở khoản phạt "đang tăng mỗi ngày", `loan.returned` chốt số tiền.
  Event lỗi sau 3 lần thử nằm ở queue `fine.loan-events.dlq`. Loan-service cần gửi thêm `loanType`
  (PHYSICAL/DIGITAL) trong payload để sách điện tử không bị phạt.
- Notification: lưu & gửi nhắc nhở
- Auth: endpoint đăng nhập phát JWT, bật `app.security.enabled=true` ở gateway
- Thay `ddl-auto=update` bằng Flyway migration trước khi release
