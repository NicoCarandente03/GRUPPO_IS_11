package boundary;

/**
 * Interfaccia del Boundary per la gestione delle prenotazioni, con le operazioni
 * previste dal diagramma delle classi.
 */
public interface BoundaryPrenotazione {

    void consultazioneDisponibilitaSaleStudio();

    void verificaFasceOrarie();

    void effettuaPrenotazione();
}