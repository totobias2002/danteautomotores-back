---
last_mapped_commit: dedb4c0b09d0e3c7ee9c69ffe7438f1d6d56bd0a
last_mapped_at: 2026-10-02
---
<!-- refreshed: 2026-10-02 -->

# Architecture

**Analysis Date:** 2026-10-02

## System Overview

This is a full-stack used car marketplace application with a Spring Boot backend API and React frontend. The system manages vehicle listings, user authentication, agencies, and buyer inquiries.

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                            FRONTEND (React + Vite)                           │
│  danteautomotores-front/                                                     │
│  ├── Pages: HomePage, AutosPage, PublicacionDetallePage, etc.              │
│  ├── Components: UI, Forms, Navigation                                       │
│  └── Services: api.js (Axios HTTP client)                                    │
└───────────────────────────┬─────────────────────────────────────────────────┘
                            │
                   HTTP/HTTPS (JSON/JWT)
                            │
┌───────────────────────────▼─────────────────────────────────────────────────┐
│                   BACKEND (Spring Boot 3.3.5, Java 21)                        │
│  danteautomotores-back/src/main/java/com/danteautomotores/               │
│  ├── Controllers: API endpoints (Auth, Publicacion, Agencia, etc.)          │
│  ├── Services: Business logic layer                                          │
│  ├── Repositories: Data access (JPA)                                         │
│  ├── Security: JWT authentication filter, SecurityConfig                     │
│  ├── DTOs: Request/Response objects                                          │
│  ├── Entities: JPA models                                                    │
│  └── Config: CloudinaryConfig, SecurityConfig                               │
└───────────────────────────┬─────────────────────────────────────────────────┘
                            │
                     PostgreSQL Driver
                            │
┌───────────────────────────▼─────────────────────────────────────────────────┐
│                         PostgreSQL Database                                   │
│  Tables: usuarios, publicaciones, agencias, consultas, favoritos, etc.       │
└─────────────────────────────────────────────────────────────────────────────┘
                            │
                     HTTP REST API
                            │
┌───────────────────────────▼─────────────────────────────────────────────────┐
│                        Cloudinary (Image Storage)                             │
│  Stores vehicle photos, accessible via URLs in responses                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| **Frontend Pages** | Handle user interface for each route (home, listings, detail, login) | `danteautomotores-front/src/pages/` |
| **Frontend Components** | Reusable UI elements (cards, filters, navigation, forms) | `danteautomotores-front/src/components/` |
| **AuthContext** | Manages user session state (JWT token, user data, login/logout) | `danteautomotores-front/src/context/AuthContext.jsx` |
| **API Service** | Centralized HTTP client with JWT interceptor | `danteautomotores-front/src/services/api.js` |
| **AuthController** | User registration and login endpoints | `src/main/java/.../controller/AuthController.java` |
| **PublicacionController** | CRUD and search for vehicle listings | `src/main/java/.../controller/PublicacionController.java` |
| **AuthService** | Handles user registration, login, JWT token generation | `src/main/java/.../service/AuthService.java` |
| **PublicacionService** | Business logic for listings (search, CRUD, photos, state changes) | `src/main/java/.../service/PublicacionService.java` |
| **SecurityConfig** | Spring Security setup, JWT filter chain, CORS configuration | `src/main/java/.../config/SecurityConfig.java` |
| **JwtAuthenticationFilter** | Extracts and validates JWT tokens from request headers | `src/main/java/.../security/JwtAuthenticationFilter.java` |
| **Repositories** | Data persistence (JPA interfaces) | `src/main/java/.../repository/` |
| **Entities** | JPA models representing database tables | `src/main/java/.../entity/` |

## Pattern Overview

**Overall:** Layered (N-tier) REST API with frontend-backend separation

