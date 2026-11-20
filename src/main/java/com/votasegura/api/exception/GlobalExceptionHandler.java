package com.votasegura.api.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones.
 * Todas las respuestas de error siguen el estándar RFC 7807 (ProblemDetail).
 * Estructura de error estandarizada:
 * <pre>
 * {
 *   "type": "https://api.votasegura.com/errors/<slug>",
 *   "title": "Descripción del error",
 *   "status": 4xx | 5xx,
 *   "detail": "Mensaje descriptivo del error",
 *   "code": "CODIGO_ERROR",
 *   "timestamp": "2024-11-17T12:00:00"
 * }
 * </pre>
 * Códigos de error manejados:
 * 400 INVALID_INPUT - Validación de campos fallida
 * 403 CUENTA_DESACTIVADA - Usuario desactivado en el sistema
 * 409 DATA_INTEGRITY_CONFLICT - Voto duplicado o correo ya registrado
 * 413 PAYLOAD_TOO_LARGE - Archivo supera 10 MB
 * 500 INTERNAL_SERVER_ERROR - Error interno no controlado
 */
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_ERROR_URL = "https://api.votasegura.com/errors/";

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setType(URI.create(BASE_ERROR_URL + slugify(ex.getCode())));
        pd.setProperty("code", ex.getCode());
        pd.setProperty("timestamp", LocalDateTime.now().toString());
        return pd;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Tu cuenta ha sido desactivada.");
        pd.setType(URI.create(BASE_ERROR_URL + "cuenta-desactivada"));
        pd.setTitle("Cuenta Desactivada");
        pd.setProperty("code", "CUENTA_DESACTIVADA");
        pd.setProperty("timestamp", LocalDateTime.now().toString());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(". "));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(URI.create(BASE_ERROR_URL + "invalid-input"));
        pd.setTitle("Parámetros de entrada no válidos");
        pd.setProperty("code", "INVALID_INPUT");
        pd.setProperty("timestamp", LocalDateTime.now().toString());
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "El recurso ya existe o la operación viola una restricción de datos únicos (ej. correo o voto duplicado).");
        pd.setType(URI.create(BASE_ERROR_URL + "conflict"));
        pd.setTitle("Conflicto de Datos");
        pd.setProperty("code", "DATA_INTEGRITY_CONFLICT");
        pd.setProperty("timestamp", LocalDateTime.now().toString());
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE, "El archivo excede el tamaño máximo permitido de 10 MB.");
        pd.setType(URI.create(BASE_ERROR_URL + "payload-too-large"));
        pd.setTitle("Archivo Demasiado Grande");
        pd.setProperty("code", "PAYLOAD_TOO_LARGE");
        pd.setProperty("timestamp", LocalDateTime.now().toString());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        String msg = ex.getMessage();
        if (msg != null && (msg.toLowerCase().contains("user is disabled") || msg.toLowerCase().contains("disabled"))) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN, "Tu cuenta ha sido desactivada.");
            pd.setType(URI.create(BASE_ERROR_URL + "cuenta-desactivada"));
            pd.setTitle("Cuenta Desactivada");
            pd.setProperty("code", "CUENTA_DESACTIVADA");
            pd.setProperty("timestamp", LocalDateTime.now().toString());
            return pd;
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor: " + msg);
        pd.setType(URI.create(BASE_ERROR_URL + "internal-error"));
        pd.setProperty("code", "INTERNAL_SERVER_ERROR");
        pd.setProperty("timestamp", LocalDateTime.now().toString());
        return pd;
    }

    private String slugify(String code) {
        return code.toLowerCase().replace("_", "-");
    }
}
