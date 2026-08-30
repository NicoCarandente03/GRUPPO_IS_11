package entity;

import external.ServizioDiNotifiche;

import java.time.LocalDateTime;

/**
 * Messaggio inviato a uno studente.
 *
 * Non e' una entity e non ha una tabella: i requisiti sui dati non elencano la
 * Notifica fra le informazioni da memorizzare, e nel diagramma delle classi
 * compare solo con dipendenze d'uso, non con associazioni. La notifica viene
 * creata, consegnata al servizio esterno e non conservata sul database.
 *
 * Il modello di dominio prevede la dipendenza verso ServizioDiNotifiche: per
 * mantenerla senza legare la classe a una implementazione concreta, il servizio
 * viene iniettato da un costruttore aggiuntivo.
 */
public class Notifica {

    private final String testo;
    private LocalDateTime dataInvio;
    private Studente destinatario;
    private ServizioDiNotifiche servizio;

    public Notifica(String testo) {
        this.testo = testo;
    }

    /**
     * Costruttore usato da NotificaController: riceve il servizio esterno da
     * usare per l'invio, cosi' nei test si puo' passare un mock.
     */
    public Notifica(String testo, ServizioDiNotifiche servizio) {
        this(testo);
        this.servizio = servizio;
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

    public void setServizio(ServizioDiNotifiche servizio) {
        this.servizio = servizio;
    }

    /**
     * Invia la notifica allo studente tramite il servizio esterno e la registra
     * fra quelle ricevute nella sessione, come nel flusso InvioNotifica.
     *
     * Se il servizio non e' stato iniettato la notifica viene comunque
     * registrata: e' il caso dei messaggi solo interni.
     */
    public void invia(Studente destinatario) {
        this.destinatario = destinatario;
        this.dataInvio = LocalDateTime.now();
        if (servizio != null && destinatario != null) {
            servizio.invioNotifica(destinatario.getMatricola(), testo);
        }
        if (destinatario != null) {
            destinatario.riceviNotifica(this);
        }
    }

    @Override
    public String toString() {
        return "[" + dataInvio + "] " + testo;
    }
}
