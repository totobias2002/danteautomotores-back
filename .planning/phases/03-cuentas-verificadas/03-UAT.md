---
status: complete
phase: 03-cuentas-verificadas
source: 03-01-SUMMARY.md .. 03-15-SUMMARY.md
started: 2026-10-06T20:50:19.269Z
updated: 2026-10-06T20:55:31.726Z
---

## Current Test

[testing complete]

## Tests

### 1. Cold Start Smoke Test
expected: Frená cualquier back local, arrancalo de cero contra una base limpia (con-back-local.sh --vacia). Flyway aplica V1 a V5 sin errores, el back levanta y GET /api/publicaciones devuelve datos.
result: pass
note: verificado por Claude contra base vacia dante_uat_frio - Flyway V1 a V5 sin errores, GET /api/publicaciones 200 (lista vacia por ser base vacia)

### 2. Registro con identidad completa
expected: En /registro se piden nombre, apellido, mail, contraseña, teléfono y DNI obligatorios. Los errores del back se muestran como mensaje. Al registrarte vas a Completa tus datos solo si faltan datos; si no, volvés al origen.
result: pass

### 3. Banner de cuenta incompleta
expected: Con un comprador de cuenta incompleta, el banner aparece arriba, sigue ahí al recargar, desaparece al cerrar sesión y no se ve con el admin.
result: pass
note: usuario confirmo que el aviso de verificar el mail aparece; recarga, cierre de sesion y admin no se confirmaron uno por uno

### 4. Pantalla Completa tus datos
expected: /completar-datos precarga lo que hay, pide teléfono y DNI, deja el DNI de solo lectura si ya está cargado, muestra el estado del mail con reenvío de confirmación y se puede saltear con Más tarde.
result: pass

### 5. Login, errores y límites
expected: Credenciales malas dan el mensaje genérico 'Email o contraseña incorrectos'; tras muchos intentos aparece el mensaje de límite; hay link a Olvidé mi contraseña; 'Creá una gratis' e 'Ingresá' conservan el origen.
result: pass

### 6. Botón de Google
expected: En login y registro aparece el botón oficial de Google; entrar con una cuenta Google crea/entra a la sesión. Sin Client ID el botón y el separador no se ven.
result: pass

### 7. Recuperar contraseña y confirmar mail
expected: Olvidé mi contraseña muestra siempre el mismo mensaje. El link del mail lleva a Restablecer, saca el token de la URL y manda al login con aviso. El link de Confirmar mail funciona una sola vez (recargar no lo reusa).
result: pass

### 8. Perfil y cambio de contraseña
expected: /perfil muestra y edita nombre, apellido y teléfono; DNI y mail son de solo lectura; se ve si la cuenta está verificada o qué falta. Cambiar la contraseña (pidiendo la actual) deja sesión nueva y otra pestaña abierta pide ingresar de nuevo.
result: pass
note: usuario respondio pass todo, tests 8 a 12 en bloque

### 9. Gate: sin sesión o cuenta incompleta
expected: Sin sesión, Lo quiero, Cotizar, Simula tu financiamiento, Cotiza tu usado, Vender tu auto y Mis mensajes llevan al login y vuelven a donde estabas. Con cuenta incompleta llevan a Completa tus datos.
result: pass
note: usuario respondio pass todo, tests 8 a 12 en bloque

### 10. Consulta desde la ficha
expected: La consulta de la ficha solo pide el mensaje (hasta 2000 caracteres) y llega al panel del admin con nombre, mail y teléfono de la cuenta. Quien no puede consultar ve el acceso en vez del formulario.
result: pass
note: usuario respondio pass todo, tests 8 a 12 en bloque

### 11. Privacidad y headers
expected: /privacidad existe como borrador con la leyenda de la AAIP; el sitio sirve Referrer-Policy strict-origin-when-cross-origin.
result: pass
note: usuario respondio pass todo, tests 8 a 12 en bloque

### 12. Producción real y mails
expected: En producción (Railway/Vercel) registro, confirmación, Google y recuperación funcionan con casillas reales, y los mails llegan de Brevo (revisar spam). La API key de Brevo que se pegó en el chat ya fue rotada.
result: pass
note: usuario respondio pass todo, tests 8 a 12 en bloque

