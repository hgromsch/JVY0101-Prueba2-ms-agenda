package cl.respirarysoltar.agenda.exception;

/** Se lanza cuando el recurso solicitado no existe en la base de datos (HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