**Key Characteristics:**
- **Stateless REST API:** JWT-based authentication, no server-side session storage
- **Token-based auth:** User JWT token stored in localStorage, sent in Authorization header
- **Separation of concerns:** Frontend handles UI/UX, backend handles business logic and data persistence
- **DTO pattern:** Request/response objects separate API contracts from internal models
- **Service layer:** Business logic abstracted from controllers
- **Repository pattern:** Data access abstracted from services via Spring Data JPA

## Layers

### Frontend (React + Vite)

**Purpose:** Provide user interface for browsing listings, managing favorites, and admin functions

**Location:** `danteautomotores-front/src/`

**Contains:**
- Pages (route components): HomePage, AutosPage, PublicacionDetallePage, LoginPage, RegistroPage, FavoritosPage, AdminDashboardPage
- Components (UI): Navbar, Footer, PublicacionCard, FiltroAcordeon, ProtectedRoute
- Context (state): AuthContext for user session management
- Services: api.js (Axios HTTP client with JWT interceptor)
- Routes: AppRouter with route protection

**Depends on:**
- React Router for routing
- Axios for HTTP
- Lucide React for icons
- TailwindCSS for styling

**Used by:** End users via browser

### Controller Layer (Spring Boot)

**Purpose:** Handle HTTP requests and delegate to services

**Location:** `src/main/java/com/danteautomotores/controller/`

**Contains:**
- `AuthController` - `/api/auth/**` - User registration and login
- `PublicacionController` - `/api/publicaciones/**` - Vehicle listings CRUD
- `AgenciaController` - `/api/agencias/**` - Agency information
- `ConsultaController` - `/api/consultas/**` - Buyer inquiries
- `FavoritoController` - `/api/favoritos/**` - User favorites

**Depends on:** Services for business logic

**Used by:** Frontend via HTTP requests

### Service Layer

**Purpose:** Implement business logic, transaction management, and data flow orchestration

**Location:** `src/main/java/com/danteautomotores/service/`

**Contains:**
- `AuthService` - User authentication, token generation
- `PublicacionService` - Listing CRUD, search with filters, photo management
- `AgenciaService` - Agency data management
- `ConsultaService` - Inquiry handling
- `FavoritoService` - User favorites management
- `CloudinaryService` - Image upload/storage integration
- `JwtService` - Token generation and validation

**Depends on:** Repositories, external services (Cloudinary), SecurityContext

**Used by:** Controllers

### Repository Layer

**Purpose:** Abstract data persistence using Spring Data JPA

**Location:** `src/main/java/com/danteautomotores/repository/`

**Contains:**
- `UsuarioRepository` - User entity persistence
- `PublicacionRepository` - Vehicle listing persistence with JpaSpecificationExecutor for complex queries
- `AgenciaRepository` - Agency persistence
- `ConsultaRepository` - Inquiry persistence
- `FavoritoRepository` - Favorite persistence
- `FotoPublicacionRepository` - Photo persistence

**Depends on:** JPA, PostgreSQL JDBC driver

**Used by:** Services

### Entity & DTO Layer

**Purpose:** Define data models and API contracts

**Location:** 
- Entities: `src/main/java/com/danteautomotores/entity/`
- DTOs: `src/main/java/com/danteautomotores/dto/`

**Entities:**
- `Usuario` - User account (email, password hash, role)
- `Publicacion` - Vehicle listing (mark, model, year, price, specs)
- `Agencia` - Dealership information
- `Consulta` - Buyer inquiry
- `Favorito` - User favorite listing
- `FotoPublicacion` - Vehicle photo (URL via Cloudinary)

**DTOs (Request/Response):**
- Auth: `LoginRequest`, `RegistroRequest`, `AuthResponse`
- Publicacion: `PublicacionRequest`, `PublicacionResponse`, `FotoResponse`, `CambiarEstadoRequest`
- Similar pattern for other entities

### Security Layer

**Purpose:** Handle authentication, authorization, and request filtering

**Location:** `src/main/java/com/danteautomotores/security/` and `src/main/java/com/danteautomotores/config/`

