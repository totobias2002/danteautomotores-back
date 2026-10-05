---
phase: 03-cuentas-verificadas
plan: 09
subsystem: auth
tags: [registro, validacion, dni, telefono, bcrypt, confirmacion-email, unique]

requires:
  - phase: 03-cuentas-verificadas
    provides: columnas apellido/dni y UNIQUE de V5, existsByEmailIgnoreCase/existsByDni (03-01); NormalizadorDeContacto (03-02); iniciarSesion y login con mail normalizado (03-03); TokenCuentaService y NotificacionesService (03-06)
provides:
  - RegistroRequest con apellido, teléfono y DNI obligatorios, contraseña de 8 a 72 caracteres y sin datos sensibles en el toString
  - AuthService.registrar completo: datos normalizados, DNI único, tope de 72 bytes, token CONFIRMAR_EMAIL y mail encolado sin que su falla rompa el alta, y sesión con faltantes
affects: [03-10, 03-12, 03-14, fase-04]

plan_head_before: 7049c5c7d05377ce6f9ac888c46c701bd47c53c6
plan_head_after: bcb12b47786e3f538a74d44e304c2dd1541cb70f

actuals:
  tokens: 6300
  tasks: 2
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Chequeos previos (existsBy...) más UNIQUE de la base como garantía real: DataIntegrityViolationException se traduce por nombre de restricción (ConstraintViolationException.getConstraintName) y cualquier otra se relanza"
    - "Efectos secundarios no críticos (token y mail) dentro de un try que atrapa RuntimeException y loguea solo la clase de la excepción"

key-files:
  created:
    - src/test/java/com/danteautomotores/dto/RegistroRequestValidationTest.java
  modified:
    - src/main/java/com/danteautomotores/dto/auth/RegistroRequest.java
    - src/main/java/com/danteautomotores/service/AuthService.java
    - src/test/java/com/danteautomotores/service/AuthServiceTest.java

key-decisions:
  - "El UNIQUE original del mail (ukkfsp0s1tflm1cwlj8idhqsad0, de V1) se traduce igual que el índice uk_usuarios_email_lower de V5: con el mismo mail exacto Postgres puede informar cualquiera de los dos"
  - "El mensaje de DNI repetido se reutiliza de UsuarioService.MENSAJE_DNI_DUPLICADO para que registro y perfil digan siempre lo mismo (D-04)"
  - "El mail también queda fuera del toString de RegistroRequest, además de contraseña, teléfono y DNI"

patterns-established:
  - "AuthService no es @Transactional: emitir tiene su propia transacción y confirma antes de que el mail asíncrono salga, así el link nunca llega antes que el token"

requirements-completed: [AUTH-01]

coverage:
  - id: D1
    description: "Sin apellido, teléfono o DNI (o con espacios) el pedido de registro es inválido en ese campo; mail sin formato o de más de 254 caracteres, contraseña fuera de 8 a 72 y los topes de largo también"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/dto/RegistroRequestValidationTest.java"
        status: pass
    human_judgment: false
  - id: D2
    description: "El toString del pedido no contiene contraseña, teléfono ni DNI, y el DTO no declara un campo de rol ni de privacidad"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/dto/RegistroRequestValidationTest.java#elToStringNoImprimeLaContrasenaElTelefonoNiElDni"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/dto/RegistroRequestValidationTest.java#noDeclaraUnCampoDeRolNiDePrivacidad"
        status: pass
    human_judgment: false
  - id: D3
    description: "La cuenta se guarda con mail recortado en minúsculas, celular +549..., DNI sin puntos, contraseña hasheada, emailConfirmado false y rol COMPRADOR aunque el body diga ADMIN"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#registroValidoGuardaLosDatosNormalizadosYSinConfirmar"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#registroConRolAdminEnElBody_siempreCreaComprador"
        status: pass
    human_judgment: false
  - id: D4
    description: "Mail repetido (sin distinguir mayúsculas) y DNI repetido se rechazan con sus mensajes fijos sin guardar; si dos registros simultáneos chocan, el UNIQUE de la base (DNI, índice lower(email) o UNIQUE original del mail) se traduce al mismo mensaje y cualquier otra violación se relanza"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#registroConEmailExistente_lanzaReglaDeNegocio"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#registroConDniExistenteNoGuardaYNoDiceDeQuienEs"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#siSaveAndFlushChocaConElUniqueDelDniSeTraduceAlMensajeDelDni"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#otraViolacionDeIntegridadSeRelanza"
        status: pass
    human_judgment: false
  - id: D5
    description: "Se emite el token CONFIRMAR_EMAIL de la cuenta guardada y se encola el mail con ese token (en ese orden); si emitir o encolar lanza, el registro igual devuelve la sesión y la advertencia no lleva mail, DNI ni teléfono"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#registroValidoEmiteElTokenDeConfirmacionYEncolaElMailConEseToken"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#siFallaEmitirElTokenElRegistroIgualDevuelveLaSesion"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#laAdvertenciaPorFallaDelMailNoLlevaMailDniNiTelefono"
        status: pass
    human_judgment: false
  - id: D6
    description: "Teléfono o DNI inválidos lanzan el mensaje del normalizador, y una contraseña de más de 72 bytes (40 caracteres con acento = 80 bytes) se rechaza sin llegar al encoder; exactamente 72 bytes se acepta"
    requirement: "AUTH-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#unaContrasenaDeMasDe72BytesSeRechazaAunqueTengaMenosDe72Caracteres"
        status: pass
      - kind: unit
        ref: "src/test/java/com/danteautomotores/service/AuthServiceTest.java#unaContrasenaDeExactamente72BytesSeAcepta"
        status: pass
    human_judgment: false

