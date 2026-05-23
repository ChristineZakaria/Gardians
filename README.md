# 🛡️ Guardians Backend — Feature 1: Auth + Pairing + Alerts

Spring Boot 3 · Java 21 · PostgreSQL · JWT · Firebase FCM

---

## ⚡ Quick Start (5 minutes)

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 14+ **OR** Docker

---

### Option A — Run with Docker (easiest)

```bash
# 1. Clone / unzip the project
cd guardians-backend

# 2. Start everything (Postgres + App)
docker-compose up --build

# 3. API is live at:
#    http://localhost:8080
#    http://localhost:8080/swagger-ui.html
```

---

### Option B — Run locally (PostgreSQL already installed)

**Step 1 — Create the database**
```sql
-- In psql or pgAdmin:
CREATE DATABASE guardians_db;
CREATE USER guardians WITH PASSWORD 'guardians123';
GRANT ALL PRIVILEGES ON DATABASE guardians_db TO guardians;
```

**Step 2 — Build & Run**
```bash
cd guardians-backend
mvn clean package -DskipTests
java -jar target/guardians-backend-1.0.0.jar
```

The app runs Flyway automatically on startup — tables are created for you.

---

## 🌐 API Overview

| Base URL | `http://localhost:8080` |
|----------|------------------------|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| API Docs | `http://localhost:8080/v3/api-docs` |
| Health | `http://localhost:8080/actuator/health` |

---

## 📋 All Endpoints

### 🔐 Auth (`/auth`)

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/auth/register` | ❌ | Register Parent or Child |
| POST | `/auth/login` | ❌ | Login → JWT |
| GET  | `/auth/me` | ✅ | Get my profile |

**Register Body:**
```json
{
  "email": "parent@example.com",
  "fullName": "Ahmed Hassan",
  "password": "SecurePass123!",
  "phone": "+201001234567",
  "role": "PARENT"
}
```
> `role` can be `PARENT` or `CHILD`. Defaults to `PARENT` if omitted.

**Login Body:**
```json
{ "email": "parent@example.com", "password": "SecurePass123!" }
```

**Login Response:**
```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "parent@example.com",
  "fullName": "Ahmed Hassan",
  "role": "PARENT"
}
```
> Add `Authorization: Bearer <token>` header to all protected requests.

---

### 📱 Devices (`/devices`)

| Method | URL | Role | Description |
|--------|-----|------|-------------|
| POST | `/devices/register` | Any | Register this device |
| GET  | `/devices/my` | Any | My registered devices |
| GET  | `/devices/children` | PARENT | Children linked to me |
| POST | `/devices/pairing/generate` | PARENT | Get 6-digit pairing code |
| POST | `/devices/pairing/connect` | CHILD | Connect using pairing code |
| PATCH| `/devices/{deviceId}/heartbeat` | Any | Update last-seen |

**Full Pairing Flow:**

```
Step 1 — Parent calls POST /devices/pairing/generate
Response: { "code": "483920", "expiresAt": "2024-...", "message": "..." }

Step 2 — Parent shows code to child (in-app or out-of-band)

Step 3 — Child calls POST /devices/pairing/connect
Body: { "code": "483920", "deviceId": "child-uuid", "deviceName": "Omar's Phone" }
Response: { "success": true, "parentId": 1, "parentName": "Ahmed Hassan", "device": {...} }

