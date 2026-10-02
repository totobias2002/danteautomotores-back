<!-- GSD:project-start source:PROJECT.md -->

## Project

**Dante Automotores**

Marketplace web de autos usados de la agencia Dante Automotores. Un único usuario administrador publica y gestiona los autos. Los usuarios registrados pueden comprar un auto (abrir una conversación de compra con el admin desde la propia web) y cotizar su propio auto, que reciben con un precio estimado al instante, para vendérselo a la agencia o entregarlo como parte de pago. Son dos repos: backend Spring Boot (`danteautomotores-back`) y frontend React (`danteautomotores-front`).

**Core Value:** Un usuario registrado y verificado puede encontrar un auto y hablar con la agencia para comprarlo, o cotizar el suyo, todo dentro de la web, y el admin sabe al 100 % con quién está hablando.

### Constraints

- **Tech stack**: Spring Boot 3.3 / Java 21 / PostgreSQL / JPA + React 18 / Vite / Tailwind v4 — se mantiene el stack existente
- **Repos**: back y front en repos separados (`danteautomotores-back`, `danteautomotores-front`); la planificación vive en el back
- **Imágenes**: Cloudinary (ya integrado) también para las fotos de los autos cotizados
- **Admin**: una única cuenta admin por ahora; el diseño no debe impedir sumar más en el futuro
- **Precios**: depende de un proveedor externo aún no elegido; el costo y el acceso pueden condicionar la elección
- **Timeline**: sin fecha; se prioriza hacerlo bien
- **UX**: el front debe ser familiar (patrones Kavak / Mercado Libre) y animado con moderación

<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->

## Technology Stack

## Languages

- Java 21 - Spring Boot REST API, JWT authentication, database ORM (JPA/Hibernate)
- JavaScript (ES6+) - React components, axios API client, React Router navigation
- CSS - Tailwind CSS v4 for styling

## Runtime

- Java 21 (eclipse-temurin for Docker)
- Spring Boot 3.3.5
- Node.js (package-lock.json indicates npm version 10+)
- Vite 5.4.10 as dev server and bundler
- Backend: Maven 3.9 - `pom.xml` contains dependency declarations, lockfile in `target/classes/`
- Frontend: npm - `package-lock.json` present (91 dependencies tracked)

## Frameworks

- Backend: Spring Boot 3.3.5 - RESTful API framework with dependency injection
- Frontend: React 18.3.1 - Component-based UI library
- Backend: Spring Web Starter - HTTP request handling, embedded Tomcat server (port 8080 default)
- Frontend: Axios 1.7.7 - HTTP client for API calls with interceptor support for JWT tokens
- Frontend: React Router v6.27.0 - Client-side navigation, protected routes for admin panel
- Backend: Spring Data JPA - ORM abstraction over Hibernate for PostgreSQL queries
- Backend: Spring Security 3.3.5 - Authorization, role-based access control (ADMIN/COMPRADOR)
- Backend: JJWT (Java JWT) 0.12.6 - JWT token generation and validation for stateless auth
- Backend: Spring Boot Test Starter, Spring Security Test - JUnit 5 framework (in pom.xml, scope:test)
- Frontend: No visible test framework configured (ESLint for linting only)
- Frontend: Tailwind CSS v4.0.0 - Utility-first CSS framework
- Frontend: Lucide React 1.47.0 - Icon library for UI components
- Backend: Maven - Multi-stage Docker build via `Dockerfile`, `spring-boot-maven-plugin` for JAR creation
- Frontend: Vite 5.4.10 - Lightning-fast build tool with React plugin, dev server on port 5173
- Frontend: @vitejs/plugin-react 4.3.3 - JSX/Fast Refresh support
- Frontend: @tailwindcss/vite 4.0.0 - Tailwind CSS integration with Vite

## Key Dependencies

- `postgresql` - JDBC driver for PostgreSQL database connectivity
- `jjwt-*` (api, impl, jackson) - JWT token handling for stateless authentication
- `cloudinary-http44` v1.39.0 - Image upload and hosting service SDK
- `spring-security` - Authentication/authorization enforcement
- `spring-data-jpa` - Database persistence layer
- `axios` - API communication with backend, token injection via interceptor
- `react-router-dom` - Navigation, route guards for admin pages
- `react` - Core UI framework
- `lucide-react` - SVG icon components for UI
- `tailwindcss` - Styling engine

