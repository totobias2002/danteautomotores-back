---
last_mapped_commit: dedb4c0b09d0e3c7ee9c69ffe7438f1d6d56bd0a
last_mapped_at: 2026-10-02
---
# External Integrations

**Analysis Date:** 2026-10-02

## APIs & External Services

**Image Hosting:**
- Cloudinary - Cloud-based image storage and CDN for auto photos
  - SDK/Client: `cloudinary-http44` v1.39.0
  - Implementation: `src/main/java/com/danteautomotores/config/CloudinaryConfig.java`
  - Auth: Environment variables `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
  - Purpose: Upload, store, and serve publication photos; supports max file size 10MB per upload

**Backend → Frontend Communication:**
- HTTP REST API over HTTPS
- Base URL: Backend at `http://localhost:8080/api` (development) or production domain
- Frontend at `danteautomotores-front/src/services/api.js` - Axios client with Bearer token injection
- CORS enabled via `src/main/java/com/danteautomotores/config/SecurityConfig.java`
  - Allowed origins: Configurable via `APP_CORS_ALLOWED_ORIGINS` environment variable
  - Default (dev): `http://localhost:5173` (Vite dev server)
  - Production example: `https://mi-app.vercel.app`

## Data Storage

**Primary Database:**
- PostgreSQL 16
  - Docker image: `postgres:16-alpine` (docker-compose.yml)
  - Development connection: `jdbc:postgresql://localhost:5433/danteautomotores` (port 5433 to avoid conflicts)
  - Production: Managed service (e.g., Railway Postgres, Render Postgres)
  - Environment variables for production:
    - `SPRING_DATASOURCE_URL` - Full JDBC connection string
    - `SPRING_DATASOURCE_USERNAME` - Database user
    - `SPRING_DATASOURCE_PASSWORD` - Database password

**Database Client:**
- Spring Data JPA with Hibernate ORM
- Location: Repository pattern in `src/main/java/com/danteautomotores/repository/`
- Schema auto-update: `spring.jpa.hibernate.ddl-auto: update` in `application.yml`

**Tables/Entities:**
- `usuario` - User accounts (ADMIN / COMPRADOR roles) with email + password (BCrypt hashed)
- `agencia` - Agency profiles managed by admins
- `publicacion` - Auto listings with title, description, price, status (DISPONIBLE/RESERVADO/VENDIDO)
- `foto_publicacion` - Photo URLs for listings (stored in Cloudinary)
- `consulta` - Contact messages from users about listings
- `favorito` - Bookmarked listings by users

**File Storage:**
- Cloudinary only - No local filesystem uploads
- Photo URLs stored in `foto_publicacion.url` column
- Development: Can upload test images; credentials can be empty or test account

**Caching:**
- None configured - No Redis or memcached integration

## Authentication & Identity

**Auth Provider:**
- Custom JWT-based (no OAuth/SAML provider)

**Implementation:**
- Backend:
  - `src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java` - Validates Bearer tokens on requests
  - `src/main/java/com/danteautomotores/controller/AuthController.java` - Login/registration endpoints
  - `src/main/java/com/danteautomotores/config/SecurityConfig.java` - Role-based authorization rules
- Frontend:
  - `danteautomotores-front/src/context/AuthContext.jsx` - Stores JWT in localStorage, adds to request headers
  - `danteautomotores-front/src/services/api.js` - Axios interceptor injects `Authorization: Bearer {token}` header
  - `danteautomotores-front/src/components/ProtectedRoute.jsx` - Guards admin pages for ADMIN role only

**Token Details:**
- Type: JWT (JSON Web Token)
- Signing algorithm: HS256 (HMAC with SHA-256)
- Secret: `APP_JWT_SECRET` environment variable (minimum 32 characters in production)
- Expiration: `APP_JWT_EXPIRATION_MS` environment variable (default 24 hours)
- Claims included: User ID, email, role (ADMIN/COMPRADOR), issue/expiration timestamps
- Storage: Frontend localStorage (`token` key) - accessible to client-side JavaScript

**Password Storage:**
- Backend: BCryptPasswordEncoder (Spring Security)
- Hash strength: Default work factor (typically 10 rounds)

**Access Control:**
- Role-based access control (RBAC) via Spring Security `@PreAuthorize` and HTTP matcher rules
- Roles: `ADMIN` (manage agencias/publicaciones), `COMPRADOR` (browse, save, consult)
- Route restrictions in `SecurityConfig.java` lines 43-52:
  - `POST/PUT/PATCH/DELETE` on agencias and publicaciones: ADMIN only
  - `GET` on agencias and publicaciones: Public
  - Auth endpoints (`/auth/**`): Public
  - Consultas (contact messages): Public POST, ADMIN-only GET/PUT/DELETE

## Monitoring & Observability

**Health Checks:**
- Spring Boot Actuator enabled (`spring-boot-starter-actuator`)
- Endpoint: `/actuator/health` (default management endpoint)
- Railway/Render can use for deployment health checks

**Error Tracking:**
- None configured (no Sentry, Datadog, etc.)
- Errors logged to stdout by Spring Boot

**Logs:**
- Backend: Spring Boot default logging to console (INFO level)
- Frontend: Browser console only (no remote logging)

## CI/CD & Deployment

**Hosting Targets:**
- Backend: Railway, Render, or any container-compatible platform
- Frontend: Vercel (configured), Netlify, or any static host with SPA rewrite

