---
last_mapped_commit: dedb4c0b09d0e3c7ee9c69ffe7438f1d6d56bd0a
last_mapped_at: 2026-10-02
---
# Codebase Structure

**Analysis Date:** 2026-10-02

## Directory Layout

### Backend (Java/Spring Boot)

```
danteautomotores-back/
├── src/
│   ├── main/
│   │   ├── java/com/danteautomotores/
│   │   │   ├── controller/           # REST API endpoints
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── PublicacionController.java
│   │   │   │   ├── AgenciaController.java
│   │   │   │   ├── ConsultaController.java
│   │   │   │   └── FavoritoController.java
│   │   │   ├── service/              # Business logic layer
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── PublicacionService.java
│   │   │   │   ├── AgenciaService.java
│   │   │   │   ├── ConsultaService.java
│   │   │   │   ├── FavoritoService.java
│   │   │   │   ├── CloudinaryService.java
│   │   │   │   └── JwtService.java
│   │   │   ├── repository/           # Data access layer (JPA)
│   │   │   │   ├── UsuarioRepository.java
│   │   │   │   ├── PublicacionRepository.java
│   │   │   │   ├── AgenciaRepository.java
│   │   │   │   ├── ConsultaRepository.java
│   │   │   │   ├── FavoritoRepository.java
│   │   │   │   ├── FotoPublicacionRepository.java
│   │   │   │   └── spec/
│   │   │   │       └── PublicacionSpecification.java  # Dynamic query builder
│   │   │   ├── entity/               # JPA entities (database models)
│   │   │   │   ├── Usuario.java
│   │   │   │   ├── Publicacion.java
│   │   │   │   ├── Agencia.java
│   │   │   │   ├── Consulta.java
│   │   │   │   ├── Favorito.java
│   │   │   │   └── FotoPublicacion.java
│   │   │   ├── dto/                  # Data Transfer Objects (API contracts)
│   │   │   │   ├── auth/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RegistroRequest.java
│   │   │   │   │   └── AuthResponse.java
│   │   │   │   ├── publicacion/
│   │   │   │   │   ├── PublicacionRequest.java
│   │   │   │   │   ├── PublicacionResponse.java
│   │   │   │   │   ├── FotoResponse.java
│   │   │   │   │   └── CambiarEstadoRequest.java
│   │   │   │   ├── agencia/
│   │   │   │   ├── consulta/
│   │   │   │   └── favorito/
│   │   │   ├── mapper/               # Entity ↔ DTO conversion
│   │   │   │   └── PublicacionMapper.java
│   │   │   ├── enums/                # Type enumerations
│   │   │   │   ├── Rol.java
│   │   │   │   ├── EstadoPublicacion.java
│   │   │   │   ├── Transmision.java
│   │   │   │   ├── Combustible.java
│   │   │   │   └── Condicion.java
│   │   │   ├── security/             # JWT and auth logic
│   │   │   │   ├── JwtService.java
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   ├── config/               # Spring configuration
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── CloudinaryConfig.java
│   │   │   ├── exception/            # Custom exceptions
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   └── DanteAutomotoresApplication.java  # Main entry point
│   │   └── resources/
│   │       └── application.yml       # Spring Boot configuration
│   └── test/                         # Test classes (JUnit, Spring Security Test)
├── pom.xml                           # Maven dependencies and build config
├── docker-compose.yml                # PostgreSQL + pgAdmin setup
├── .planning/
│   └── codebase/                     # Architecture documentation
│       ├── ARCHITECTURE.md
│       └── STRUCTURE.md
├── .git/                             # Version control
└── target/                           # Build output (Maven)
```

### Frontend (React + Vite)

