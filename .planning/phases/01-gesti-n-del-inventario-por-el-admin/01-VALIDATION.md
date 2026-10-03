---
phase: "1"
slug: "gesti-n-del-inventario-por-el-admin"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-10-02"
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + Spring MockMvc / spring-security-test (`spring-boot-starter-test`, Boot 3.3.5) |
| **Config file** | none — `src/test/` does not exist yet (Wave 0 creates it) |
| **Shell setup (Git Bash)** | `export JAVA_HOME="/c/Program Files/Java/jdk-17"; export PATH="/c/Users/toto/.maven/maven-3.9.16/bin:$PATH"` |
| **Quick run command** | `mvn -B -o -Djava.version=17 test -Dtest=<TestClass>` |
| **Full suite command** | `mvn -B -o -Djava.version=17 test` |
| **Front gate** | `npm --prefix C:/Users/toto/Desktop/work/danteautomotores-front run build` (no front test runner; `npm run lint` not usable) |
| **Estimated runtime** | ~30 seconds (backend, no DB) + ~15 seconds (front build) |

Note: machine has JDK 17 only; `-Djava.version=17` overrides the pom for local test runs. Do NOT change `java.version` in `pom.xml`. Use `clean` if a stale `class file version 65.0` error appears.

---

## Sampling Rate

- **After every task commit:** Run `mvn -B -o -Djava.version=17 test -Dtest=<task test class>` (+ front build if the task touches the front)
- **After every plan wave:** Run `mvn -B -o -Djava.version=17 test` and the front build
- **Before `/gsd-verify-work`:** Full suite green + front build + manual UAT (Docker/Postgres + Cloudinary)
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| ADM-01 | Seeder creates ADMIN only if none; never modifies existing; prod w/o vars fails; dev warns; seeds agency | unit (Mockito) | `mvn -B -o -Djava.version=17 test -Dtest=DataSeederTest` | ❌ W0 | ⬜ pending |
| ADM-01 | Registro with `"rol":"ADMIN"` still yields COMPRADOR | unit | `mvn -B -o -Djava.version=17 test -Dtest=AuthServiceTest` | ❌ W0 | ⬜ pending |
| ADM-02 | crear/actualizar assign single agency; eliminar removes favoritos/consultas first | unit | `mvn -B -o -Djava.version=17 test -Dtest=PublicacionServiceTest` | ❌ W0 | ⬜ pending |
| ADM-02 / D-07 (rev.) | Agency CRUD ADMIN-only; DELETE with autos → 400 | slice + unit | `mvn -B -o -Djava.version=17 test -Dtest=AgenciaControllerTest,AgenciaServiceTest` | ❌ W0 | ⬜ pending |
| ADM-03 | Image validator: real JPEG/PNG/WebP ok; spoofed content-type, >10MB, empty rejected | unit | `mvn -B -o -Djava.version=17 test -Dtest=ImagenValidatorTest` | ❌ W0 | ⬜ pending |
| ADM-03 | 10-photo cap, reorder validation, resequence on delete, public_id destroy best-effort | unit | `mvn -B -o -Djava.version=17 test -Dtest=PublicacionServiceTest` | ❌ W0 | ⬜ pending |
| ADM-04 | Admin listing returns all states, ADMIN-only | slice | `mvn -B -o -Djava.version=17 test -Dtest=AdminPublicacionControllerTest` | ❌ W0 | ⬜ pending |
| ADM-05 | PATCH destacado persists; missing body field → 400 | unit + slice | `mvn -B -o -Djava.version=17 test -Dtest=PublicacionServiceTest,PublicacionControllerTest` | ❌ W0 | ⬜ pending |
| PROD-01 | Bad/expired/empty token → 401 `{"error"}` on protected, 200 on public; COMPRADOR → 403 | slice | `mvn -B -o -Djava.version=17 test -Dtest=SeguridadErroresTest` | ❌ W0 | ⬜ pending |
| PROD-01 | Uniform format: validation, 404, malformed JSON, 405, 413, generic 500 w/o stacktrace | slice | `mvn -B -o -Djava.version=17 test -Dtest=GlobalExceptionHandlerTest` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/danteautomotores/config/DataSeederTest.java` — ADM-01
- [ ] `src/test/java/com/danteautomotores/service/AuthServiceTest.java` — ADM-01
- [ ] `src/test/java/com/danteautomotores/service/ImagenValidatorTest.java` — ADM-03
- [ ] `src/test/java/com/danteautomotores/service/PublicacionServiceTest.java` — ADM-02/03/04/05
- [ ] `src/test/java/com/danteautomotores/security/SeguridadErroresTest.java` — PROD-01
- [ ] `src/test/java/com/danteautomotores/exception/GlobalExceptionHandlerTest.java` — PROD-01
- [ ] `src/test/java/com/danteautomotores/controller/{Agencia,AdminPublicacion,Publicacion}ControllerTest.java` — D-07, ADM-04/05
- [ ] Framework install: none (all in pom)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Panel CRUD with in-UI confirm, state select, destacado toggle, photo reorder arrows, backend error messages, multi-agency CRUD with inline delete confirm and agency selector | ADM-02/03/04/05 | No front test framework (QA-V2-01 deferred) | Run back + front, exercise each action, reload to confirm persistence |
| Expired token → `/login` with "sesión vencida" notice, returns to origin page; bad login does NOT redirect | PROD-01 | No front test framework | Set `localStorage.token` to garbage or `APP_JWT_EXPIRATION_MS=60000` |
| 12–15 MB upload shows clear message (no connection reset) | ADM-03 | MockMvc doesn't apply Tomcat limits | Upload large file against running app |
| App boots with ADMIN_* vars and admin can log in; `prod` profile without vars fails | ADM-01 | Needs real Postgres | `mvn spring-boot:run` + `curl -X POST localhost:8080/api/auth/login` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