✅ Done — child device is now linked to parent
```

**Code expires in 5 minutes.** A new code invalidates nothing; multiple codes can exist. Expired codes are cleaned automatically every minute.

---

### 🔔 Alerts (`/alerts`)

| Method | URL | Role | Description |
|--------|-----|------|-------------|
| POST | `/alerts/send` | CHILD | Send alert to linked parent |
| GET  | `/alerts` | PARENT | Get all alerts (paginated) |
| GET  | `/alerts?unreadOnly=true` | PARENT | Unread alerts only |
| GET  | `/alerts/unread-count` | PARENT | Count of unread |
| PATCH| `/alerts/{id}/read` | PARENT | Mark one as read |
| PATCH| `/alerts/read-all` | PARENT | Mark all as read |
| DELETE| `/alerts/{id}` | PARENT | Delete alert |

**Send Alert Body:**
```json
{
  "type": "UNSAFE_URL",
  "title": "Blocked website access attempt",
  "message": "Child tried to open blocked-site.com",
  "severity": "HIGH",
  "deviceId": "child-device-xyz789",
  "metadata": {
    "url": "https://blocked-site.com",
    "app": "Chrome"
  }
}
```

**Alert Types:**
`GEOFENCE_EXIT` | `GEOFENCE_ENTER` | `INAPPROPRIATE_IMAGE` | `INAPPROPRIATE_VIDEO` | `UNSAFE_URL` | `INAPPROPRIATE_TEXT` | `APP_BLOCKED` | `DEVICE_BLOCKED` | `DEVICE_OFFLINE` | `LOW_BATTERY` | `SCREEN_TIME_EXCEEDED` | `GENERAL` | `SOS`

**Severity Levels:** `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`

> When Firebase is enabled, parent receives a push notification automatically.

---

## 🧪 Testing with Postman

1. Open Postman → **Import** → select `Guardians_API.postman_collection.json`
2. The collection has pre-configured variables and test scripts
3. Run in order:
   - Register Parent → Register Child
   - Login Parent → Login Child *(tokens auto-saved)*
   - Register Parent Device → Register Child Device
   - **Generate Pairing Code** *(code auto-saved)*
   - **Connect Child Device** *(uses saved code)*
   - Send Alert → Get Alerts

Or use **Collection Runner** to run all requests automatically.

---

## 🔥 Firebase Setup (Optional)

By default `firebase.enabled=false` — FCM pushes are skipped (logged as mock).

To enable real FCM:
1. Go to Firebase Console → Project Settings → Service Accounts
2. Download `firebase-service-account.json`
3. Place it in `src/main/resources/`
4. Set environment variable: `FIREBASE_ENABLED=true`

---

## ⚙️ Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USER` | `guardians` | PostgreSQL username |
| `DB_PASS` | `guardians123` | PostgreSQL password |
| `JWT_SECRET` | *(64-char hex)* | **Change in production!** |
| `FIREBASE_ENABLED` | `false` | Enable FCM push |
| `FIREBASE_CREDENTIALS_PATH` | `firebase-service-account.json` | Path to Firebase JSON |

---

## 🏗️ Project Structure

```
src/main/java/com/guardians/
├── GuardiansApplication.java
├── config/
│   ├── SecurityConfig.java        ← JWT filter + CORS
│   ├── FirebaseConfig.java        ← FCM setup (mock-safe)
│   └── OpenApiConfig.java         ← Swagger
├── shared/
│   ├── entity/
│   │   ├── User.java              ← Implements UserDetails
│   │   ├── Device.java
│   │   ├── PairingCode.java
│   │   └── AlertEntity.java
│   └── exception/
│       ├── ApiException.java
│       └── GlobalExceptionHandler.java
└── modules/
    ├── auth/
    │   ├── controller/AuthController.java
    │   ├── service/AuthService.java
    │   ├── service/JwtService.java
    │   ├── service/UserDetailsServiceImpl.java
    │   ├── repository/UserRepository.java
    │   └── dto/ (RegisterRequest, LoginRequest, AuthResponse, UserResponse)
    ├── device/
    │   ├── controller/DeviceController.java
    │   ├── service/DeviceService.java
    │   ├── repository/DeviceRepository.java
    │   ├── repository/PairingCodeRepository.java
    │   └── dto/ (RegisterDeviceRequest, DeviceResponse, GeneratePairingCodeResponse,
    │             UsePairingCodeRequest, PairingResultResponse)
    └── alerts/
        ├── controller/AlertController.java
        ├── service/AlertService.java
        ├── service/FirebaseService.java
        ├── repository/AlertRepository.java
        └── dto/ (SendAlertRequest, AlertResponse, AlertPageResponse)
```

---

## 🔐 Security Notes

- JWT tokens expire in **24 hours**
- Passwords hashed with **BCrypt (cost 12)**
- Role-based access enforced with `@PreAuthorize`
- CORS configured to allow all origins (tighten in production)
- Change `JWT_SECRET` to a real 64-char random string in production

---

## 📦 Next Features (Step 2)

- Location tracking + Geofencing
- App monitoring + blocking
- Screen overlay control
- AI content detection (images, URLs, text)
- WebSocket real-time alerts
