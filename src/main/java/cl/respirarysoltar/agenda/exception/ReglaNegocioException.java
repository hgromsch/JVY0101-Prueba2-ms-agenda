package cl.respirarysoltar.agenda.exception;

/** Se lanza cuando la petición viola una regla de negocio (HTTP 409 Conflict). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