```
danteautomotores-front/
├── src/
│   ├── pages/                    # Route components (full pages)
│   │   ├── HomePage.jsx
│   │   ├── AutosPage.jsx         # Listings with filters
│   │   ├── PublicacionDetallePage.jsx  # Single listing detail
│   │   ├── AgenciaPage.jsx       # Agency detail page
│   │   ├── LoginPage.jsx         # User login
│   │   ├── RegistroPage.jsx      # User registration
│   │   ├── FavoritosPage.jsx     # User saved listings (protected)
│   │   └── admin/                # Admin-only pages
│   │       ├── AdminDashboardPage.jsx
│   │       └── AdminPublicacionFormPage.jsx
│   ├── components/               # Reusable UI components
│   │   ├── Navbar.jsx            # Top navigation
│   │   ├── Footer.jsx            # Bottom section
│   │   ├── PublicacionCard.jsx   # Listing card component
│   │   ├── FiltroAcordeon.jsx    # Collapsible filter section
│   │   ├── ProtectedRoute.jsx    # Auth/role gating
│   │   ├── Logo.jsx
│   │   ├── LogoMarca.jsx         # Brand logos
│   │   ├── ScrollToTop.jsx       # Scroll behavior
│   │   ├── SeccionConfianza.jsx  # UI section
│   │   ├── SeccionFinanciamiento.jsx
│   │   ├── BotonFlotanteWhatsapp.jsx
│   │   └── IconoGoogle.jsx
│   ├── context/                  # React Context providers
│   │   └── AuthContext.jsx       # User auth state (login, logout, role)
│   ├── services/                 # API service layer
│   │   └── api.js                # Axios HTTP client with JWT interceptor
│   ├── routes/
│   │   └── AppRouter.jsx         # Route definitions
│   ├── mocks/                    # Development mock data
│   │   └── catalogoMock.js       # Sample car listings (temporary)
│   ├── utils/                    # Utility functions
│   ├── App.jsx                   # Main app component (layout)
│   ├── main.jsx                  # React entry point
│   └── index.css                 # Global styles (TailwindCSS)
├── public/                       # Static assets
├── package.json                  # npm dependencies and scripts
├── vite.config.js                # Vite bundler config
├── tailwind.config.js            # TailwindCSS config
└── .env.local                    # Local environment variables (VITE_API_URL)
```

## Directory Purposes

### Backend

**`src/main/java/com/danteautomotores/`**
- Location of all Java source code
- Organized by layer (controller → service → repository)
- Each layer has clear responsibility with no upward dependencies

**`src/main/java/com/danteautomotores/controller/`**
- REST API endpoint definitions
- Maps HTTP methods to service methods
- Validates input via `@Valid` annotation
- Coordinates request/response transformation

**`src/main/java/com/danteautomotores/service/`**
- Implements business logic and workflows
- Manages transactions with `@Transactional`
- Orchestrates repository calls
- Applies domain rules (e.g., no duplicate email registration)
- Converts between entities and DTOs

**`src/main/java/com/danteautomotores/repository/`**
- Spring Data JPA interfaces
- Extends `JpaRepository` and `JpaSpecificationExecutor` for query building
- `spec/PublicacionSpecification.java` enables dynamic filtering
- No business logic — pure data access

**`src/main/java/com/danteautomotores/entity/`**
- JPA entity classes with `@Entity` annotation
- Map 1:1 to database tables
- Use Lombok (`@Getter`, `@Setter`, `@Builder`) to reduce boilerplate
- Define relationships (e.g., Publicacion.agencia is `@ManyToOne`)
- Include `@PrePersist` hooks for timestamps

**`src/main/java/com/danteautomotores/dto/`**
- Request/Response objects for API contracts
- Separate from entities to allow API evolution independently
- Use builders for object creation
- Includes validation annotations (`@NotNull`, `@Email`)
- Organized in subdirectories by domain (auth/, publicacion/, etc.)

**`src/main/java/com/danteautomotores/security/`**
- `JwtService.java` — Generate, parse, validate JWT tokens
- `JwtAuthenticationFilter.java` — Intercept requests, extract token, validate signature

**`src/main/java/com/danteautomotores/config/`**
- `SecurityConfig.java` — Configures Spring Security filter chain, authorization rules, CORS
- `CloudinaryConfig.java` — Initializes Cloudinary SDK with credentials from environment

**`src/main/resources/application.yml`**
- Spring Boot configuration
- Database connection details (PostgreSQL URL, credentials)
- JWT settings (secret, expiration time)
- CORS allowed origins
- File upload size limits
- Properties use environment variables with fallback defaults (e.g., `${PORT:8080}`)

### Frontend

**`danteautomotores-front/src/pages/`**
- Full-page components corresponding to routes
- Each file exports a component for one route
- Pages manage their own state (useState for filters, forms, loading)
- Pages call API services or use context (AuthContext)
- Admin pages wrapped in ProtectedRoute for role-based access

**`danteautomotores-front/src/components/`**
- Reusable UI building blocks
- Stateless or minimal state (local UI state only)
- Receive data and callbacks via props
- Examples: PublicacionCard (renders a single listing), Navbar (top nav), ProtectedRoute (conditionally renders children)

**`danteautomotores-front/src/context/AuthContext.jsx`**
- Provides global user session state
- Stores JWT token in localStorage
- Exposes: `usuario`, `esAdmin`, `login()`, `registrar()`, `logout()`
- Consumed by pages and ProtectedRoute

**`danteautomotores-front/src/services/api.js`**
- Axios instance with base URL pointing to backend
- Axios interceptor automatically adds JWT token to request headers: `Authorization: Bearer {token}`
- Used by all pages to call `/publicaciones`, `/auth`, `/consultas`, etc.
- Handles JWT refresh (placeholder for future implementation)

