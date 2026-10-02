---
last_mapped_commit: dedb4c0b09d0e3c7ee9c69ffe7438f1d6d56bd0a
last_mapped_at: 2026-10-02
---
# Coding Conventions

**Analysis Date:** 2026-10-02

## Backend (Java/Spring Boot)

### Naming Patterns

**Files:**
- Controllers: `*Controller.java` (e.g., `AuthController.java`)
- Services: `*Service.java` (e.g., `PublicacionService.java`)
- Entities: PascalCase, singular noun (e.g., `Usuario.java`, `Publicacion.java`)
- DTOs: Organized by feature in subdirectories under `dto/` (e.g., `dto/auth/LoginRequest.java`, `dto/publicacion/PublicacionResponse.java`)
- Repositories: `*Repository.java` (e.g., `UsuarioRepository.java`)
- Mappers: `*Mapper.java` (e.g., `PublicacionMapper.java`)
- Config classes: `*Config.java` (e.g., `SecurityConfig.java`, `CloudinaryConfig.java`)
- Exceptions: `*Exception.java` (e.g., `ResourceNotFoundException.java`)

**Classes:**
- PascalCase for all class names
- Use meaningful names describing responsibility
- Entity classes: singular nouns (`Usuario`, `Publicacion`, `Agencia`, `Consulta`, `Favorito`)

**Methods:**
- camelCase
- Action verbs: `crear()`, `actualizar()`, `obtener()`, `buscar()`, `guardar()`, `eliminar()`
- Service methods often mirror controller endpoints
- Mapper methods follow pattern: `toResponse()`, `toEntity()`
- Private helper methods use descriptive names: `construirRespuesta()`, `obtenerUsuarioAutenticado()`

**Variables:**
- camelCase for local variables and fields
- Use meaningful names: `usuarioAutenticado`, `publicacionResponse`, `agenciaId`
- Boolean variables with is/has prefixes: `isAdmin`, `hasErrors`

**Types/Enums:**
- PascalCase: `Rol`, `Combustible`, `Condicion`, `EstadoPublicacion`, `Transmision`
- Located in `enums/` directory
- Use English or Spanish consistently within same enum

### Code Style

**Formatting:**
- Inherited from Spring Boot starter parent (Maven enforces defaults)
- 4-space indentation
- UTF-8 encoding
- Line length: not strictly enforced but follow Spring conventions (~100 chars recommended)

**Linting:**
- No explicit linting tool configured (SpotBugs not present)
- Relies on IDE defaults and Spring Boot conventions
- Java 21 as target version in `pom.xml` property `<java.version>21</java.version>`

**Annotations:**
- Heavily used from Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Data`, `@RequiredArgsConstructor`
- Spring Framework: `@Service`, `@Repository`, `@Configuration`, `@Bean`, `@Value`, `@Autowired`, `@RequestMapping`, `@PostMapping`, `@GetMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- Jakarta Validation: `@NotNull`, `@NotBlank`, `@Positive`, `@Valid`
- JPA/Hibernate: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated`, `@PrePersist`, `@OneToMany`, `@ManyToOne`
- Security: `@EnableWebSecurity`, `@RestControllerAdvice`, `@ExceptionHandler`

### Import Organization

**Order:**
1. Java standard library imports (`java.io`, `java.time`, etc.)
2. Jakarta imports (`jakarta.persistence`, `jakarta.validation`, etc.)
3. Spring imports (all `org.springframework.*`)
4. Third-party libraries (Lombok, JJWT, Cloudinary, etc.)
5. Project-specific imports (com.danteautomotores.*)

**Example from `AuthController.java`:**

```java
package com.danteautomotores.controller;