### 13. V5 se aplica sobre una producción simulada (V1 con datos, baseline y V2 a V5) sin perder filas, con UNIQUE de dni, CHECK de formato, índice único lower(email), password_hash nullable y tabla tokens_cuenta
expected: V5 se aplica sobre una producción simulada (V1 con datos, baseline y V2 a V5) sin perder filas, con UNIQUE de dni, CHECK de formato, índice único lower(email), password_hash nullable y tabla tokens_cuenta
result: pass
source: automated
coverage_id: 03-01/D1

### 14. Regla de cuenta verificada: apellido, teléfono, DNI y mail confirmado; un ADMIN nunca tiene faltantes
expected: Regla de cuenta verificada: apellido, teléfono, DNI y mail confirmado; un ADMIN nunca tiene faltantes
result: pass
source: automated
coverage_id: 03-01/D2

### 15. Registro y login devuelven apellido, emailConfirmado, cuentaVerificada y faltantes, y no devuelven dni ni telefono
expected: Registro y login devuelven apellido, emailConfirmado, cuentaVerificada y faltantes, y no devuelven dni ni telefono
result: pass
source: automated
coverage_id: 03-01/D3

### 16. El telefono se valida como celular argentino y se guarda como +549 mas 10 digitos; otro pais, sin codigo de area, incompleto o vacio se rechaza con mensaje claro
expected: El telefono se valida como celular argentino y se guarda como +549 mas 10 digitos; otro pais, sin codigo de area, incompleto o vacio se rechaza con mensaje claro
result: pass
source: automated
coverage_id: 03-02/D1

### 17. El DNI se acepta con puntos, espacios o guiones y se guarda sin ellos (7 u 8 digitos, sin cero inicial); todo lo demas se rechaza
expected: El DNI se acepta con puntos, espacios o guiones y se guarda sin ellos (7 u 8 digitos, sin cero inicial); todo lo demas se rechaza
result: pass
source: automated
coverage_id: 03-02/D2

### 18. Una cuenta con telefono viejo de texto libre invalido cuenta como faltante de TELEFONO; exigir lanza con los faltantes y no hace nada con una cuenta verificada
expected: Una cuenta con telefono viejo de texto libre invalido cuenta como faltante de TELEFONO; exigir lanza con los faltantes y no hace nada con una cuenta verificada
result: pass
source: automated
coverage_id: 03-02/D3

### 19. 403 CUENTA_NO_VERIFICADA con error, codigo y faltantes; 429 con Retry-After y mensaje en espanol, en el formato de error uniforme
expected: 403 CUENTA_NO_VERIFICADA con error, codigo y faltantes; 429 con Retry-After y mensaje en espanol, en el formato de error uniforme
result: pass
source: automated
coverage_id: 03-02/D4

### 20. El log de una violacion de integridad contiene el nombre de la restriccion y no el DNI ni la fila
expected: El log de una violacion de integridad contiene el nombre de la restriccion y no el DNI ni la fila
result: pass
source: automated
coverage_id: 03-02/D5

### 21. Una cuenta sin contraseña (solo Google) se autentica con su JWT en el filtro real, con su rol, y no queda sin autenticar en silencio
expected: Una cuenta sin contraseña (solo Google) se autentica con su JWT en el filtro real, con su rol, y no queda sin autenticar en silencio
result: pass
source: automated
coverage_id: 03-03/D1

### 22. Un token emitido antes de un cambio de contraseña deja de valer, uno posterior vale y los tokens viejos sin claim pca valen hasta el primer cambio
expected: Un token emitido antes de un cambio de contraseña deja de valer, uno posterior vale y los tokens viejos sin claim pca valen hasta el primer cambio
result: pass
source: automated
coverage_id: 03-03/D2

### 23. El login normaliza el mail, rechaza con el 401 genérico a una cuenta sin contraseña sin llamar al AuthenticationManager, y todas las sesiones salen de iniciarSesion con una CuentaUserDetails
expected: El login normaliza el mail, rechaza con el 401 genérico a una cuenta sin contraseña sin llamar al AuthenticationManager, y todas las sesiones salen de iniciarSesion con una CuentaUserDetails
result: pass
source: automated
coverage_id: 03-03/D3

