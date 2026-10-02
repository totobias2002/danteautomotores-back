---
last_mapped_commit: dedb4c0b09d0e3c7ee9c69ffe7438f1d6d56bd0a
last_mapped_at: 2026-10-02
---
# Testing Patterns

**Analysis Date:** 2026-10-02

## Backend (Java/Spring Boot)

### Test Framework

**Status:** Test infrastructure configured but **NO test files present in codebase**

**Available Dependencies (from pom.xml):**
- Runner: Spring Boot Test (embedded in `spring-boot-starter-test`)
- Assertion: JUnit 5 (Jupiter)
- Mocking: Mockito (included in starter-test)
- Security Testing: `spring-security-test`

**Run Commands (available but no tests to run):**

```bash
mvn test                    # Run all tests (would run if tests exist)
mvn test -Dtest=ClassName  # Run specific test class (if exists)
mvn clean test              # Clean and run tests
```

**Maven Configuration:**

```xml
<!-- pom.xml dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Test Organization

**Location:** Not implemented (would be `src/test/java/com/danteautomotores/...`)

**Recommended Pattern:**
- Mirror source package structure in `src/test/`
- Example: Service in `src/main/java/com/danteautomotores/service/AuthService.java`
- Test in `src/test/java/com/danteautomotores/service/AuthServiceTest.java`

**Naming:** (Convention, not yet applied)
- Classes: `*Test.java` or `*Tests.java` (e.g., `AuthServiceTest.java`)

**Structure (To Be Implemented):**

```
src/test/java/com/danteautomotores/
├── service/
│   ├── AuthServiceTest.java
│   ├── PublicacionServiceTest.java
│   └── ...
├── controller/
│   ├── AuthControllerTest.java
│   ├── PublicacionControllerTest.java
│   └── ...
└── integration/
    └── AuthIntegrationTest.java
```

### Test Structure

**Recommended Setup (no tests currently exist):**

For unit tests in services, follow this pattern:

```java
@SpringBootTest
class AuthServiceTest {
    
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @InjectMocks
    private AuthService authService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testRegistrar() {
        // Arrange
        RegistroRequest request = new RegistroRequest(...);
        
        // Act
        AuthResponse response = authService.registrar(request);
        
        // Assert
        assertNotNull(response);
    }
}
```

**Patterns to Use:**
- **Setup:** Use `@BeforeEach` for test initialization
- **Teardown:** Use `@AfterEach` if needed for cleanup
- **Assertions:** Use JUnit 5 assertions: `assertEquals()`, `assertNotNull()`, `assertThrows()`, `assertTrue()`
- **Arrange-Act-Assert (AAA):** Follow this pattern in each test method
- **Test naming:** Describe what is tested: `testRegistrarConEmailExistente()`, `testBuscarPorIdNoEncontrado()`

### Mocking

**Framework:** Mockito (included in spring-boot-starter-test)

**Recommended Pattern:**

```java
@Mock
private UsuarioRepository usuarioRepository;

@Mock
private PasswordEncoder passwordEncoder;

@InjectMocks
private AuthService authService;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}

@Test
void testLoginWithBadCredentials() {
    // Arrange
    when(usuarioRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(new Usuario(...)));
    
    // Act & Assert
    assertThrows(BadCredentialsException.class, () -> {
        authService.login(new LoginRequest("test@example.com", "wrong"));
    });
}
```

**What to Mock:**
- Repository dependencies
- External service integrations (CloudinaryService, JwtService)
- Password encoder (PasswordEncoder)
- Authentication manager

**What NOT to Mock:**
- Entity builders and domain logic
- DTO construction
- Mapper logic (these should be simple transformations)

### Fixtures and Factories

**Test Data (To Be Implemented):**

Create factory methods for common test objects:

```java
class TestFixtures {
    public static Usuario crearUsuario() {
        return Usuario.builder()
            .id(1L)
            .nombre("Test Usuario")
            .email("test@example.com")
            .passwordHash("hashed_password")
            .rol(Rol.COMPRADOR)
            .fechaRegistro(LocalDateTime.now())
            .build();
    }
    
    public static Publicacion crearPublicacion() {
        return Publicacion.builder()
            .id(1L)
            .marca("Toyota")
            .modelo("Corolla")
            .anio(2020)
            .precio(new BigDecimal("15000"))
            // ... other fields
            .build();
    }
}
```

**Location (To Be Created):**
- `src/test/java/com/danteautomotores/util/TestFixtures.java`

### Coverage

**Requirements:** Not enforced (no coverage plugin configured)

**Recommended Target:** 70%+ for services and controllers, 50%+ overall

**To Enable Coverage (JaCoCo):**

```xml
<!-- Add to pom.xml plugins section -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**View Coverage:**

```bash
mvn clean test

# Report generated at target/site/jacoco/index.html

```

### Test Types

**Unit Tests:**
- Scope: Individual service methods
- Approach: Mock repositories and external dependencies
- Example: Test `AuthService.registrar()` with mocked `UsuarioRepository`
- File location: `src/test/java/com/danteautomotores/service/*Test.java`

