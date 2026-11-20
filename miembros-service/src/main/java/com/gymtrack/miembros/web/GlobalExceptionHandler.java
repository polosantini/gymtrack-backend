package com.gymtrack.miembros.web;

import com.gymtrack.miembros.domain.exception.EmailDuplicadoException;
import com.gymtrack.miembros.domain.exception.MiembroNotFoundException;
import com.gymtrack.miembros.domain.exception.PlanInvalidoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.gymtrack.miembros.web.dto.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MiembroNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMiembroNotFound(MiembroNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleEmailDuplicado(EmailDuplicadoException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(PlanInvalidoException.class)
    public ResponseEntity<ErrorResponse> handlePlanInvalido(PlanInvalidoException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        String mensaje;
        if (ex.getBindingResult().getFieldErrors().isEmpty()) {
            mensaje = "Datos de entrada inválidos";
        } else {
            mensaje = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> entradaInvalida(Exception ex,HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(LocalDateTime.now(),400,"Bad Request",
                "Datos de entrada invalidos; revise los tipos y formatos",request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        if(ex instanceof org.springframework.web.ErrorResponse errorHttp) {
            var status=errorHttp.getStatusCode();
            var nombre=HttpStatus.resolve(status.value());
            String texto=nombre==null?"Error en la peticion":nombre.getReasonPhrase();
            return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(),status.value(),texto,texto,request.getRequestURI()));
        }

        log.error("Error inesperado en {}", request.getRequestURI(), ex);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Error interno del servidor",
                request.getRequestURI()
        );


        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