import com.danteautomotores.dto.auth.AuthResponse;
import com.danteautomotores.dto.auth.LoginRequest;
import com.danteautomotores.dto.auth.RegistroRequest;
import com.danteautomotores.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
```

### Error Handling

**Approach:** Centralized exception handling via `GlobalExceptionHandler.java` with `@RestControllerAdvice`

**Patterns:**
- Use custom `ResourceNotFoundException` (extends `RuntimeException`) for not-found scenarios
- Use `IllegalArgumentException` for validation/business rule violations (e.g., "Ya existe una cuenta con ese email")
- Use Spring's `BadCredentialsException` for authentication failures
- Use `MethodArgumentNotValidException` for validation errors

**Exception Handler Mapping:**
- `ResourceNotFoundException` → HTTP 404 with JSON: `{"error": "message"}`
- `IllegalArgumentException` → HTTP 400 with JSON: `{"error": "message"}`
- `BadCredentialsException` → HTTP 401 with generic message
- `MethodArgumentNotValidException` → HTTP 400 with field errors: `{"fieldName": "validation message", ...}`

**Example from `PublicacionService.java` (line 55-56):**

```java
Agencia agencia = agenciaRepository.findById(request.getAgenciaId())
    .orElseThrow(() -> new ResourceNotFoundException("No existe una agencia con id: " + request.getAgenciaId()));
```

### Logging

**Framework:** Not explicitly configured; uses Spring default (SLF4J)

**Patterns:**
- No logger instances found in codebase
- Configuration in `application.yml`: `show-sql: true` and `format_sql: true` for SQL logging
- SQL logging for debugging during development

### Comments

**When to Comment:**
- Explain business logic not obvious from code
- Document assumptions (e.g., "el registro público siempre crea compradores; los admins se cargan aparte")
- Explain why, not what

**JSDoc/JavaDoc:**
- Not systematically used in codebase
- Consider adding for public API methods

**Example from `AuthService.java` (line 35):**

```java
// el registro público siempre crea compradores; los admins se cargan aparte
.rol(Rol.COMPRADOR)
```

### Function Design

**Size:**
- Methods range from 2 lines (simple getters/setters via Lombok) to ~70 lines (PublicacionService methods)
- Recommended: Keep methods under 30 lines where possible
- Use helper methods for complex logic

**Parameters:**
- Use DTOs for request objects instead of multiple primitives
- Example: `crearAgencia(AgenciaRequest request)` not `crearAgencia(String nombre, String email, ...)`
- Use `@Valid` annotation on DTO parameters for automatic validation

**Return Values:**
- Services return DTOs (Response objects) not entities
- Controllers wrap service responses in `ResponseEntity<T>` for HTTP status control
- Use `.orElseThrow()` for repository operations to fail fast on missing entities

**Example from `PublicacionService.buscar()` (lines 36-47):**

```java
public List<PublicacionResponse> buscar(String marca, String modelo, Integer anioMin, Integer anioMax,
                                        BigDecimal precioMin, BigDecimal precioMax,
                                        EstadoPublicacion estado, Long agenciaId) {
    EstadoPublicacion estadoFiltro = estado != null ? estado : EstadoPublicacion.DISPONIBLE;
    
    return publicacionRepository
            .findAll(PublicacionSpecification.conFiltros(marca, modelo, anioMin, anioMax, precioMin, precioMax, estadoFiltro, agenciaId))
            .stream()
            .map(PublicacionMapper::toResponse)
            .toList();
}
```

### Module Design

**Package Structure:**

```
src/main/java/com/danteautomotores/
├── config/          # Configuration beans
├── controller/      # REST endpoints
├── dto/            # Data Transfer Objects (organized by feature)
├── entity/         # JPA entities
├── enums/          # Enumeration types
├── exception/      # Custom exceptions
├── mapper/         # Entity to DTO mappers
├── repository/     # Spring Data repositories (with specs subdirectory)
├── security/       # Security-related classes (JWT, filters)
├── service/        # Business logic
└── DanteAutomotoresApplication.java  # Main application class
```

**Exports/Visibility:**
- Controllers expose REST endpoints via `@RestController` and request mapping annotations
- Services expose business logic methods (public)
- Repositories extended from Spring Data `JpaRepository<T, ID>`
- DTOs are request/response contracts
- Mappers use public static methods for stateless conversion

**Example Mapper Pattern from `PublicacionMapper.java`:**

```java
public class PublicacionMapper {
    private PublicacionMapper() {}  // Utility class, non-instantiable
    