### 24. El limitador cuenta por clave y ventana, vence solo con un reloj falso, no suma al consultar, tiene tope de 10.000 claves y no lanza excepciones
expected: El limitador cuenta por clave y ventana, vence solo con un reloj falso, no suma al consultar, tiene tope de 10.000 claves y no lanza excepciones
result: pass
source: automated
coverage_id: 03-03/D4

### 25. BrevoEmailSender manda api-key, JSON y el cuerpo del contrato (sender, to, subject, htmlContent, textContent, replyTo solo si esta configurado) con los acentos en UTF-8
expected: BrevoEmailSender manda api-key, JSON y el cuerpo del contrato (sender, to, subject, htmlContent, textContent, replyTo solo si esta configurado) con los acentos en UTF-8
result: pass
source: automated
coverage_id: 03-04/D1

### 26. Un 4xx, un 5xx, un timeout o una conexion rechazada lanzan ServicioExternoException con mensaje fijo; los logs y la excepcion no contienen el texto del mail, el destinatario ni la API key; un 401 se loguea aparte mencionando la API key y las IPs
expected: Un 4xx, un 5xx, un timeout o una conexion rechazada lanzan ServicioExternoException con mensaje fijo; los logs y la excepcion no contienen el texto del mail, el destinatario ni la API key; un 401 se loguea aparte mencionando la API key y las IPs
result: pass
source: automated
coverage_id: 03-04/D2

### 27. Sin BREVO_API_KEY el bean es LogEmailSender (una sola linea de log con destinatario, asunto y link) y con ella es BrevoEmailSender; la clave no se loguea; un envio @Async corre en un hilo mail-
expected: Sin BREVO_API_KEY el bean es LogEmailSender (una sola linea de log con destinatario, asunto y link) y con ella es BrevoEmailSender; la clave no se loguea; un envio @Async corre en un hilo mail-
result: pass
source: automated
coverage_id: 03-04/D3

### 28. Fuera del modo desarrollo el back no arranca sin BREVO_API_KEY, MAIL_REMITENTE_EMAIL, APP_FRONTEND_URL (https, no localhost ni 127.0.0.1) o GOOGLE_CLIENT_ID, y el mensaje nombra la variable exacta; en desarrollo solo avisa
expected: Fuera del modo desarrollo el back no arranca sin BREVO_API_KEY, MAIL_REMITENTE_EMAIL, APP_FRONTEND_URL (https, no localhost ni 127.0.0.1) o GOOGLE_CLIENT_ID, y el mensaje nombra la variable exacta; en desarrollo solo avisa
result: pass
source: automated
coverage_id: 03-04/D4

### 29. Las variables nuevas estan en application.yml y en el README sin ningun valor de credencial
expected: Las variables nuevas estan en application.yml y en el README sin ningun valor de credencial
result: pass
source: automated
coverage_id: 03-04/D5

### 30. GET /api/usuarios/me devuelve el perfil del propio usuario con faltantes, cuentaVerificada, tieneContrasena y tieneGoogle, sin recibir ningun id
expected: GET /api/usuarios/me devuelve el perfil del propio usuario con faltantes, cuentaVerificada, tieneContrasena y tieneGoogle, sin recibir ningun id
result: pass
source: automated
coverage_id: 03-05/D1

### 31. PUT /api/usuarios/me guarda nombre, apellido, telefono normalizado y DNI normalizado; la cuenta queda solo con EMAIL_SIN_CONFIRMAR
expected: PUT /api/usuarios/me guarda nombre, apellido, telefono normalizado y DNI normalizado; la cuenta queda solo con EMAIL_SIN_CONFIRMAR
result: pass
source: automated
coverage_id: 03-05/D2

### 32. El DNI es inmutable una vez cargado (el mismo con puntos se acepta, uno distinto da 400) y obligatorio para compradores pero no para el admin
expected: El DNI es inmutable una vez cargado (el mismo con puntos se acepta, uno distinto da 400) y obligatorio para compradores pero no para el admin
result: pass
source: automated
coverage_id: 03-05/D3