## Configuration

- Primary config: `src/main/resources/application.yml` - Spring Boot YAML configuration
- Reads from environment variables (externalized for production deployment):
- Primary config: `vite.config.js` - Vite build configuration
- Environment variable: `VITE_API_URL` - Backend API base URL (defaults to `http://localhost:8080/api`)
- Config file: `.env.example` → `.env` in development
- Backend: `pom.xml` - Maven POM with spring-boot-maven-plugin for JAR packaging
- Frontend: `vite.config.js` - Vite config with React plugin and Tailwind CSS plugin
- Backend Dockerfile: Multi-stage build (Maven + JDK 21 for build → JRE 21-alpine for runtime)
- Backend: `docker-compose.yml` - PostgreSQL 16 on port 5433 (to avoid conflicts)
- Frontend: Dev server on localhost:5173 with hot module replacement (HMR)

## Platform Requirements

- Java 21 JDK (e.g., Eclipse Temurin)
- Maven 3.9+
- Node.js (npm 10+)
- Docker + Docker Compose (for PostgreSQL development database)
- PostgreSQL client (optional, for manual DB queries)
- Container runtime (Docker, Kubernetes) OR Java 21 JRE runtime + Tomcat-compatible server
- PostgreSQL database (managed service or self-hosted)
- Cloudinary account with API credentials
- HTTP port binding capability (Railway/Render provide PORT env var)
- Static hosting with SPA rewrite support (Vercel, Netlify, etc.)
- Vercel configuration present in `vercel.json` (rewrites all non-asset requests to `/index.html`)

## Deployment Targets

- Railway (native Spring Boot support, environment variable injection)
- Render (Web Service with custom Dockerfile)
- Any container hosting with environment variable support
- Vercel (configured with `vercel.json` for SPA routing)
- Netlify
- Any static host with SPA rewrite capability

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

## Backend (Java/Spring Boot)

### Naming Patterns

- Controllers: `*Controller.java` (e.g., `AuthController.java`)
- Services: `*Service.java` (e.g., `PublicacionService.java`)
- Entities: PascalCase, singular noun (e.g., `Usuario.java`, `Publicacion.java`)
- DTOs: Organized by feature in subdirectories under `dto/` (e.g., `dto/auth/LoginRequest.java`, `dto/publicacion/PublicacionResponse.java`)
- Repositories: `*Repository.java` (e.g., `UsuarioRepository.java`)
- Mappers: `*Mapper.java` (e.g., `PublicacionMapper.java`)
- Config classes: `*Config.java` (e.g., `SecurityConfig.java`, `CloudinaryConfig.java`)
- Exceptions: `*Exception.java` (e.g., `ResourceNotFoundException.java`)
- PascalCase for all class names
- Use meaningful names describing responsibility
- Entity classes: singular nouns (`Usuario`, `Publicacion`, `Agencia`, `Consulta`, `Favorito`)
- camelCase
- Action verbs: `crear()`, `actualizar()`, `obtener()`, `buscar()`, `guardar()`, `eliminar()`
- Service methods often mirror controller endpoints
- Mapper methods follow pattern: `toResponse()`, `toEntity()`
- Private helper methods use descriptive names: `construirRespuesta()`, `obtenerUsuarioAutenticado()`
- camelCase for local variables and fields
- Use meaningful names: `usuarioAutenticado`, `publicacionResponse`, `agenciaId`
- Boolean variables with is/has prefixes: `isAdmin`, `hasErrors`
- PascalCase: `Rol`, `Combustible`, `Condicion`, `EstadoPublicacion`, `Transmision`
- Located in `enums/` directory
- Use English or Spanish consistently within same enum

### Code Style

- Inherited from Spring Boot starter parent (Maven enforces defaults)
- 4-space indentation
- UTF-8 encoding
- Line length: not strictly enforced but follow Spring conventions (~100 chars recommended)
- No explicit linting tool configured (SpotBugs not present)
- Relies on IDE defaults and Spring Boot conventions
- Java 21 as target version in `pom.xml` property `<java.version>21</java.version>`
- Heavily used from Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Data`, `@RequiredArgsConstructor`
- Spring Framework: `@Service`, `@Repository`, `@Configuration`, `@Bean`, `@Value`, `@Autowired`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- Jakarta Validation: `@NotNull`, `@NotBlank`, `@Positive`, `@Valid`
- JPA/Hibernate: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated`, `@PrePersist`, `@OneToMany`, `@ManyToOne`
- Security: `@EnableWebSecurity`, `@RestControllerAdvice`, `@ExceptionHandler`