**Contains:**
- `JwtAuthenticationFilter` - Extracts JWT from Authorization header, validates token, sets SecurityContext
- `JwtService` - Generates tokens, extracts claims, validates signature
- `SecurityConfig` - Configures Spring Security filter chain, CORS, authorization rules
- Password encoder (BCrypt)

**Auth Flow:**
1. User registers/logs in via `/api/auth/registro` or `/api/auth/login`
2. Backend validates credentials, generates JWT token
3. Token returned in `AuthResponse.token`
4. Frontend stores token in `localStorage.token`
5. Frontend Axios interceptor adds token to every request header: `Authorization: Bearer {token}`
6. Backend `JwtAuthenticationFilter` validates token on each request
7. If valid, user details loaded into `SecurityContext`
8. Controllers check authorization rules (public endpoints, admin-only endpoints, authenticated endpoints)

## Data Flow

### Primary Request Path: Browse & Search Listings

1. **Frontend:** User clicks "Autos" or applies filters on `AutosPage.jsx` (line 145-203)
2. **Frontend:** Page calls mock data (currently) from `catalogoMock.js`, or when backend ready: `api.get('/publicaciones', { params: {...} })`
3. **Backend:** Request arrives at `PublicacionController.buscar()` (line 24-36)
4. **Backend:** Controller delegates to `PublicacionService.buscar()` (line 36-48)
5. **Backend:** Service uses `PublicacionRepository.findAll()` with `PublicacionSpecification` for filtered query
6. **Database:** PostgreSQL executes query, returns Publicacion entities
7. **Backend:** `PublicacionMapper.toResponse()` converts entities to `PublicacionResponse` DTOs
8. **Backend:** Controller returns `ResponseEntity<List<PublicacionResponse>>`
9. **Frontend:** Axios receives JSON array
10. **Frontend:** AutosPage renders results as `PublicacionCard` components
11. **User:** Sees filtered list of vehicles

### Secondary Path: View Detail & Submit Inquiry

1. **Frontend:** User clicks on listing card → navigates to `/publicaciones/{id}` → `PublicacionDetallePage.jsx`
2. **Frontend:** Page calls `api.get('/publicaciones/{id}')` (line 86-90)
3. **Backend:** `PublicacionController.obtenerPorId(id)` (line 38-41)
4. **Backend:** Returns single `PublicacionResponse` with photos, specs, and agency info
5. **Frontend:** Renders detail page with gallery, specs, and contact form
6. **User:** Fills inquiry form (name, email, phone, message)
7. **Frontend:** Form submits via `api.post('/consultas', {...})` (line 142-154)
8. **Backend:** `ConsultaController` receives inquiry
9. **Backend:** Stored in database as `Consulta` entity
10. **Frontend:** Shows success message

### Authentication Path: Register & Login

1. **Frontend:** User navigates to `/registro` → `RegistroPage.jsx`
2. **Frontend:** Fills form with name, email, password, phone
3. **Frontend:** Form submission calls `AuthContext.registrar()` (line 17-20)
4. **Frontend:** `api.post('/auth/registro', { nombre, email, password, telefono })`
5. **Backend:** `AuthController.registro()` receives request (line 19-22)
6. **Backend:** `AuthService.registrar()` validates email uniqueness, hashes password, creates Usuario
7. **Backend:** Calls `JwtService.generateToken()` to create JWT
8. **Backend:** Returns `AuthResponse` with token, nombre, email, rol
9. **Frontend:** `guardarSesion()` stores token in `localStorage.token` and user data in `localStorage.usuario`
10. **Frontend:** Sets `usuario` state, context subscribers notified
11. **Frontend:** Protected routes now accessible, user can create favorites/submit inquiries

### Admin Path: Create/Edit Listing