### 33. Un DNI de otra cuenta se rechaza con el mensaje de D-04 sin revelar el mail ni el DNI, tambien cuando lo decide el UNIQUE de la base
expected: Un DNI de otra cuenta se rechaza con el mensaje de D-04 sin revelar el mail ni el DNI, tambien cuando lo decide el UNIQUE de la base
result: pass
source: automated
coverage_id: 03-05/D4

### 34. Sin IDOR ni mass assignment: el mail sale del token, el request no declara mail ni rol y un body con campos de mas no rompe
expected: Sin IDOR ni mass assignment: el mail sale del token, el request no declara mail ni rol y un body con campos de mas no rompe
result: pass
source: automated
coverage_id: 03-05/D5

### 35. El telefono y el DNI no aparecen en el toString del request ni de la respuesta ni en el log del back durante el humo
expected: El telefono y el DNI no aparecen en el toString del request ni de la respuesta ni en el log del back durante el humo
result: pass
source: automated
coverage_id: 03-05/D6

### 36. El token son 256 bits aleatorios (43 caracteres) y en la base solo se guarda el SHA-256 hex; ningun valor de la fila es igual al token
expected: El token son 256 bits aleatorios (43 caracteres) y en la base solo se guarda el SHA-256 hex; ningun valor de la fila es igual al token
result: pass
source: automated
coverage_id: 03-06/D1

### 37. Un token se consume una sola vez aunque lleguen ocho pedidos simultaneos (exactamente un exito); vencido, usado, de otro tipo o inexistente dan el mismo vacio
expected: Un token se consume una sola vez aunque lleguen ocho pedidos simultaneos (exactamente un exito); vencido, usado, de otro tipo o inexistente dan el mismo vacio
result: pass
source: automated
coverage_id: 03-06/D2

### 38. La confirmacion de mail vence a las 24 horas (valida a las 23 h 59, invalida a las 24 h 1) y el restablecimiento de contraseña a la hora
expected: La confirmacion de mail vence a las 24 horas (valida a las 23 h 59, invalida a las 24 h 1) y el restablecimiento de contraseña a la hora
result: pass
source: automated
coverage_id: 03-06/D3

### 39. Emitir un token nuevo borra el anterior del mismo tipo sin tocar el del otro tipo; descartarPendientes borra los dos tipos de la cuenta y solo de esa cuenta
expected: Emitir un token nuevo borra el anterior del mismo tipo sin tocar el del otro tipo; descartarPendientes borra los dos tipos de la cuenta y solo de esa cuenta
result: pass
source: automated
coverage_id: 03-06/D4

### 40. Los links de los tres mails se arman solo con app.frontend-url (sin duplicar la barra final) y el aviso de contraseña cambiada no lleva ningun link; el HTML escapa el nombre
expected: Los links de los tres mails se arman solo con app.frontend-url (sin duplicar la barra final) y el aviso de contraseña cambiada no lleva ningun link; el HTML escapa el nombre
result: pass
source: automated
coverage_id: 03-06/D5

### 41. Superado el tope diario (por defecto 250) no se envia nada y se loguea un error sin datos personales; una falla de envio no hace lanzar al metodo y el log no contiene el token ni el mail del destinatario
expected: Superado el tope diario (por defecto 250) no se envia nada y se loguea un error sin datos personales; una falla de envio no hace lanzar al metodo y el log no contiene el token ni el mail del destinatario
result: pass
source: automated
coverage_id: 03-06/D6

### 42. Con la cola del ejecutor de mail llena el envio se descarta y se loguea, sin lanzar excepcion a quien lo pidio
expected: Con la cola del ejecutor de mail llena el envio se descarta y se loguea, sin lanzar excepcion a quien lo pidio
result: pass
source: automated
coverage_id: 03-06/D7