**Integration Tests:**
- Scope: Service + Repository + Database (with H2 in-memory DB for tests)
- Approach: Use `@SpringBootTest` with test database
- Example: Test `PublicacionService.buscar()` with actual specifications and database queries
- File location: `src/test/java/com/danteautomotores/integration/*IntegrationTest.java`

**Controller/API Tests (Recommended):**
- Scope: HTTP endpoints with mock service layer
- Approach: Use `@WebMvcTest` or `@SpringBootTest` with `MockMvc`
- Example: Test `AuthController.login()` returns 401 on invalid credentials
- File location: `src/test/java/com/danteautomotores/controller/*ControllerTest.java`

**Security Tests:**
- Use `spring-security-test` with `@WithMockUser` for authenticated requests
- Test authorization rules defined in `SecurityConfig`

### Common Patterns

**Async Testing:**
- Use `@SpringBootTest` with real async execution
- Use `CompletableFuture` or reactive types if implemented
- Not currently present in this codebase

**Error Testing:**

```java
@Test
void testCrearPublicacionSinAgencia() {
    // Arrange
    PublicacionRequest request = new PublicacionRequest();
    request.setAgenciaId(999L); // Non-existent
    
    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> {
        publicacionService.crear(request);
    });
}
```

**Database Tests (Integration):**

```java
@SpringBootTest
@Transactional
class PublicacionRepositoryTest {
    
    @Autowired
    private PublicacionRepository repository;
    
    @Test
    void testFindByMarcaAndModelo() {
        // Setup with testcontainers or H2
        // Test query logic
    }
}
```

---

## Frontend (React/Vite)

### Test Framework

**Status:** Testing infrastructure NOT configured, **NO test files present in codebase**

**Recommended Setup:**

Neither Jest nor Vitest is installed. To add testing:

```bash
npm install --save-dev vitest @testing-library/react @testing-library/jest-dom
```

**Configuration (To Be Created - vitest.config.js):**

```javascript
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
    },
  },
})
```

**Run Commands (Once Configured):**

```bash
npm run test                 # Run all tests
npm run test -- --watch     # Watch mode
npm run test -- --coverage  # Generate coverage report
```

### Test Organization

**Recommended Location:**
- `src/__tests__/` (Vitest convention)
- Or `src/components/__tests__/` (co-located)

**Naming Convention:**
- `*.test.jsx` or `*.spec.jsx` (e.g., `PublicacionCard.test.jsx`)

**Recommended Structure:**

```
src/
├── __tests__/
│   ├── components/
│   │   ├── PublicacionCard.test.jsx
│   │   ├── ProtectedRoute.test.jsx
│   │   └── ...
│   ├── pages/
│   │   ├── HomePage.test.jsx
│   │   ├── LoginPage.test.jsx
│   │   └── ...
│   ├── services/
│   │   └── api.test.js
│   └── context/
│       └── AuthContext.test.jsx
```

### Test Structure

**Recommended Setup (Using Vitest + React Testing Library):**

```javascript
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PublicacionCard from '../PublicacionCard.jsx'

describe('PublicacionCard', () => {
  const mockPublicacion = {
    id: 1,
    marca: 'Toyota',
    modelo: 'Corolla',
    anio: 2020,
    precio: 15000,
    fotos: [],
    estado: 'DISPONIBLE',
  }

  it('renders car title', () => {
    render(<PublicacionCard publicacion={mockPublicacion} />)
    expect(screen.getByText('Toyota • Corolla')).toBeInTheDocument()
  })

  it('displays price formatted as number', () => {
    render(<PublicacionCard publicacion={mockPublicacion} />)
    expect(screen.getByText('$ 15.000')).toBeInTheDocument()
  })

  it('shows "Sin foto" when no images', () => {
    render(<PublicacionCard publicacion={mockPublicacion} />)
    expect(screen.getByText('Sin foto')).toBeInTheDocument()
  })
})
```

**Patterns to Use:**
- **Setup:** No specific setup pattern yet; use `beforeEach()` for common setup
- **Teardown:** Vitest handles cleanup automatically
- **Assertions:** Use `@testing-library/jest-dom` matchers: `toBeInTheDocument()`, `toHaveTextContent()`, `toBeVisible()`
- **Testing philosophy:** Test user behavior, not implementation details
- **Query priority:** Prefer `getByRole()` > `getByLabelText()` > `getByText()` > `getByTestId()`

### Mocking

**Framework:** Vitest built-in + `vi` module

**Recommended Patterns:**

Mock API calls:

```javascript
import { vi } from 'vitest'
import api from '../services/api'

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

it('loads publications on mount', async () => {
  api.get.mockResolvedValue({ data: [{ id: 1, marca: 'Toyota' }] })
  render(<HomePage />)
  expect(await screen.findByText('Toyota')).toBeInTheDocument()
})
```

Mock Context:

```javascript
import { AuthProvider } from '../context/AuthContext'

const mockAuthValue = {
  usuario: { nombre: 'Test', email: 'test@example.com', rol: 'COMPRADOR' },
  esAdmin: false,
  login: vi.fn(),
  logout: vi.fn(),
}

// Wrap component with mock provider or use custom render function
```

