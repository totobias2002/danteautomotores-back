package com.danteautomotores.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Único advice de la API: toda respuesta de error tiene el formato {"error": "mensaje en español"} y, en las
 * validaciones, además {"campos": {campo: mensaje}}.
 * <p>
 * Las excepciones de Spring MVC (JSON mal formado, tipo de path inválido, parte multipart faltante, 404 de ruta,
 * 405, 413, 415, etc.) NO tienen un @ExceptionHandler propio: las resuelve la clase base y acá solo se
 * personalizan los overrides. Declarar un handler explícito para una de ellas (MethodArgumentNotValidException,
 * MaxUploadSizeExceededException) deja un mapeo ambiguo y la app no arranca.
 * <p>
 * Los errores del filtro JWT (token inválido o vencido) no pasan por acá: los responde RestAuthenticationEntryPoint.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Solo las reglas de negocio lanzadas a propósito llegan al cliente con su mensaje. Un IllegalArgumentException
    // cualquiera (Spring, Hibernate, JDK) cae en handleUnexpected: se loguea y responde un 500 genérico.
    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleReglaDeNegocio(ReglaDeNegocioException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    // Tiene que existir: sin él, el handler de Exception convertiría un AccessDenied lanzado desde un service en 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "No tenés permiso para realizar esta acción.");
    }

    // Nunca se devuelve el mensaje de la excepción: trae nombres de constraints y SQL.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "No se pudo completar la operación porque hay datos relacionados.");
    }

    // La fila de la publicación está bloqueada por otra operación sobre sus fotos y se agotó la espera (ver
    // PublicacionRepository.findByIdForUpdate). Es transitorio: el cliente puede reintentar.
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleBloqueo(PessimisticLockingFailureException ex) {
        log.warn("No se obtuvo el lock de la fila a tiempo: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "Otra operación está modificando este auto. Intentá de nuevo en unos segundos.");
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<Map<String, Object>> handleServicioExterno(ServicioExternoException ex) {
        log.error("Falla de un servicio externo", ex);
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá de nuevo más tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", "Datos inválidos");
        cuerpo.put("campos", campos);
        return ResponseEntity.status(status).headers(headers).body(cuerpo);
    }

    // Reemplaza el ProblemDetail por defecto por el formato uniforme, conservando la lógica de la base
    // (response ya commiteada, headers como Allow en el 405).
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        return super.handleExceptionInternal(ex, Map.of("error", mensajePorStatus(statusCode.value())),
                headers, statusCode, request);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }

    private String mensajePorStatus(int status) {
        return switch (status) {
            case 400 -> "La solicitud tiene datos inválidos o mal formados.";
            case 401 -> "Credenciales inválidas";
            case 403 -> "No tenés permiso para realizar esta acción.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "Método no permitido para esta ruta.";
            case 413 -> "El archivo supera el tamaño máximo permitido (10 MB).";
            case 415 -> "Tipo de contenido no soportado.";
            default -> "Ocurrió un error al procesar la solicitud.";
        };
    }
}
