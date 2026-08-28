package eccezioni;

/**
 * Errore tecnico del livello di persistenza: connessione non disponibile, query
 * fallita, transazione non completata.
 *
 * Isola i Controller dalle eccezioni specifiche di JDBC e di JPA.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String messaggio) {
        super(messaggio);
    }

    public DataAccessException(String messaggio, Throwable causa) {
        super(messaggio, causa);
    }
}