1. **Frontend:** Admin navigates to `/admin` → `AdminDashboardPage.jsx`
2. **Frontend:** Clicks "Nueva publicación" → `/admin/publicaciones/nueva` → `AdminPublicacionFormPage.jsx`
3. **Frontend:** Form includes fields for vehicle specs, price, agency, and photo upload
4. **Frontend:** Form submission calls `api.post('/api/publicaciones', { marca, modelo, anio, precio, ... })`
5. **Backend:** `PublicacionController.crear()` receives authenticated request (line 43-46)
6. **Backend:** `SecurityConfig` verifies user has ROLE_ADMIN (line 47)
7. **Backend:** `PublicacionService.crear()` builds entity from request (line 54-76)
8. **Backend:** Gets authenticated user via `obtenerUsuarioAutenticado()` (sets as admin)
9. **Database:** Saves Publicacion entity
10. **Frontend:** Optionally uploads photos via `api.post('/publicaciones/{id}/fotos', formData)` (line 64-67 controller)
11. **Backend:** `PublicacionService.agregarFoto()` uploads to Cloudinary, stores URL in FotoPublicacion
12. **Frontend:** Redirects to listing detail page

## State Management

**Frontend:**
- **AuthContext:** Manages `usuario`, `esAdmin`, provides `login()`, `registrar()`, `logout()`
- **localStorage:** Persists `token` and `usuario` across page reloads
- **useState in pages:** Local state for filters, form values, loading states

**Backend:**
- **SecurityContext (Spring):** Holds authenticated user throughout request lifecycle
- **Database:** Single source of truth for all persistent data
- **No session storage:** Stateless design — all state in JWT token (user ID, email, role)

## Key Abstractions

### ProtectedRoute Component

**Purpose:** Restrict pages to authenticated users and check admin role

**File:** `danteautomotores-front/src/components/ProtectedRoute.jsx`

**Pattern:** 
- Checks `useAuth()` context
- If not authenticated, redirects to `/login`
- If `soloAdmin` prop set, checks `esAdmin` flag
- Renders children if authorized, else redirects

**Used by:** Routes in `AppRouter.jsx` for `/favoritos`, `/admin`, `/admin/publicaciones/*`

### JWT Token Structure

**Purpose:** Encode user identity and role without server-side state

**Format:** Bearer token signed with HS256

**Contents:** User email, role (ADMIN/COMPRADOR), expiration time

**Validation:** `JwtService` checks signature and expiration on each request

**Lifecycle:** Generated on auth, sent in every request header, validated before granting access

### Specification Pattern (PublicacionSpecification)

**Purpose:** Build complex database queries dynamically based on multiple filter criteria

**File:** `src/main/java/com/danteautomotores/repository/spec/PublicacionSpecification.java`

**Pattern:** Uses Spring Data `Specification<T>` to construct predicates for filtering by marca, modelo, price range, year range, agency, state, etc.

**Used by:** `PublicacionService.buscar()` when building filtered queries

## Entry Points

### Backend Entry Point

**Location:** `src/main/java/com/danteautomotores/DanteAutomotoresApplication.java`

**Triggers:** Spring Boot application startup

**Responsibilities:** 
- Initializes Spring context
- Loads application.yml configuration
- Registers all beans (Controllers, Services, Repositories, Security filters)
- Starts embedded Tomcat on port 8080

### Frontend Entry Point

**Location:** `danteautomotores-front/src/main.jsx`

**Triggers:** Browser loads `/index.html`

**Responsibilities:**
- Creates React root element
- Wraps App with `BrowserRouter` (enables routing)
- Wraps App with `AuthProvider` (enables auth context)
- Renders `<App />`

### Frontend Router Entry Point

**Location:** `danteautomotores-front/src/App.jsx`

**Structure:**
- `Navbar` component (top navigation)
- `AppRouter` component (route definitions)
- `Footer` component (bottom section)

**Routes:** Defined in `danteautomotores-front/src/routes/AppRouter.jsx` (lines 13-55)

## Architectural Constraints