### Import Organization

### Error Handling

- Use custom `ResourceNotFoundException` (extends `RuntimeException`) for not-found scenarios
- Use `IllegalArgumentException` for validation/business rule violations (e.g., "Ya existe una cuenta con ese email")
- Use Spring's `BadCredentialsException` for authentication failures
- Use `MethodArgumentNotValidException` for validation errors
- `ResourceNotFoundException` → HTTP 404 with JSON: `{"error": "message"}`
- `IllegalArgumentException` → HTTP 400 with JSON: `{"error": "message"}`
- `BadCredentialsException` → HTTP 401 with generic message
- `MethodArgumentNotValidException` → HTTP 400 with field errors: `{"fieldName": "validation message", ...}`

### Logging

- No logger instances found in codebase
- Configuration in `application.yml`: `show-sql: true` and `format_sql: true` for SQL logging
- SQL logging for debugging during development

### Comments

- Explain business logic not obvious from code
- Document assumptions (e.g., "el registro público siempre crea compradores; los admins se cargan aparte")
- Explain why, not what
- Not systematically used in codebase
- Consider adding for public API methods

### Function Design

- Methods range from 2 lines (simple getters/setters via Lombok) to ~70 lines (PublicacionService methods)
- Recommended: Keep methods under 30 lines where possible
- Use helper methods for complex logic
- Use DTOs for request objects instead of multiple primitives
- Example: `crearAgencia(AgenciaRequest request)` not `crearAgencia(String nombre, String email, ...)`
- Use `@Valid` annotation on DTO parameters for automatic validation
- Services return DTOs (Response objects) not entities
- Controllers wrap service responses in `ResponseEntity<T>` for HTTP status control
- Use `.orElseThrow()` for repository operations to fail fast on missing entities

### Module Design

- Controllers expose REST endpoints via `@RestController` and request mapping annotations
- Services expose business logic methods (public)
- Repositories extended from Spring Data `JpaRepository<T, ID>`
- DTOs are request/response contracts
- Mappers use public static methods for stateless conversion

## Frontend (React/Vite)

### Naming Patterns

- Components: PascalCase with `.jsx` extension (e.g., `PublicacionCard.jsx`, `Navbar.jsx`)
- Pages: PascalCase with `.jsx` extension (e.g., `HomePage.jsx`, `LoginPage.jsx`)
- Contexts: PascalCase with `.jsx` extension (e.g., `AuthContext.jsx`)
- Services/utilities: camelCase with `.js` extension (e.g., `api.js`, `whatsapp.js`)
- Mocks: camelCase with `Mock.js` suffix (e.g., `catalogoMock.js`, `homeMock.js`)
- PascalCase: `PublicacionCard`, `SeccionConfianza`, `AdminDashboardPage`, `ProtectedRoute`
- Descriptive names reflecting purpose
- Suffix with `Page` for page/route components
- Suffix with `Accordion`, `Card`, `Form` for reusable components
- camelCase: `formatoNumero()`, `sugerencias`, `cargando`, `handleSubmit()`
- Hook names start with `use`: `useAuth()`, `useEffect()`, `useState()`, `useMemo()`
- Event handlers: `handle*` prefix (e.g., `handleSubmit`, `handleClick`, `elegirSugerencia`)
- Constants in UPPERCASE: `USE_MOCK_DATA`, `ESTADO_BADGE`, `BENEFICIOS`
- camelCase for prop names and state variables
- Descriptive: `publicacion`, `mostrarPassword`, `sugerenciasAbiertas`, `cargando`

### Code Style

- No `.prettierrc` configured; uses IDE defaults
- ESLint configured with `npm run lint: eslint .`
- Vite with React plugin for Hot Module Replacement
- ESLint enabled: `"lint": "eslint ."` in package.json
- No specific config file found; uses default ESLint + React rules
- JSX syntax via @vitejs/plugin-react
- 2-space indentation (common React convention)
- Semicolons used consistently
- String interpolation with backticks for template literals
- Destructuring imports: `import { useState, useEffect } from 'react'`

### Import Organization

- Not configured; uses relative imports (`../components/`, `../services/`)

