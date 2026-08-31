package boundary;

/**
 * Interfaccia del Boundary di monitoraggio, con le operazioni previste dal
 * diagramma delle classi.
 *
 * Di queste, consultazioneStoricoPrenotazioni appartiene al caso d'uso
 * Consultazione Storico Prenotazioni
 */
public interface BoundaryMonitoraggioSale {

    void monitoraggioSala();

    void consultazioneStoricoPrenotazioni();

    void monitoraggioAndamentoServizi();
}