- **Threading:** Single-threaded event loop (React), multi-threaded request handling (Spring Boot with embedded Tomcat)
- **Global state:** AuthContext (frontend), SecurityContext per request (backend)
- **Session state:** Stateless by design — all auth info in JWT token
- **Database connections:** Pooled via Spring Boot datasource configuration
- **Image storage:** External (Cloudinary) — backend doesn't store files locally
- **CORS:** Restricted to `VITE_API_URL` origin (configurable via environment) or defaults to `http://localhost:5173`
- **JWT expiration:** 24 hours (configurable via `APP_JWT_EXPIRATION_MS`)
- **Password security:** BCrypt hashing with salt (backend only)
- **File upload size:** Max 10MB per file (multipart configuration)

## Anti-Patterns to Avoid

### Mock Data in Production

**What happens:** Frontend components (AutosPage, PublicacionDetallePage) check `USE_MOCK_DATA` flag and use `catalogoMock.js` instead of calling API

**Why it's wrong:** 
- Data never reaches backend
- Can't test backend integration
- Hides API errors
- Frontend changes won't reflect in real data

**Do this instead:** Set `USE_MOCK_DATA = false` and ensure backend is running and accessible via `VITE_API_URL` environment variable. Test with real API data throughout development.

### Storing Sensitive Data in localStorage

**What happens:** JWT token and user role stored in `localStorage` (currently done in `AuthContext.jsx`)

**Why it's wrong:** 
- localStorage is vulnerable to XSS attacks
- No automatic expiration mechanism
- Shared across tabs unsecurely

**Do this instead:** Consider using httpOnly cookies for production. For now, ensure CSP headers are strict, sanitize all user inputs, and implement token refresh mechanism.

### Authorization Checks Only in Frontend

**What happens:** `ProtectedRoute` blocks navigation but doesn't prevent direct API calls if frontend is compromised

**Why it's wrong:** Frontend checks are easily bypassed; backend must enforce authorization

**Do this instead:** Always validate on backend. SecurityConfig properly restricts endpoints by role (lines 43-52). This is already implemented correctly — keep it.

### Mutating Response Objects

**What happens:** If DTOs were directly modified, it could expose internal state

**Why it's wrong:** API consumers could manipulate responses

**Do this instead:** Use immutable DTOs and mappers. Current approach with builders is correct.

## Error Handling

**Strategy:** 
- Backend throws custom exceptions (ResourceNotFoundException, IllegalArgumentException)
- Spring catches exceptions, returns appropriate HTTP status (404, 400, 401, 403, 500)
- Frontend handles errors in `.catch()` blocks, shows user-friendly messages

**Patterns:**
- Validation: `@Valid` annotations on DTO fields in controllers
- Not found: `ResourceNotFoundException` thrown in service layer
- Unauthorized: Spring Security returns 401, handled by frontend redirect to `/login`
- Bad request: `IllegalArgumentException` for business rule violations

**Current gaps:**
- Global exception handler missing — should implement `@ControllerAdvice` for consistent error responses
- Frontend doesn't retry on network failures
- No error logging configured for backend

## Cross-Cutting Concerns

**Logging:** 
- Backend: SLF4J with Spring Boot default (logback), SQL formatted output enabled via `jpa.show-sql`
- Frontend: Browser console, no structured logging

**Validation:**
- Backend: Hibernate Validator via `@Valid`, `@NotNull`, `@Email`, etc.
- Frontend: HTML5 input validation + manual checks before submission

**Authentication:**
- Backend: JWT token validation in `JwtAuthenticationFilter` before every request
- Frontend: Token presence check in `AuthContext`, redirect to login if missing

**CORS:**
- Backend: `CorsConfigurationSource` in `SecurityConfig` restricts origins to `APP_CORS_ALLOWED_ORIGINS`
- Frontend: Axios client configured with default `baseURL` pointing to backend

---

*Architecture analysis: 2026-10-02*