duration: 16 min
completed: 2026-10-05
status: complete
---

# Phase 3 Plan 09: Registro con identidad completa Summary

**El registro exige nombre, apellido, mail, contraseña (8 a 72 caracteres y 72 bytes), teléfono y DNI; guarda todo normalizado con DNI único (el UNIQUE de la base resuelve las carreras), emite el token de confirmación de 24 horas, encola el mail sin que su falla rompa el alta y devuelve la sesión con los datos faltantes.**

## Performance

- **Duration:** 16 min
- **Tasks:** 2 (cada una con ciclo RED y GREEN: 4 commits)
- **Files modified:** 4 (1 creado, 3 modificados), todos en el back

## Accomplishments

- `RegistroRequest` (sigue siendo `@Data`): apellido, teléfono y DNI son `@NotBlank` con topes de largo (100, 30 y 20), el mail tiene `@Size(max = 254)` y la contraseña acepta de 8 a 72 caracteres con el mensaje "La contraseña debe tener entre 8 y 72 caracteres". `password`, `telefono`, `dni` y `email` llevan `@ToString.Exclude`. No hay campo de rol ni de aceptación de privacidad.
- `AuthService.registrar` sigue el orden pedido: mail normalizado, mail repetido (mensaje de D-17 con el compromiso de UX y la deuda de seguridad anotados en un comentario), tope de 72 bytes en UTF-8, normalización de teléfono y DNI, DNI repetido (D-04), alta con `saveAndFlush` traduciendo `DataIntegrityViolationException` por nombre de restricción, token y mail en un `try` que atrapa `RuntimeException`, y `iniciarSesion`.
- La advertencia por falla de mail registra solo la clase de la excepción (nunca su mensaje, que podría traer mail, DNI o teléfono); un test captura el log y comprueba que no tiene datos personales ni `throwableProxy`.
- `AuthService` sigue sin `@Transactional`: `TokenCuentaService.emitir` confirma en su propia transacción antes de llamar a `NotificacionesService`, que es lo que pidió el aviso de 03-06 (el link nunca llega antes que el token).

## Task Commits

1. **Tarea 1: contrato del registro (TDD)**
   - RED: `7d5c353` (test con `RegistroRequest` apenas con los campos nuevos y sin restricciones; 5 de 8 fallan por aserción)
   - GREEN: `9609ab1`
2. **Tarea 2: registrar completo (TDD)**
   - RED: `ede3a13` (17 de 24 tests fallan contra el `registrar` anterior)
   - GREEN: `bcb12b4`

**Plan metadata:** commit de docs con este SUMMARY (siguiente commit).

## Decisions Made

- Se traduce también el UNIQUE original del mail de V1 (`ukkfsp0s1tflm1cwlj8idhqsad0`), no solo `uk_usuarios_email_lower`: la tabla tiene los dos y, ante dos registros con el mismo mail exacto, Postgres informa el que evalúe primero.
- El mensaje de DNI repetido sale de `UsuarioService.MENSAJE_DNI_DUPLICADO`, la misma constante del perfil (03-05), para que no diverjan.
- El mail también queda fuera del `toString` (el plan pedía contraseña, teléfono y DNI): la Ley 25.326 lo incluye y no cuesta nada.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Falta traducir el UNIQUE original del mail**
- **Found during:** Tarea 2 (al leer V1 y V5 para el nombre de la restricción)
- **Issue:** El plan traducía solo `uk_usuarios_email_lower`. La tabla `usuarios` conserva además `UNIQUE (email)` de V1 (`ukkfsp0s1tflm1cwlj8idhqsad0`). Con dos registros simultáneos con el mismo mail exacto, Postgres puede reportar esa restricción y la violación se habría relanzado como 409 genérico en vez del mensaje del mail.
- **Fix:** `AuthService` reconoce las dos restricciones del mail; un test cubre la restricción original.
- **Files modified:** `src/main/java/com/danteautomotores/service/AuthService.java`, `src/test/java/com/danteautomotores/service/AuthServiceTest.java`
- **Commit:** `bcb12b4`

