package boundary;

/**
 * Interfaccia del Boundary di gestione delle sale, con le operazioni previste
 * dal diagramma delle classi.
 *
 * Di queste, creazioneAulaStudio appartiene al caso d'uso Creazione Aula
 * Studio. 
 */
public interface BoundaryGestioneSale {

    void creazioneAulaStudio();

    void modificaAulaStudio();

    void eliminazioneAulaStudio();
}