    public static PublicacionResponse toResponse(Publicacion publicacion) {
        // conversion logic
    }
}
```

---

## Frontend (React/Vite)

### Naming Patterns

**Files:**
- Components: PascalCase with `.jsx` extension (e.g., `PublicacionCard.jsx`, `Navbar.jsx`)
- Pages: PascalCase with `.jsx` extension (e.g., `HomePage.jsx`, `LoginPage.jsx`)
- Contexts: PascalCase with `.jsx` extension (e.g., `AuthContext.jsx`)
- Services/utilities: camelCase with `.js` extension (e.g., `api.js`, `whatsapp.js`)
- Mocks: camelCase with `Mock.js` suffix (e.g., `catalogoMock.js`, `homeMock.js`)

**Components:**
- PascalCase: `PublicacionCard`, `SeccionConfianza`, `AdminDashboardPage`, `ProtectedRoute`
- Descriptive names reflecting purpose
- Suffix with `Page` for page/route components
- Suffix with `Accordion`, `Card`, `Form` for reusable components

**Functions/Variables:**
- camelCase: `formatoNumero()`, `sugerencias`, `cargando`, `handleSubmit()`
- Hook names start with `use`: `useAuth()`, `useEffect()`, `useState()`, `useMemo()`
- Event handlers: `handle*` prefix (e.g., `handleSubmit`, `handleClick`, `elegirSugerencia`)
- Constants in UPPERCASE: `USE_MOCK_DATA`, `ESTADO_BADGE`, `BENEFICIOS`

**Props/State:**
- camelCase for prop names and state variables
- Descriptive: `publicacion`, `mostrarPassword`, `sugerenciasAbiertas`, `cargando`

### Code Style

**Formatting:**
- No `.prettierrc` configured; uses IDE defaults
- ESLint configured with `npm run lint: eslint .`
- Vite with React plugin for Hot Module Replacement

**Linting:**
- ESLint enabled: `"lint": "eslint ."` in package.json
- No specific config file found; uses default ESLint + React rules
- JSX syntax via @vitejs/plugin-react

**Conventions Observed:**
- 2-space indentation (common React convention)
- Semicolons used consistently
- String interpolation with backticks for template literals
- Destructuring imports: `import { useState, useEffect } from 'react'`

### Import Organization

**Order:**
1. React and core dependencies (`react`, `react-dom`, `react-router-dom`)
2. Icon libraries (`lucide-react`)
3. API/service imports (`api`, custom hooks)
4. Components and pages
5. Utilities and mocks
6. Styles

**Example from `HomePage.jsx` (lines 1-10):**

```javascript
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, Check, Search, Sparkles } from 'lucide-react'
import api from '../services/api.js'
import PublicacionCard from '../components/PublicacionCard.jsx'
import SeccionConfianza from '../components/SeccionConfianza.jsx'
import SeccionFinanciamiento from '../components/SeccionFinanciamiento.jsx'
import LogoMarca, { LOGOS } from '../components/LogoMarca.jsx'
import { destacadosMock } from '../mocks/homeMock.js'
import { BANDAS_PRECIO, catalogoMock } from '../mocks/catalogoMock.js'
```

**Path Aliases:**
- Not configured; uses relative imports (`../components/`, `../services/`)

### Error Handling

**Patterns:**
- Try-catch in async handlers with `.catch()` fallbacks
- Error state via `useState('')`
- Display errors in UI with conditional rendering

**Example from `LoginPage.jsx` (lines 19-31):**

```javascript
const handleSubmit = async (e) => {
  e.preventDefault()
  setError('')
  setCargando(true)
  try {
    await login(email, password)
    navigate('/')
  } catch {
    setError('Email o contraseña incorrectos')
  } finally {
    setCargando(false)
  }
}
```

**API Error Handling in `AuthContext.jsx`:**
- Service methods throw errors; components catch them
- No global error boundary observed
- Each page/component manages its own error state

### Logging

**Framework:** `console` (browser DevTools)

**Patterns:**
- No explicit logging found in codebase
- Use browser console for debugging

### Comments

**When to Comment:**
- Explain business logic (e.g., default filter behavior)
- Mark TODO items for future work
- Document mock data usage

**TODOs Found:**
- `src/pages/HomePage.jsx` (line 12-14): Mock data toggle with TODO to remove when backend ready
- `src/pages/LoginPage.jsx` (line 33): Google login integration TODO
- Multiple pages have `.catch(() => {})` with empty handlers (catch-all for non-critical API failures)

**Example from `HomePage.jsx`:**

```javascript
// TODO: sacar esto cuando el backend esté levantado y probado.
// Mientras USE_MOCK_DATA sea true, el Home ignora la API real y muestra
// datos hardcodeados solo para previsualizar el diseño.
const USE_MOCK_DATA = true
```

### Function Design

**Size:**
- Page components range from 50-580 lines (HomePages is 280 lines)
- Component functions are compact and focused
- Extract complex rendering into subcomponents

**Parameters:**
- Props destructured in function signature: `export default function PublicacionCard({ publicacion })`
- Use descriptive prop names

**Return Values:**
- Components return JSX elements
- Handlers return void (side effects via setState)
- Custom hooks return state/functions

**Example from `PublicacionCard.jsx` (lines 10-22):**

```javascript
export default function PublicacionCard({ publicacion }) {
  const foto = publicacion.fotos?.[0]?.url
  const estadoBadge = ESTADO_BADGE[publicacion.estado]

  const specs = [
    publicacion.anio,
    publicacion.kilometraje != null ? `${formatoNumero(publicacion.kilometraje)} km` : null,
    publicacion.mecanica,
  ].filter(Boolean)
  
  return (/* JSX */)
}
```

### Module Design

**Directory Structure:**

```
danteautomotores-front/src/
├── components/      # Reusable components
├── pages/          # Page components (routable)
│   └── admin/      # Admin-only pages
├── context/        # React Context providers
├── routes/         # Routing configuration
├── services/       # API service
├── utils/          # Utility functions
├── mocks/          # Mock data for development
├── App.jsx         # Root component
├── main.jsx        # Entry point
└── index.css       # Global styles (Tailwind)
```

**Exports/Visibility:**
- Components export as default: `export default function ComponentName() {}`
- Context exports both provider and hook: `export function AuthProvider()` and `export function useAuth()`
- Mock data exports named exports for flexibility

**Context Pattern from `AuthContext.jsx`:**

```javascript
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // provider logic
}

export function useAuth() {
  return useContext(AuthContext)
}
```

### Styling

**Framework:** Tailwind CSS via `@tailwindcss/vite`

**Patterns:**
- Utility-first: `className="flex items-center gap-2 rounded-xl border..."`
- Color system: `text-bronze`, `bg-navy`, `text-slate-400`, `text-white/90`
- Responsive: `md:grid-cols-2`, `lg:px-10`, `sm:flex`
- Hover/interactive: `hover:bg-bronze`, `hover:text-white`, `focus-within:border-bronze`
- Animations: `animate-fade-in-up`, `animate-marquee`
- Custom config in `tailwind.config.*` (if present)

---

## Language & Terminology

**Note:** Codebase uses Spanish for:
- User-facing messages
- Error messages
- Comments and documentation
- Database table names and field names
- Enum values

**Examples:**
- Database tables: `usuarios`, `publicaciones`, `agencias`, `consultas`, `favoritos`
- Entity classes: `Usuario`, `Publicacion`, `Agencia`, `Consulta`, `Favorito`
- DTOs: `RegistroRequest`, `LoginRequest`, `PublicacionRequest`
- Messages: "Ya existe una cuenta con ese email", "Email o contraseña incorrectos"

---

*Convention analysis: 2026-10-02*