### Notas de ejecución (no son desvíos de código)

- **Tests con `RegistroRequest` en el RED de la Tarea 1:** se commiteó el RED con los campos nuevos agregados sin restricciones para que compile y falle por aserción (el mismo criterio que 03-03). No se corrió `gsd_run check tdd-red-evidence`; el fallo se observó en la salida de Maven.
- **Mensaje de los 72 bytes:** el plan pide "un mensaje claro"; es "La contraseña es demasiado larga: no puede superar los 72 bytes (los acentos y símbolos ocupan más de uno)." El front (03-12) lo muestra tal cual vía `mensajeDeError`.
- **Archivos tocados:** coinciden con `files_modified` del plan; no hubo otros.
- **AUTH-01** ya figuraba como "Complete" en la tabla de trazabilidad de REQUIREMENTS.md; se vuelve a pasar por `requirements.mark-complete` y queda igual.

**Total deviations:** 1 auto-fixed (Rule 1). **Impact:** el mensaje de mail repetido vale también en la carrera con el UNIQUE original.

## Issues Encountered

- Mockito estricto marcó `UnnecessaryStubbing` en los cuatro tests de violación de integridad, porque el helper del camino feliz ya stubbeaba `saveAndFlush`. Se separó el helper en `prepararChequeosPrevios` y `prepararRegistroValido`. Fue un error del test, no del código.

## Verificación

- `mvn -B -o -Djava.version=17 test -Dtest=AuthServiceTest,RegistroRequestValidationTest,NormalizadorDeContactoTest`: 68 tests (25 + 8 + 35), 0 fallas.
- Suite completa del back (`-o -Djava.version=17 -Ddante.pg.required=true`): 573 tests, 0 fallas, 0 errores, 0 omitidos, BUILD SUCCESS (antes 550 sin Postgres obligatorio según 03-08; este plan suma 23: 8 `RegistroRequestValidationTest` y 15 más en `AuthServiceTest`, que pasó de 10 a 25). `GlobalExceptionHandlerTest` y el resto de los tests de controlador corrieron dentro de la suite completa y pasaron.
- No se tocó producción ni el `.env` del front; no se escribió ninguna credencial; los datos de los tests son ficticios.

## Pendiente de UAT manual

None - este plan no tiene verificaciones de navegador. El formulario de registro con los campos nuevos y la leyenda de privacidad es 03-12 (front); el envío real del mail de confirmación es el humo de 03-14.

## Known Stubs

None.

## Threat Flags

None. No hay endpoints ni rutas nuevas: `/api/auth/registro` conserva su forma. T-03-47 (rol fijo en COMPRADOR), T-03-49 (sin datos sensibles en `toString` ni en logs), T-03-50 (UNIQUE traducido), T-03-51 (72 caracteres en el DTO y 72 bytes en el service) y T-03-52 (token y mail fuera del camino crítico) quedaron mitigadas y probadas. T-03-48 se acepta según D-17: el límite por IP del registro lo pone `AuthController` en 03-10 y la deuda va al README en 03-14.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 03-10 debe inyectar el `LimitadorDeIntentos` en `AuthController` para el registro (D-17), agregar el reenvío del mail de confirmación y actualizar `GlobalExceptionHandlerTest` si el controlador gana dependencias.
- 03-12 (front) manda apellido, teléfono y DNI en el registro; el 400 por campos faltantes ya llega en `campos` y las `ReglaDeNegocioException` en `error`.
- El registro no confirma el mail: la sesión sale con `EMAIL_SIN_CONFIRMAR` en `faltantes` hasta que se use el link del mail (endpoint de confirmación en 03-10).
- El mail se guarda en minúsculas solo en cuentas nuevas; las cuentas anteriores conservan su mayúscula original y se encuentran igual por `findByEmailIgnoreCase`.

## Self-Check: PASSED

- Archivos presentes en disco: `RegistroRequestValidationTest.java`, `RegistroRequest.java`, `AuthService.java` y `AuthServiceTest.java`.
- Commits presentes: `7d5c353`, `9609ab1`, `ede3a13` y `bcb12b4`.
