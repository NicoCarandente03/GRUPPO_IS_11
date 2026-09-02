package entity;

import java.time.LocalDateTime;

/**
 * Messaggio inviato a uno studente.
 *
 * Non e' una entity e non ha una tabella: i requisiti sui dati non elencano la
 * Notifica fra le informazioni da memorizzare, e nel diagramma delle classi
 * compare solo con dipendenze d'uso, non con associazioni. La notifica viene
 * creata, consegnata allo studente e non conservata sul database.
 *
 * La classe non conosce il servizio esterno di invio: e' il controller a
 * chiamarlo passando matricola e testo. Cosi' il dominio resta indipendente
 * dal canale di consegna.
 */
public class Notifica {

    private final String testo;
    private LocalDateTime dataInvio;
    private Studente destinatario;

    public Notifica(String testo) {
        this.testo = testo;
    }

    public String getTesto() {
        return testo;
    }

    public LocalDateTime getDataInvio() {
        return dataInvio;
    }

    public Studente getDestinatario() {
        return destinatario;
    }

    /**
     * Registra la notifica fra quelle ricevute dallo studente, come nel flusso
     * InvioNotifica. L'invio vero e proprio verso l'esterno lo fa il
     * controller.
     */
    public void invia(Studente destinatario) {
        this.destinatario = destinatario;
        this.dataInvio = LocalDateTime.now();
        if (destinatario != null) {
            destinatario.riceviNotifica(this);
        }
    }

    @Override
    public String toString() {
        return "[" + dataInvio + "] " + testo;
    }
}