### 43. El verificador acepta solo un ID token de Google bien firmado, con el emisor correcto (con o sin https), la audiencia igual al Client ID y vigente (60 s de tolerancia); rechaza audiencia o emisor ajenos, vencidos hace más de 60 s, malformados, vacíos, nulos, firmados con otra clave RSA y cualquier token con el Client ID en blanco
expected: El verificador acepta solo un ID token de Google bien firmado, con el emisor correcto (con o sin https), la audiencia igual al Client ID y vigente (60 s de tolerancia); rechaza audiencia o emisor ajenos, vencidos hace más de 60 s, malformados, vacíos, nulos, firmados con otra clave RSA y cualquier token con el Client ID en blanco
result: pass
source: automated
coverage_id: 03-07/D1

### 44. email_verified se interpreta con tolerancia (booleano true o texto true), nombre y apellido salen de given_name y family_name con respaldo en name, y la excepción lleva un mensaje fijo sin el token
expected: email_verified se interpreta con tolerancia (booleano true o texto true), nombre y apellido salen de given_name y family_name con respaldo en name, y la excepción lleva un mensaje fijo sin el token
result: pass
source: automated
coverage_id: 03-07/D2

### 45. Un sub ya vinculado entra a su cuenta; un sub nuevo sin cuenta crea un COMPRADOR con mail en minúsculas confirmado, sin teléfono, DNI ni contraseña; un token inválido o con email_verified falso no toca la base
expected: Un sub ya vinculado entra a su cuenta; un sub nuevo sin cuenta crea un COMPRADOR con mail en minúsculas confirmado, sin teléfono, DNI ni contraseña; un token inválido o con email_verified falso no toca la base
result: pass
source: automated
coverage_id: 03-07/D3

### 46. La unión por mail conserva la contraseña de una cuenta con mail confirmado y descarta la contraseña (con passwordCambiadaEn igual al reloj) de una con mail sin confirmar (anti pre-hijacking, D-15), sin perder datos; completa el apellido solo si faltaba
expected: La unión por mail conserva la contraseña de una cuenta con mail confirmado y descarta la contraseña (con passwordCambiadaEn igual al reloj) de una con mail sin confirmar (anti pre-hijacking, D-15), sin perder datos; completa el apellido solo si faltaba
result: pass
source: automated
coverage_id: 03-07/D4

### 47. Una cuenta ADMIN, una cuenta ya unida a otro Google y un mail no confirmado se rechazan sin modificar ni guardar nada; la carrera de dos ingresos reintenta la búsqueda por sub; el servicio no es transaccional
expected: Una cuenta ADMIN, una cuenta ya unida a otro Google y un mail no confirmado se rechazan sin modificar ni guardar nada; la carrera de dos ingresos reintenta la búsqueda por sub; el servicio no es transaccional
result: pass
source: automated
coverage_id: 03-07/D5

### 48. Consultar desde la ficha exige comprador con sesion: sin token 401, admin 403, cuenta incompleta 403 CUENTA_NO_VERIFICADA con faltantes
expected: Consultar desde la ficha exige comprador con sesion: sin token 401, admin 403, cuenta incompleta 403 CUENTA_NO_VERIFICADA con faltantes
result: pass
source: automated
coverage_id: 03-08/D1

### 49. La consulta toma nombre, mail y telefono de la cuenta; un cuerpo viejo con esos campos no rompe y se ignoran
expected: La consulta toma nombre, mail y telefono de la cuenta; un cuerpo viejo con esos campos no rompe y se ignoran
result: pass
source: automated
coverage_id: 03-08/D2

### 50. Un auto vendido sigue rechazado con 'Este auto ya se vendio'; uno reservado o disponible se consulta con cuenta verificada
expected: Un auto vendido sigue rechazado con 'Este auto ya se vendio'; uno reservado o disponible se consulta con cuenta verificada
result: pass
source: automated
coverage_id: 03-08/D3

### 51. Crear una solicitud de Vender tu auto exige comprador verificado; el listado y el cambio de estado siguen siendo solo del admin
expected: Crear una solicitud de Vender tu auto exige comprador verificado; el listado y el cambio de estado siguen siendo solo del admin
result: pass
source: automated
coverage_id: 03-08/D4

