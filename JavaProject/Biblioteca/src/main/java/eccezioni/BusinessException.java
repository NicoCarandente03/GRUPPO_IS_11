package eccezioni;

/**
 * Errore di regola di business: dati non validi, vincolo di dominio violato,
 * operazione non consentita nello stato corrente.
 *
 * Viene sollevata dai Controller e intercettata dai Boundary, che la traducono
 * nel messaggio mostrato all'utente. Serve a mantenere le firme previste dal
 * diagramma delle classi anche dove il metodo non ha un valore di ritorno con
 * cui segnalare l'esito negativo.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String messaggio) {
        super(messaggio);
    }

    public BusinessException(String messaggio, Throwable causa) {
        super(messaggio, causa);
    }
}
