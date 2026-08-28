package entity;

import external.ServizioDiNotifiche;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notifica inviata a uno studente.
 *
 * Il modello di dominio prevede la dipendenza da ServizioDiNotifiche: per
 * mantenerla senza legare l'entity a una implementazione concreta, il servizio
 * viene iniettato da un costruttore aggiuntivo ed e' marcato Transient, quindi
 * non finisce sul database.
 *
 * L'identificativo non compare fra gli attributi del diagramma ma serve come
 * chiave primaria: viene generato nel costruttore.
 */
@Entity
@Table(name = "Notifica")
public class Notifica {

    @Id
    private String idNotifica;

    private String testo;
    private LocalDateTime dataInvio;

    @ManyToOne
    @JoinColumn(name = "matricola")
    private Studente destinatario;

    @Transient
    private ServizioDiNotifiche servizio;

    public Notifica() {
    }

    public Notifica(String testo) {
        this.idNotifica = UUID.randomUUID().toString();
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

    public String getIdNotifica() {
        return idNotifica;
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
     * fra quelle ricevute, come nel flusso InvioNotifica.
     *
     * Se il servizio non e' stato iniettato la notifica viene comunque
     * registrata: e' il caso delle notifiche solo interne.
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