**`danteautomotores-front/src/routes/AppRouter.jsx`**
- React Router `<Routes>` definition
- Each `<Route>` maps path to page component
- Protected routes use `<ProtectedRoute>` wrapper
- Routes include: `/`, `/autos`, `/publicaciones/:id`, `/login`, `/registro`, `/favoritos`, `/admin/*`

**`danteautomotores-front/src/mocks/catalogoMock.js`**
- Temporary hardcoded car listing data
- Used when `USE_MOCK_DATA = true` in pages
- Contains ~30 sample vehicles with all fields (marca, modelo, precio, fotos, etc.)
- Allows frontend development without backend
- TODO: Remove when backend is fully integrated

## Key File Locations

**Backend Entry Points:**
- `src/main/java/com/danteautomotores/DanteAutomotoresApplication.java` — Main class, runs Spring Boot

**Backend Configuration:**
- `src/main/resources/application.yml` — Datasource, JWT, CORS, Cloudinary config
- `src/main/java/com/danteautomotores/config/SecurityConfig.java` — Spring Security rules
- `pom.xml` — Maven dependencies

**Frontend Entry Points:**
- `danteautomotores-front/src/main.jsx` — React app initialization
- `danteautomotores-front/src/App.jsx` — Root component with Navbar, Router, Footer
- `danteautomotores-front/src/routes/AppRouter.jsx` — Route definitions

**Core Logic - Backend:**
- `src/main/java/com/danteautomotores/service/AuthService.java` — User registration/login logic
- `src/main/java/com/danteautomotores/service/PublicacionService.java` — Listing CRUD and search
- `src/main/java/com/danteautomotores/security/JwtAuthenticationFilter.java` — Token validation on every request

**Core Logic - Frontend:**
- `danteautomotores-front/src/context/AuthContext.jsx` — User session management
- `danteautomotores-front/src/pages/AutosPage.jsx` — Listing search and filtering
- `danteautomotores-front/src/pages/PublicacionDetallePage.jsx` — Single listing detail and inquiry form

**Testing - Backend:**
- `src/test/java/com/danteautomotores/` — Spring Boot tests with `@SpringBootTest`, MockMvc, etc.

**Testing - Frontend:**
- Not yet implemented (recommended: Jest + React Testing Library or Vitest)

## Naming Conventions

### Backend Naming

**Files:**
- Class names: PascalCase, e.g., `PublicacionController.java`, `AuthService.java`
- File name matches class name
- Package structure mirrors domain: `...controller`, `...service`, `...repository`

**Classes:**
- Controllers: `[Domain]Controller`, e.g., `PublicacionController`
- Services: `[Domain]Service`, e.g., `AuthService`
- Repositories: `[Entity]Repository`, e.g., `UsuarioRepository`
- DTOs: `[Domain][Operation]Request/Response`, e.g., `LoginRequest`, `PublicacionResponse`
- Entities: Singular noun, e.g., `Usuario`, `Publicacion`, `Agencia`

**Methods:**
- Controller methods: HTTP verb + purpose, e.g., `buscar()`, `obtenerPorId()`, `crear()`, `actualizar()`, `eliminar()`
- Service methods: imperative verbs, e.g., `registrar()`, `login()`, `cambiarEstado()`
- Repository methods: passive finders, e.g., `findByEstado()`, `findByAgenciaIdAndEstado()`

**Variables:**
- camelCase: `publicacionId`, `usuarioActivo`, `fotoUrl`
- Enum values: SCREAMING_SNAKE_CASE, e.g., `EstadoPublicacion.DISPONIBLE`
- Private fields: camelCase with `private` modifier

**Constants:**
- SCREAMING_SNAKE_CASE: `MAX_FILE_SIZE`, `JWT_EXPIRATION_MS`
- Enum constants: `DISPONIBLE`, `RESERVADO`, `VENDIDO`

### Frontend Naming

**Files:**
- Component files: PascalCase `.jsx`, e.g., `PublicacionCard.jsx`, `LoginPage.jsx`
- Service/utility files: camelCase `.js`, e.g., `api.js`, `catalogoMock.js`
- Context files: PascalCase `.jsx`, e.g., `AuthContext.jsx`
- Page files: PascalCase ending in `Page`, e.g., `HomePage.jsx`, `AutosPage.jsx`

**Components:**
- Function components: PascalCase, e.g., `export default function PublicacionCard() { ... }`
- Props use camelCase: `publicacion`, `onToggle`, `isLoading`
- State variables: camelCase, e.g., `const [busqueda, setBusqueda] = useState('')`

**Variables:**
- camelCase: `usuarioActivo`, `fotoUrl`, `precioMin`, `marcasActivas`
- Boolean variables/props: prefix `is`, `has`, `can`, e.g., `isLoading`, `hasError`, `canEdit`
- Arrays plural: `fotos`, `comentarios`, `ubicacionesActivas`
- Functions: camelCase starting with verb, e.g., `handleSubmit()`, `toggleMarca()`, `formatoNumero()`

