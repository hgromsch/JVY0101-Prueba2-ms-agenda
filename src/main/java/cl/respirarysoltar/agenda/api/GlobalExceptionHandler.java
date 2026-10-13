package cl.respirarysoltar.agenda.api;

import cl.respirarysoltar.agenda.exception.RecursoNoEncontradoException;
import cl.respirarysoltar.agenda.exception.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Manejo centralizado de errores: traduce las excepciones de negocio y de
 * validación a respuestas HTTP con un cuerpo de error consistente (ApiError).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.de(404, "Recurso no encontrado", ex.getMessage(), request));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.de(409, "Regla de negocio violada", ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.conDetalles(400, "Datos de entrada inválidos",
                        "La petición no supera la validación de campos", request, detalles));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> peticionMalFormada(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiError.de(400, "Petición mal formada",
                        "El cuerpo o los parámetros de la petición no son válidos", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> errorInesperado(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.de(500, "Error interno", "Ocurrió un error inesperado", request));
    }
}
