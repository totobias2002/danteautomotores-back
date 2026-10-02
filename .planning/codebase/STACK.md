---
last_mapped_commit: dedb4c0b09d0e3c7ee9c69ffe7438f1d6d56bd0a
last_mapped_at: 2026-10-02
---
# Technology Stack

**Analysis Date:** 2026-10-02

## Languages

**Backend:**
- Java 21 - Spring Boot REST API, JWT authentication, database ORM (JPA/Hibernate)

**Frontend:**
- JavaScript (ES6+) - React components, axios API client, React Router navigation
- CSS - Tailwind CSS v4 for styling

## Runtime

**Backend Environment:**
- Java 21 (eclipse-temurin for Docker)
- Spring Boot 3.3.5

**Frontend Environment:**
- Node.js (package-lock.json indicates npm version 10+)
- Vite 5.4.10 as dev server and bundler

**Package Managers:**
- Backend: Maven 3.9 - `pom.xml` contains dependency declarations, lockfile in `target/classes/`
- Frontend: npm - `package-lock.json` present (91 dependencies tracked)

## Frameworks

**Core:**
- Backend: Spring Boot 3.3.5 - RESTful API framework with dependency injection
- Frontend: React 18.3.1 - Component-based UI library

**Web/HTTP:**
- Backend: Spring Web Starter - HTTP request handling, embedded Tomcat server (port 8080 default)
- Frontend: Axios 1.7.7 - HTTP client for API calls with interceptor support for JWT tokens

**Routing:**
- Frontend: React Router v6.27.0 - Client-side navigation, protected routes for admin panel

**Data Access:**
- Backend: Spring Data JPA - ORM abstraction over Hibernate for PostgreSQL queries

**Security & Authentication:**
- Backend: Spring Security 3.3.5 - Authorization, role-based access control (ADMIN/COMPRADOR)
- Backend: JJWT (Java JWT) 0.12.6 - JWT token generation and validation for stateless auth

**Testing:**
- Backend: Spring Boot Test Starter, Spring Security Test - JUnit 5 framework (in pom.xml, scope:test)
- Frontend: No visible test framework configured (ESLint for linting only)

**Styling:**
- Frontend: Tailwind CSS v4.0.0 - Utility-first CSS framework
- Frontend: Lucide React 1.47.0 - Icon library for UI components

**Build/Dev:**
- Backend: Maven - Multi-stage Docker build via `Dockerfile`, `spring-boot-maven-plugin` for JAR creation
- Frontend: Vite 5.4.10 - Lightning-fast build tool with React plugin, dev server on port 5173
- Frontend: @vitejs/plugin-react 4.3.3 - JSX/Fast Refresh support
- Frontend: @tailwindcss/vite 4.0.0 - Tailwind CSS integration with Vite

## Key Dependencies

**Critical - Backend:**
- `postgresql` - JDBC driver for PostgreSQL database connectivity
- `jjwt-*` (api, impl, jackson) - JWT token handling for stateless authentication
- `cloudinary-http44` v1.39.0 - Image upload and hosting service SDK
- `spring-security` - Authentication/authorization enforcement
- `spring-data-jpa` - Database persistence layer

**Critical - Frontend:**
- `axios` - API communication with backend, token injection via interceptor
- `react-router-dom` - Navigation, route guards for admin pages
- `react` - Core UI framework

**Recommended Utilities:**
- `lucide-react` - SVG icon components for UI
- `tailwindcss` - Styling engine

## Configuration

**Backend Environment:**
- Primary config: `src/main/resources/application.yml` - Spring Boot YAML configuration
- Reads from environment variables (externalized for production deployment):
  - `PORT` - HTTP port (Railway/Render inject automatically)
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` - PostgreSQL credentials
  - `APP_JWT_SECRET` - JWT signing key (must be ≥32 characters in production)
  - `APP_JWT_EXPIRATION_MS` - Token TTL (defaults to 86400000ms = 24 hours)
  - `APP_CORS_ALLOWED_ORIGINS` - CORS whitelist (comma-separated origins)
  - `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` - Cloudinary credentials

**Frontend Environment:**
- Primary config: `vite.config.js` - Vite build configuration
- Environment variable: `VITE_API_URL` - Backend API base URL (defaults to `http://localhost:8080/api`)
- Config file: `.env.example` → `.env` in development

**Build:**
- Backend: `pom.xml` - Maven POM with spring-boot-maven-plugin for JAR packaging
- Frontend: `vite.config.js` - Vite config with React plugin and Tailwind CSS plugin
- Backend Dockerfile: Multi-stage build (Maven + JDK 21 for build → JRE 21-alpine for runtime)

**Development:**
- Backend: `docker-compose.yml` - PostgreSQL 16 on port 5433 (to avoid conflicts)
  - Database name: `danteautomotores`
  - Credentials: `dante` / `dante_dev_password`
  - Volume: `danteautomotores_pgdata` (data persistence)
- Frontend: Dev server on localhost:5173 with hot module replacement (HMR)

## Platform Requirements

**Development:**
- Java 21 JDK (e.g., Eclipse Temurin)
- Maven 3.9+
- Node.js (npm 10+)
- Docker + Docker Compose (for PostgreSQL development database)
- PostgreSQL client (optional, for manual DB queries)

**Production - Backend:**
- Container runtime (Docker, Kubernetes) OR Java 21 JRE runtime + Tomcat-compatible server
- PostgreSQL database (managed service or self-hosted)
- Cloudinary account with API credentials
- HTTP port binding capability (Railway/Render provide PORT env var)

**Production - Frontend:**
- Static hosting with SPA rewrite support (Vercel, Netlify, etc.)
- Vercel configuration present in `vercel.json` (rewrites all non-asset requests to `/index.html`)

## Deployment Targets

**Backend:**
- Railway (native Spring Boot support, environment variable injection)
- Render (Web Service with custom Dockerfile)
- Any container hosting with environment variable support

**Frontend:**
- Vercel (configured with `vercel.json` for SPA routing)
- Netlify
- Any static host with SPA rewrite capability

---

*Stack analysis: 2026-10-02*