### Error Handling

- Try-catch in async handlers with `.catch()` fallbacks
- Error state via `useState('')`
- Display errors in UI with conditional rendering
- Service methods throw errors; components catch them
- No global error boundary observed
- Each page/component manages its own error state

### Logging

- No explicit logging found in codebase
- Use browser console for debugging

### Comments

- Explain business logic (e.g., default filter behavior)
- Mark TODO items for future work
- Document mock data usage
- `src/pages/HomePage.jsx` (line 12-14): Mock data toggle with TODO to remove when backend ready
- `src/pages/LoginPage.jsx` (line 33): Google login integration TODO
- Multiple pages have `.catch(() => {})` with empty handlers (catch-all for non-critical API failures)

### Function Design

- Page components range from 50-580 lines (HomePages is 280 lines)
- Component functions are compact and focused
- Extract complex rendering into subcomponents
- Props destructured in function signature: `export default function PublicacionCard({ publicacion })`
- Use descriptive prop names
- Components return JSX elements
- Handlers return void (side effects via setState)
- Custom hooks return state/functions

### Module Design

- Components export as default: `export default function ComponentName() {}`
- Context exports both provider and hook: `export function AuthProvider()` and `export function useAuth()`
- Mock data exports named exports for flexibility

### Styling

- Utility-first: `className="flex items-center gap-2 rounded-xl border..."`
- Color system: `text-bronze`, `bg-navy`, `text-slate-400`, `text-white/90`
- Responsive: `md:grid-cols-2`, `lg:px-10`, `sm:flex`
- Hover/interactive: `hover:bg-bronze`, `hover:text-white`, `focus-within:border-bronze`
- Animations: `animate-fade-in-up`, `animate-marquee`
- Custom config in `tailwind.config.*` (if present)

## Language & Terminology

- User-facing messages
- Error messages
- Comments and documentation
- Database table names and field names
- Enum values
- Database tables: `usuarios`, `publicaciones`, `agencias`, `consultas`, `favoritos`
- Entity classes: `Usuario`, `Publicacion`, `Agencia`, `Consulta`, `Favorito`
- DTOs: `RegistroRequest`, `LoginRequest`, `PublicacionRequest`
- Messages: "Ya existe una cuenta con ese email", "Email o contraseña incorrectos"

<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

## System Overview

