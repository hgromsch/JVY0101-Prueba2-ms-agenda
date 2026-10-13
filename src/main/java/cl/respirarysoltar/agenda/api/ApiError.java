package cl.respirarysoltar.agenda.api;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

/** Cuerpo de error estándar devuelto por la API ante cualquier fallo. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        List<String> detalles
) {
    public static ApiError de(int status, String error, String mensaje, HttpServletRequest request) {
        return new ApiError(LocalDateTime.now(), status, error, mensaje, request.getRequestURI(), List.of());
    }

    public static ApiError conDetalles(int status, String error, String mensaje,
                                       HttpServletRequest request, List<String> detalles) {
        return new ApiError(LocalDateTime.now(), status, error, mensaje, request.getRequestURI(), detalles);
    }
}