**Constants:**
- SCREAMING_SNAKE_CASE for configuration constants: `USE_MOCK_DATA`, `ESTADO_LABELS`, `MAX_FILE_SIZE`
- camelCase for computed values

## Where to Add New Code

### New Backend Feature (e.g., Reviews/Ratings)

**Implementation:**
1. Create entity: `src/main/java/com/danteautomotores/entity/Resena.java`
2. Create repository: `src/main/java/com/danteautomotores/repository/ResenaRepository.java`
3. Create DTOs: `src/main/java/com/danteautomotores/dto/resena/ResenaRequest.java`, `ResenaResponse.java`
4. Create service: `src/main/java/com/danteautomotores/service/ResenaService.java` — add business logic
5. Create controller: `src/main/java/com/danteautomotores/controller/ResenaController.java` — expose endpoints
6. Update `SecurityConfig.java` to add authorization rules for review endpoints
7. Create mapper (if needed): `src/main/java/com/danteautomotores/mapper/ResenaMapper.java`

**Tests:**
- `src/test/java/com/danteautomotores/service/ResenaServiceTest.java`
- `src/test/java/com/danteautomotores/controller/ResenaControllerTest.java`

**Database:**
- If adding fields to existing entity, JPA auto-migration handles it (ddl-auto: update)
- If creating new table, add `@Entity @Table(name="resenas")` to entity class

### New Frontend Page (e.g., User Profile)

**Implementation:**
1. Create page component: `danteautomotores-front/src/pages/ProfilePage.jsx`
2. Add route to `danteautomotores-front/src/routes/AppRouter.jsx`: `<Route path="/perfil" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />`
3. Create necessary components in `danteautomotores-front/src/components/` if reusable
4. Page calls API via `api.js`: `api.get('/usuarios/perfil')`, `api.patch('/usuarios', {...})`
5. Add navigation link in `danteautomotores-front/src/components/Navbar.jsx`

**State Management:**
- If shared state needed (e.g., user data), add to `AuthContext.jsx`
- Otherwise, use `useState()` within page component

**Styling:**
- Use TailwindCSS utility classes (already configured)
- Consistent with existing color scheme (navy, bronze, slate)

### New Admin Feature (e.g., User Management)

**Backend:**
1. Create entity: `src/main/java/com/danteautomotores/entity/[Entity].java`
2. Create repository, service, controller following pattern above
3. In `SecurityConfig.java` (line 47-50), restrict endpoint to `hasRole("ADMIN")`

**Frontend:**
1. Create admin page: `danteautomotores-front/src/pages/admin/AdminUsersPage.jsx`
2. Wrap in `<ProtectedRoute soloAdmin>` in AppRouter
3. Add navigation in admin dashboard

### New Service Integration (e.g., Payment Gateway)

**File:** `src/main/java/com/danteautomotores/service/PaymentService.java`

**Pattern:**
- Create service class with `@Service`
- Inject payment SDK via Spring `@Bean` in config file
- Implement payment logic (charge, refund, validation)
- Inject from controllers/other services using `@RequiredArgsConstructor`
- Config credentials via environment variables in `application.yml`

**Example:** Cloudinary integration exists in `CloudinaryService.java` and `CloudinaryConfig.java`

## Special Directories

**`.planning/codebase/`**
- Purpose: Architecture and structure documentation
- Generated: No (manually maintained)
- Committed: Yes (shared across team)
- Contents: ARCHITECTURE.md, STRUCTURE.md, and future docs

**`src/test/`**
- Purpose: Unit and integration tests
- Generated: No (developer-written)
- Committed: Yes (required for CI/CD)
- Run with: `mvn test`

**`danteautomotores-front/node_modules/`**
- Purpose: npm dependencies
- Generated: Yes (by `npm install`)
- Committed: No (.gitignore excludes it)
- Install with: `npm install`

**`danteautomotores-front/dist/`** or **`build/`**
- Purpose: Production build output
- Generated: Yes (by `npm run build`)
- Committed: No (regenerated on deployment)
- Build with: `npm run build`

**`target/`** (Backend)
- Purpose: Maven build output
- Generated: Yes (by `mvn clean package`)
- Committed: No (.gitignore excludes it)
- Build with: `mvn clean package`

**`src/main/resources/`**
- application.yml: Configuration for each environment (dev, test, prod)
- Static files: Could hold frontend files if needed
- Properties files: Could hold encrypted secrets (currently uses environment variables)

**`.env.local`** (Frontend)
- Purpose: Local environment variables (VITE_API_URL, etc.)
- Generated: No (developer-created)
- Committed: No (.gitignore should exclude it)
- Provides: Backend API URL for local/dev environment

---

*Structure analysis: 2026-10-02*
