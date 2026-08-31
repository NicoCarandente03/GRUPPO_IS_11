package boundary;

/**
 * Interfaccia del Boundary del profilo studente, con le operazioni previste dal
 * diagramma delle classi.
 *
 * Le classi di questo package si occupano della sola presentazione: non toccano
 * mai le Entity ne' il livello Database, ma passano sempre da un Controller e
 * ricevono indietro dei DTO.
 *
 * Di queste operazioni, visualizzaPrenotazioniEffettuate e
 * annullamentoPrenotazione appartengono al caso d'uso Annullamento Prenotazione.
 */
public interface BoundaryProfiloStudente {

    void visualizzaPrenotazioniEffettuate();

    void annullamentoPrenotazione();

    void checkin();

    void consultaAccessi();

    void visualizzaNotifiche();
}