### 52. El rechazo lo decide el back en cada accion con el estado actual de la base, no con el front ni un claim del token
expected: El rechazo lo decide el back en cada accion con el estado actual de la base, no con el front ni un claim del token
result: pass
source: automated
coverage_id: 03-08/D5

### 53. Sin apellido, teléfono o DNI (o con espacios) el pedido de registro es inválido en ese campo; mail sin formato o de más de 254 caracteres, contraseña fuera de 8 a 72 y los topes de largo también
expected: Sin apellido, teléfono o DNI (o con espacios) el pedido de registro es inválido en ese campo; mail sin formato o de más de 254 caracteres, contraseña fuera de 8 a 72 y los topes de largo también
result: pass
source: automated
coverage_id: 03-09/D1

### 54. El toString del pedido no contiene contraseña, teléfono ni DNI, y el DTO no declara un campo de rol ni de privacidad
expected: El toString del pedido no contiene contraseña, teléfono ni DNI, y el DTO no declara un campo de rol ni de privacidad
result: pass
source: automated
coverage_id: 03-09/D2

### 55. La cuenta se guarda con mail recortado en minúsculas, celular +549..., DNI sin puntos, contraseña hasheada, emailConfirmado false y rol COMPRADOR aunque el body diga ADMIN
expected: La cuenta se guarda con mail recortado en minúsculas, celular +549..., DNI sin puntos, contraseña hasheada, emailConfirmado false y rol COMPRADOR aunque el body diga ADMIN
result: pass
source: automated
coverage_id: 03-09/D3

### 56. Mail repetido (sin distinguir mayúsculas) y DNI repetido se rechazan con sus mensajes fijos sin guardar; si dos registros simultáneos chocan, el UNIQUE de la base (DNI, índice lower(email) o UNIQUE original del mail) se traduce al mismo mensaje y cualquier otra violación se relanza
expected: Mail repetido (sin distinguir mayúsculas) y DNI repetido se rechazan con sus mensajes fijos sin guardar; si dos registros simultáneos chocan, el UNIQUE de la base (DNI, índice lower(email) o UNIQUE original del mail) se traduce al mismo mensaje y cualquier otra violación se relanza
result: pass
source: automated
coverage_id: 03-09/D4

### 57. Se emite el token CONFIRMAR_EMAIL de la cuenta guardada y se encola el mail con ese token (en ese orden); si emitir o encolar lanza, el registro igual devuelve la sesión y la advertencia no lleva mail, DNI ni teléfono
expected: Se emite el token CONFIRMAR_EMAIL de la cuenta guardada y se encola el mail con ese token (en ese orden); si emitir o encolar lanza, el registro igual devuelve la sesión y la advertencia no lleva mail, DNI ni teléfono
result: pass
source: automated
coverage_id: 03-09/D5

### 58. Teléfono o DNI inválidos lanzan el mensaje del normalizador, y una contraseña de más de 72 bytes (40 caracteres con acento = 80 bytes) se rechaza sin llegar al encoder; exactamente 72 bytes se acepta
expected: Teléfono o DNI inválidos lanzan el mensaje del normalizador, y una contraseña de más de 72 bytes (40 caracteres con acento = 80 bytes) se rechaza sin llegar al encoder; exactamente 72 bytes se acepta
result: pass
source: automated
coverage_id: 03-09/D6

### 59. Pedir el cambio de contraseña responde 200 con el mismo texto para dos mails distintos y aun después de diez logins fallidos del mismo mail; para una cuenta inexistente o un límite excedido el service no emite ni manda ni lanza
expected: Pedir el cambio de contraseña responde 200 con el mismo texto para dos mails distintos y aun después de diez logins fallidos del mismo mail; para una cuenta inexistente o un límite excedido el service no emite ni manda ni lanza
result: pass
source: automated
coverage_id: 03-10/D1

### 60. Restablecer con token válido cambia el hash, fija passwordCambiadaEn, confirma el mail, borra los tokens pendientes, avisa y no devuelve sesión; con token inválido da 400 con el mismo mensaje y no cambia nada; una contraseña de más de 72 bytes se rechaza sin gastar el token
expected: Restablecer con token válido cambia el hash, fija passwordCambiadaEn, confirma el mail, borra los tokens pendientes, avisa y no devuelve sesión; con token inválido da 400 con el mismo mensaje y no cambia nada; una contraseña de más de 72 bytes se rechaza sin gastar el token
result: pass
source: automated
coverage_id: 03-10/D2