**What to Mock:**
- API calls (axios interceptors)
- Context providers (especially AuthContext)
- External libraries (lucide-react icons can be mocked if needed)
- Router hooks (useNavigate, useParams) using `vi.mock('react-router-dom')`

**What NOT to Mock:**
- Utility functions like `formatoNumero()`
- Custom hooks logic (test behavior, not mocks)
- Component rendering logic

### Fixtures and Factories

**Test Data (To Be Implemented):**

Create mock data factory:

```javascript
// src/__tests__/fixtures/mockData.js
export const mockPublicacion = {
  id: 1,
  marca: 'Toyota',
  modelo: 'Corolla',
  anio: 2020,
  precio: 15000,
  moneda: 'ARS',
  kilometraje: 100000,
  estado: 'DISPONIBLE',
  fotos: [{ id: 1, url: 'https://example.com/car.jpg', orden: 0 }],
  agenciaNombre: 'Test Agencia',
  agenciaSlug: 'test-agencia',
  // ...other fields
}

export const mockUsuario = {
  nombre: 'Test User',
  email: 'test@example.com',
  rol: 'COMPRADOR',
}

export const mockAgencia = {
  id: 1,
  nombre: 'Test Agencia',
  slug: 'test-agencia',
  // ...other fields
}
```

**Location:** `src/__tests__/fixtures/mockData.js`

### Coverage

**Requirements:** Not enforced (no coverage tool configured)

**Recommended Target:** 70%+ for components, 80%+ for utilities and context

**To Enable Coverage (Vitest):**

In `vitest.config.js`:

```javascript
coverage: {
  provider: 'v8',
  reporter: ['text', 'json', 'html'],
  exclude: [
    'node_modules/',
    'src/__tests__/',
  ]
}
```

**View Coverage:**

```bash
npm run test -- --coverage

# Report generated at coverage/index.html

```

### Test Types

**Unit Tests:**
- Scope: Individual component rendering and user interactions
- Approach: Render component, simulate user events, assert output
- Example: Test `PublicacionCard` displays price and title correctly
- File location: `src/__tests__/components/PublicacionCard.test.jsx`

**Integration Tests:**
- Scope: Multiple components interacting (e.g., form submission through context)
- Approach: Render full pages or feature flows
- Example: Test login flow: user enters credentials → calls AuthContext → navigates home
- File location: `src/__tests__/integration/LoginFlow.test.jsx`

**Hook Tests:**
- Scope: Custom hook behavior (useAuth)
- Approach: Use `renderHook()` from testing library
- Example: Test `useAuth()` returns user from localStorage
- File location: `src/__tests__/hooks/useAuth.test.jsx`

### Common Patterns

**Async Testing:**

```javascript
it('loads publications asynchronously', async () => {
  api.get.mockResolvedValue({ data: [{ id: 1, marca: 'Toyota' }] })
  render(<HomePage />)
  
  const elemento = await screen.findByText('Toyota')
  expect(elemento).toBeInTheDocument()
})
```

**Error Testing:**

```javascript
it('displays error message on API failure', async () => {
  api.post.mockRejectedValue(new Error('Network error'))
  
  const user = userEvent.setup()
  render(<LoginPage />)
  
  await user.type(screen.getByLabelText('Email'), 'test@example.com')
  await user.type(screen.getByLabelText('Contraseña'), 'wrong')
  await user.click(screen.getByRole('button', { name: 'Ingresar' }))
  
  expect(await screen.findByText(/Email o contraseña/)).toBeInTheDocument()
})
```

**User Interaction Testing:**

```javascript
it('opens suggestions on input focus', async () => {
  const user = userEvent.setup()
  render(<HomePage />)
  
  const input = screen.getByPlaceholderText('Marca o modelo')
  await user.click(input)
  
  expect(screen.getByRole('listbox')).toBeVisible()
})
```

---

## Current Testing Status Summary

| Aspect | Backend | Frontend |
|--------|---------|----------|
| Dependencies Installed | ✓ (spring-boot-starter-test, spring-security-test) | ✗ (Not installed) |
| Configuration | Partial (deps only, no config) | ✗ (Not configured) |
| Test Files | ✗ (0 files) | ✗ (0 files) |
| Coverage Tools | ✗ (No JaCoCo) | ✗ (No coverage tool) |
| Priority | Create service + controller tests | Create component + integration tests |

## Recommended Next Steps

1. **Backend:**
   - Create `src/test/java/com/danteautomotores/` directory structure
   - Implement `AuthServiceTest` and `AuthControllerTest` as foundation
   - Add `TestFixtures` factory for mock objects
   - Aim for 70%+ coverage of services and controllers

2. **Frontend:**
   - Install Vitest + Testing Library: `npm install --save-dev vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom`
   - Create `vitest.config.js`
   - Create `src/__tests__/` directory structure
   - Start with `HomePage.test.jsx` and `LoginPage.test.jsx`
   - Add fixtures for mock data in `src/__tests__/fixtures/`

---

*Testing analysis: 2026-10-02*
