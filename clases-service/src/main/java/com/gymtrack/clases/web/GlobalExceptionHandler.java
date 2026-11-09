package com.gymtrack.clases.web;

import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.exception.ReglaReservaIncumplidaException;
import com.gymtrack.clases.domain.exception.EstadoReservaInvalidoException;
import com.gymtrack.clases.domain.exception.FechaNoCorrespondeException;
import com.gymtrack.clases.domain.exception.ReservaDuplicadaException;
import com.gymtrack.clases.domain.exception.ReservaNotFoundException;
import com.gymtrack.clases.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

/**
 * Traduce las excepciones a respuestas JSON con el mismo formato que usan
 * miembros-service y pagos-service: timestamp, status, error, message, path.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReglaReservaIncumplidaException.class)
    public ResponseEntity<ErrorResponse> handleReglaReserva(ReglaReservaIncumplidaException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ClaseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClaseNotFound(ClaseNotFoundException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ReservaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservaNotFound(ReservaNotFoundException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(EstadoReservaInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleEstadoReservaInvalido(EstadoReservaInvalidoException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(FechaNoCorrespondeException.class)
    public ResponseEntity<ErrorResponse> handleFechaNoCorresponde(FechaNoCorrespondeException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ReservaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleReservaDuplicada(ReservaDuplicadaException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje;
        if (ex.getBindingResult().getFieldErrors().isEmpty()) {
            mensaje = "Datos de entrada inválidos";
        } else {
            mensaje = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        return construir(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    /** JSON mal formado: hora que no es HH:mm, fecha que no es yyyy-MM-dd, día que no existe, etc. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBodyIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "Cuerpo de la petición inválido. Revisa los formatos: fecha yyyy-MM-dd, hora HH:mm y días LUNES a DOMINGO",
                request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "Falta el parámetro requerido: " + ex.getParameterName(), request);
    }

    /** Parámetro con formato incorrecto, por ejemplo ?fecha=17-11-2026 o /api/clases/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST,
                "Valor inválido para el parámetro '" + ex.getName() + "': " + ex.getValue(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        // Errores propios de Spring (ruta inexistente, método no permitido...) conservan su código HTTP
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus httpStatus = HttpStatus.resolve(status.value());
            String mensaje = httpStatus != null ? httpStatus.getReasonPhrase() : "Error en la petición";
            return construir(status, mensaje, request);
        }

        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatusCode status, String mensaje, HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                httpStatus != null ? httpStatus.getReasonPhrase() : String.valueOf(status.value()),
                mensaje,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