### 61. Cambiar la contraseña desde el perfil exige la actual, devuelve la sesión de iniciarSesion con la cuenta ya actualizada (las demás caen por pca), limita 5 fallos por cuenta y manda a una cuenta solo-Google a Olvidé mi contraseña
expected: Cambiar la contraseña desde el perfil exige la actual, devuelve la sesión de iniciarSesion con la cuenta ya actualizada (las demás caen por pca), limita 5 fallos por cuenta y manda a una cuenta solo-Google a Olvidé mi contraseña
result: pass
source: automated
coverage_id: 03-10/D3

### 62. El undécimo login fallido del mismo mail, el trigésimo primero de la misma IP, el registro número 11 y el intento 31 de Google o de consumo de tokens dan 429 con Retry-After; un login correcto reinicia el contador y otro mail no está bloqueado
expected: El undécimo login fallido del mismo mail, el trigésimo primero de la misma IP, el registro número 11 y el intento 31 de Google o de consumo de tokens dan 429 con Retry-After; un login correcto reinicia el contador y otro mail no está bloqueado
result: pass
source: automated
coverage_id: 03-10/D4

### 63. POST /api/auth/google devuelve la misma sesión que el login; sin credential da 400 con campos y con BadCredentialsException da 401; confirmar-email y restablecer-contrasena son POST (el GET da 405)
expected: POST /api/auth/google devuelve la misma sesión que el login; sin credential da 400 con campos y con BadCredentialsException da 401; confirmar-email y restablecer-contrasena son POST (el GET da 405)
result: pass
source: automated
coverage_id: 03-10/D5

### 64. El reenvío de confirmación manda el mail con la cuenta sin confirmar, no hace nada ni gasta cupo con la cuenta confirmada y el cuarto reenvío en la hora lanza LimiteDeIntentosException (429)
expected: El reenvío de confirmación manda el mail con la cuenta sin confirmar, no hace nada ni gasta cupo con la cuenta confirmada y el cuarto reenvío en la hora lanza LimiteDeIntentosException (429)
result: pass
source: automated
coverage_id: 03-10/D6

### 65. Existe una unica funcion pura que decide el acceso (anonimo, desconocida, incompleta, verificada), el admin siempre es verificada y un destino externo nunca es valido
expected: Existe una unica funcion pura que decide el acceso (anonimo, desconocida, incompleta, verificada), el admin siempre es verificada y un destino externo nunca es valido
result: pass
source: automated
coverage_id: 03-11/D1

### 66. Lo quiero reemplaza a Reservar o agendar visita, Mis mensajes aparece en el navbar de quien tiene sesion y /mensajes es una pagina Proximamente
expected: Lo quiero reemplaza a Reservar o agendar visita, Mis mensajes aparece en el navbar de quien tiene sesion y /mensajes es una pagina Proximamente
result: pass
source: automated
coverage_id: 03-13/D4

### 67. Antes de aplicar V5 en produccion existe un backup que se lista con pg_restore y un ensayo con una copia termino con V5 aplicada, Hibernate validando, los mismos conteos, cuentas previas sin DNI y el humo sin fallas
expected: Antes de aplicar V5 en produccion existe un backup que se lista con pg_restore y un ensayo con una copia termino con V5 aplicada, Hibernate validando, los mismos conteos, cuentas previas sin DNI y el humo sin fallas
result: pass
source: automated
coverage_id: 03-15/D1

### 68. El back corre en Railway con V5 aplicada y las variables nuevas, el catalogo sigue sirviendo y el front de Vercel tiene el Client ID de produccion compilado
expected: El back corre en Railway con V5 aplicada y las variables nuevas, el catalogo sigue sirviendo y el front de Vercel tiene el Client ID de produccion compilado
result: pass
source: automated
coverage_id: 03-15/D2

## Summary

total: 68
passed: 68
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none yet]
