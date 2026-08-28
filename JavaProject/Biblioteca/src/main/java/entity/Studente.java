package entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Studente registrato al servizio. Estende Utente e aggiunge la matricola, che
 * e' anche la chiave primaria della tabella, e il contatore degli accessi.
 */
@Entity
@Table(name = "Studente")
public class Studente extends Utente {

    @Id
    private String matricola;

    private int numAccessiTotali;

    // Associazione 1 - 0..* con Prenotazione
    @OneToMany(mappedBy = "studente", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Prenotazione> prenotazioni = new ArrayList<>();

    // Lato inverso della notifica: sola lettura, il salvataggio passa da NotificaDAO
    @OneToMany(mappedBy = "destinatario")
    private List<Notifica> notifiche = new ArrayList<>();

    public Studente() {
    }

    public Studente(String nome, String cognome, String email, String password, String matricola) {
        super(nome, cognome, email, password, RUOLO_STUDENTE);
        this.matricola = matricola;
        this.numAccessiTotali = 0;
        // prenotazioni e notifiche restano liste vuote (molteplicita' 0..*)
    }

    public String getMatricola() {
        return matricola;
    }

    /**
     * Elenco delle prenotazioni dello studente.
     *
     * Restituisce una vista non modificabile, cosi' la lista si aggiorna solo
     * tramite aggiungiPrenotazione e la coerenza dell'associazione resta a
     * carico dell'entity.
     */
    public List<Prenotazione> elencaPrenotazioni() {
        return Collections.unmodifiableList(prenotazioni);
    }

    /** Prenotazioni non ancora annullate ne' scadute. */
    public List<Prenotazione> elencaPrenotazioniAttive() {
        List<Prenotazione> attive = new ArrayList<>();
        for (Prenotazione prenotazione : prenotazioni) {
            if (prenotazione.isOccupante()) {
                attive.add(prenotazione);
            }
        }
        return attive;
    }

    public void aggiungiPrenotazione(Prenotazione prenotazione) {
        if (prenotazione == null || prenotazioni.contains(prenotazione)) {
            return;
        }
        prenotazioni.add(prenotazione);
    }

    public int getNumAccessiTotali() {
        return numAccessiTotali;
    }

    public void incrementaAccessi() {
        this.numAccessiTotali++;
    }

    /** Registra una notifica ricevuta, come nel flusso InvioNotifica. */
    public void riceviNotifica(String testo) {
        riceviNotifica(new Notifica(testo));
    }

    /**
     * Overload usato da Notifica.invia, che ha gia' costruito l'oggetto e non
     * deve crearne un duplicato.
     */
    public void riceviNotifica(Notifica notifica) {
        if (notifica == null || notifiche.contains(notifica)) {
            return;
        }
        notifiche.add(notifica);
    }

    public List<Notifica> getNotifiche() {
        return Collections.unmodifiableList(notifiche);
    }
}