```text

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

- **Stateless REST API:** JWT-based authentication, no server-side session storage
- **Token-based auth:** User JWT token stored in localStorage, sent in Authorization header
- **Separation of concerns:** Frontend handles UI/UX, backend handles business logic and data persistence
- **DTO pattern:** Request/response objects separate API contracts from internal models
- **Service layer:** Business logic abstracted from controllers
- **Repository pattern:** Data access abstracted from services via Spring Data JPA

## Layers

### Frontend (React + Vite)

- Pages (route components): HomePage, AutosPage, PublicacionDetallePage, LoginPage, RegistroPage, FavoritosPage, AdminDashboardPage
- Components (UI): Navbar, Footer, PublicacionCard, FiltroAcordeon, ProtectedRoute
- Context (state): AuthContext for user session management
- Services: api.js (Axios HTTP client with JWT interceptor)
- Routes: AppRouter with route protection
- React Router for routing
- Axios for HTTP
- Lucide React for icons
- TailwindCSS for styling

### Controller Layer (Spring Boot)

- `AuthController` - `/api/auth/**` - User registration and login
- `PublicacionController` - `/api/publicaciones/**` - Vehicle listings CRUD
- `AgenciaController` - `/api/agencias/**` - Agency information
- `ConsultaController` - `/api/consultas/**` - Buyer inquiries
- `FavoritoController` - `/api/favoritos/**` - User favorites

### Service Layer

- `AuthService` - User authentication, token generation
- `PublicacionService` - Listing CRUD, search with filters, photo management
- `AgenciaService` - Agency data management
- `ConsultaService` - Inquiry handling
- `FavoritoService` - User favorites management
- `CloudinaryService` - Image upload/storage integration
- `JwtService` - Token generation and validation

### Repository Layer

- `UsuarioRepository` - User entity persistence
- `PublicacionRepository` - Vehicle listing persistence with JpaSpecificationExecutor for complex queries
- `AgenciaRepository` - Agency persistence
- `ConsultaRepository` - Inquiry persistence
- `FavoritoRepository` - Favorite persistence
- `FotoPublicacionRepository` - Photo persistence

### Entity & DTO Layer

- Entities: `src/main/java/com/danteautomotores/entity/`
- DTOs: `src/main/java/com/danteautomotores/dto/`
- `Usuario` - User account (email, password hash, role)
- `Publicacion` - Vehicle listing (mark, model, year, price, specs)
- `Agencia` - Dealership information
- `Consulta` - Buyer inquiry
- `Favorito` - User favorite listing
- `FotoPublicacion` - Vehicle photo (URL via Cloudinary)
- Auth: `LoginRequest`, `RegistroRequest`, `AuthResponse`
- Publicacion: `PublicacionRequest`, `PublicacionResponse`, `FotoResponse`, `CambiarEstadoRequest`
- Similar pattern for other entities

### Security Layer

- `JwtAuthenticationFilter` - Extracts JWT from Authorization header, validates token, sets SecurityContext
- `JwtService` - Generates tokens, extracts claims, validates signature
- `SecurityConfig` - Configures Spring Security filter chain, CORS, authorization rules
- Password encoder (BCrypt)

## Data Flow

### Primary Request Path: Browse & Search Listings

### Secondary Path: View Detail & Submit Inquiry

### Authentication Path: Register & Login

### Admin Path: Create/Edit Listing

## State Management

- **AuthContext:** Manages `usuario`, `esAdmin`, provides `login()`, `registrar()`, `logout()`
- **localStorage:** Persists `token` and `usuario` across page reloads
- **useState in pages:** Local state for filters, form values, loading states
- **SecurityContext (Spring):** Holds authenticated user throughout request lifecycle
- **Database:** Single source of truth for all persistent data
- **No session storage:** Stateless design — all state in JWT token (user ID, email, role)

## Key Abstractions

### ProtectedRoute Component

- Checks `useAuth()` context
- If not authenticated, redirects to `/login`
- If `soloAdmin` prop set, checks `esAdmin` flag
- Renders children if authorized, else redirects

### JWT Token Structure

### Specification Pattern (PublicacionSpecification)

## Entry Points

### Backend Entry Point

- Initializes Spring context
- Loads application.yml configuration
- Registers all beans (Controllers, Services, Repositories, Security filters)
- Starts embedded Tomcat on port 8080

### Frontend Entry Point

- Creates React root element
- Wraps App with `BrowserRouter` (enables routing)
- Wraps App with `AuthProvider` (enables auth context)
- Renders `<App />`

### Frontend Router Entry Point

- `Navbar` component (top navigation)
- `AppRouter` component (route definitions)
- `Footer` component (bottom section)

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

- Data never reaches backend
- Can't test backend integration
- Hides API errors
- Frontend changes won't reflect in real data

### Storing Sensitive Data in localStorage

- localStorage is vulnerable to XSS attacks
- No automatic expiration mechanism
- Shared across tabs unsecurely

### Authorization Checks Only in Frontend

### Mutating Response Objects

## Error Handling

- Backend throws custom exceptions (ResourceNotFoundException, IllegalArgumentException)
- Spring catches exceptions, returns appropriate HTTP status (404, 400, 401, 403, 500)
- Frontend handles errors in `.catch()` blocks, shows user-friendly messages
- Validation: `@Valid` annotations on DTO fields in controllers
- Not found: `ResourceNotFoundException` thrown in service layer
- Unauthorized: Spring Security returns 401, handled by frontend redirect to `/login`
- Bad request: `IllegalArgumentException` for business rule violations
- Global exception handler missing — should implement `@ControllerAdvice` for consistent error responses
- Frontend doesn't retry on network failures
- No error logging configured for backend

## Cross-Cutting Concerns

- Backend: SLF4J with Spring Boot default (logback), SQL formatted output enabled via `jpa.show-sql`
- Frontend: Browser console, no structured logging
- Backend: Hibernate Validator via `@Valid`, `@NotNull`, `@Email`, etc.
- Frontend: HTML5 input validation + manual checks before submission
- Backend: JWT token validation in `JwtAuthenticationFilter` before every request
- Frontend: Token presence check in `AuthContext`, redirect to login if missing
- Backend: `CorsConfigurationSource` in `SecurityConfig` restricts origins to `APP_CORS_ALLOWED_ORIGINS`
- Frontend: Axios client configured with default `baseURL` pointing to backend

<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
