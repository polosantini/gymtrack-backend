package com.gymtrack.pagos.web;


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.gymtrack.pagos.domain.exception.PlanInvalidoException;
import com.gymtrack.pagos.domain.exception.PeriodoInvalidoException;
import com.gymtrack.pagos.web.dto.ErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);



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

    @ExceptionHandler(PeriodoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handlePeriodoInvalido(PeriodoInvalidoException ex, HttpServletRequest request) {
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Falta el parámetro requerido: " + ex.getParameterName(),
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

    @ExceptionHandler(com.gymtrack.pagos.domain.exception.MiembroSinMembresiaException.class)
    public ResponseEntity<ErrorResponse> sinMembresia(RuntimeException ex,HttpServletRequest request) {
        return errorValidacion(HttpStatus.CONFLICT,ex,request);
    }
    @ExceptionHandler(com.gymtrack.pagos.domain.exception.MiembroPagoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> miembroInexistente(RuntimeException ex,HttpServletRequest request) {
        return errorValidacion(HttpStatus.NOT_FOUND,ex,request);
    }
    @ExceptionHandler(com.gymtrack.pagos.domain.exception.ValidacionPagoNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> validacionNoDisponible(RuntimeException ex,HttpServletRequest request) {
        return errorValidacion(HttpStatus.SERVICE_UNAVAILABLE,ex,request);
    }
    private ResponseEntity<ErrorResponse> errorValidacion(HttpStatus status,RuntimeException ex,HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(),status.value(),
                status.getReasonPhrase(),ex.getMessage(),request.getRequestURI()));
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