**CI Pipeline:**
- None detected (no GitHub Actions, Jenkins, GitLab CI configurations)
- Manual deployments expected

**Container Support:**
- Backend: Multi-stage Docker build in `Dockerfile`
  - Build stage: Maven 3.9 + JDK 21 (compiles Java)
  - Runtime stage: JRE 21-alpine (minimal size, ~700MB)
  - Exposed port: 8080
  - Entry: `java -jar app.jar`
- Frontend: No Dockerfile (deploy as static files to Vercel/Netlify)

**Frontend SPA Rewrite:**
- Vercel routing configured in `vercel.json` - all non-asset requests rewrite to `/index.html` for React Router

## Environment Configuration

**Required Env Vars for Production - Backend:**
- `PORT` - HTTP port (injected by Railway/Render)
- `SPRING_DATASOURCE_URL` - PostgreSQL JDBC URL, e.g., `jdbc:postgresql://db.railway.app:5432/production`
- `SPRING_DATASOURCE_USERNAME` - DB user
- `SPRING_DATASOURCE_PASSWORD` - DB password
- `APP_JWT_SECRET` - JWT signing key (32+ characters, cryptographically random)
- `APP_JWT_EXPIRATION_MS` - Token lifetime in milliseconds (optional, default 86400000 = 24h)
- `APP_CORS_ALLOWED_ORIGINS` - Comma-separated list, e.g., `https://frontend.vercel.app,https://example.com`
- `CLOUDINARY_CLOUD_NAME` - Cloudinary cloud identifier
- `CLOUDINARY_API_KEY` - Cloudinary API key
- `CLOUDINARY_API_SECRET` - Cloudinary API secret

**Required Env Vars for Production - Frontend:**
- `VITE_API_URL` - Backend API base URL, e.g., `https://api.railway.app/api`

**Secrets Location:**
- Backend: Environment variables (Railway/Render UI, GitHub Secrets, or `.env` file - never committed)
- Frontend: Environment variables in `.env` file (not committed; built into SPA)
- Never commit: `.env` files, `application.yml` with real credentials (template only in repo)

**Development Secrets:**
- Backend: `src/main/resources/application.yml` contains development defaults (weak JWT secret, hardcoded DB creds)
- Frontend: `.env.example` → `.env` with `VITE_API_URL=http://localhost:8080/api`
- Docker Compose: `docker-compose.yml` hardcodes dev DB credentials (local development only)

## Webhooks & Callbacks

**Incoming Webhooks:**
- None detected (no external webhook receivers)

**Outgoing Webhooks:**
- None detected (no notification system calling external services)

## API Endpoints Summary

**Public Endpoints:**
- `POST /api/auth/login` - User login (returns JWT token)
- `POST /api/auth/registro` - User registration (returns JWT token)
- `GET /api/publicaciones` - List all car listings (filters: search, price range, status, etc.)
- `GET /api/publicaciones/{id}` - Get single listing details
- `GET /api/agencias` - List all agencies
- `GET /api/agencias/{id}` - Get single agency profile
- `POST /api/consultas` - Create contact inquiry about a listing (no auth required)

**Admin-Only Endpoints:**
- `POST /api/agencias` - Create agency
- `PUT /api/agencias/{id}` - Edit agency
- `DELETE /api/agencias/{id}` - Delete agency
- `PATCH /api/agencias/{id}` - Partial agency update
- `POST /api/publicaciones` - Create listing (upload photos to Cloudinary)
- `PUT /api/publicaciones/{id}` - Edit listing
- `DELETE /api/publicaciones/{id}` - Delete listing
- `PATCH /api/publicaciones/{id}/cambiar-estado` - Change listing status (DISPONIBLE → RESERVADO → VENDIDO)
- `GET /api/consultas` - View all contact inquiries
- `GET /api/consultas/{id}` - Get single inquiry
- `PUT /api/consultas/{id}` - Reply to inquiry
- `DELETE /api/consultas/{id}` - Delete inquiry

**Authenticated User Endpoints:**
- `POST /api/favoritos` - Save listing to favorites
- `DELETE /api/favoritos/{publicacionId}` - Remove from favorites
- `GET /api/favoritos` - Retrieve user's saved listings

## Data Flow: Frontend → Backend → Database

1. **Login Flow:**
   - Frontend form submit → `POST /api/auth/login` (axios at `danteautomotores-front/src/services/api.js`)
   - Backend validates credentials, returns JWT → Frontend stores in localStorage
   - Future requests include `Authorization: Bearer {token}` via axios interceptor

2. **Browse Listings Flow:**
   - Frontend → `GET /api/publicaciones` (public, no auth needed)
   - Backend queries PostgreSQL, returns JSON with listing + photo URLs from Cloudinary
   - Frontend renders PublicacionCard components with Cloudinary image tags

3. **Upload Listing Photos (Admin):**
   - Admin form submit with file multipart upload → `POST /api/publicaciones`
   - Backend receives files, uploads each to Cloudinary via SDK
   - Cloudinary returns URLs → Backend saves URLs in `foto_publicacion` table
   - Backend returns listing with embedded photo URLs

4. **Contact Inquiry Flow:**
   - Frontend form → `POST /api/consultas` (public)
   - Backend stores consulta record in PostgreSQL
   - Admin views inquiries via `GET /api/consultas` (admin-only)

---

*Integration audit: 2026-10-02*
