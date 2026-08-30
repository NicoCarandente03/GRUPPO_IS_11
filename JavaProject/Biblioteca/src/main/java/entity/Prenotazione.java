package entity;

import eccezioni.BusinessException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Prenotazione di una postazione da parte di uno studente, per una data e una
 * fascia oraria.
 *
 * Non tiene riferimenti diretti all'Area e alla SalaStudio: si risalgono
 * navigando la postazione prenotata, con getPostazione, getArea e getSalaStudio.
 * Tenerli anche qui sarebbe una ridondanza.
 *
 * I predicati temporali hanno un overload che riceve l'istante di riferimento:
 * la versione senza parametro usa l'ora corrente, quella con parametro rende i
 * metodi verificabili nei test senza dipendere dall'orologio di sistema.
 */
@Entity
@Table(name = "Prenotazione")
public class Prenotazione {

    // L'id lo assegna il chiamante, come nel costruttore del diagramma.
    @Id
    private String idPrenotazione;

    private LocalDate data;
    private String fasciaOraria;

    // Valori ammessi per lo stato, che il diagramma delle classi dichiara String
    public static final String ATTIVA = "ATTIVA";
    public static final String CONFERMATA = "CONFERMATA";
    public static final String ANNULLATA = "ANNULLATA";
    public static final String SCADUTA = "SCADUTA";

    private String stato;

    //LI HA AGGIUNTI NICO, POI PARLAIMO, NON SO COME RISOLVERE
    private Area area;
    private SalaStudio sala;

    @ManyToOne
    @JoinColumn(name = "matricola")
    private Studente studente;

    @ManyToOne
    @JoinColumn(name = "idPostazione")
    private Postazione postazione;

    public Prenotazione() {
    }

    public Prenotazione(String idPrenotazione, LocalDate data, String fasciaOraria,
                        Studente studente, Postazione postazione) {
        if (!FasceOrarie.isValida(fasciaOraria)) {
            throw new BusinessException("Fascia oraria non valida: " + fasciaOraria);
        }
        this.idPrenotazione = idPrenotazione;
        this.data = data;
        this.fasciaOraria = fasciaOraria;
        this.studente = studente;
        this.postazione = postazione;
        attiva();

        // allineamento dei lati inversi delle associazioni
        if (studente != null) {
            studente.aggiungiPrenotazione(this);
        }
        if (postazione != null) {
            postazione.aggiungiPrenotazione(this);
        }
    }

    public String getIdPrenotazione() {
        return idPrenotazione;
    }
    public void setIdPrenotazione(String idPrenotazione) { this.idPrenotazione = idPrenotazione; }

    public LocalDate getData() {
        return data;
    }
    public void setData(LocalDate data) { this.data = data; }

    public String getFasciaOraria() {
        return fasciaOraria;
    }
    public void setFasciaOraria(String fasciaOraria) { this.fasciaOraria = fasciaOraria; }

    public String getStato() {
        return stato;
    }
    public void setStato(String stato) { this.stato = stato; }

    public Studente getStudente() {
        return studente;
    }
    public void setStudente(Studente studente) {
        this.studente = studente;
    }

    public Postazione getPostazione() {
        return postazione;
    }
    public void setPostazione(Postazione postazione) {
        this.postazione = postazione;
    }

    /** Area della postazione prenotata, risalita per navigazione. */
    public Area getArea() {
        return postazione == null ? null : postazione.getArea();
    }
    public void setArea(Area area) {
        this.area = area;
    }

    /** Sala della postazione prenotata, risalita per navigazione. */
    public SalaStudio getSala() {
        Area areaPrenotata = getArea();
        return areaPrenotata == null ? null : areaPrenotata.getSalaStudio();
    }
    public void setSala(SalaStudio sala) {
        this.sala = sala;
    }



    /**
     * Registra la presenza dello studente
     */
    public boolean checkin(int intervalloCheckinMinuti) {
        return checkin(intervalloCheckinMinuti, LocalDateTime.now());
    }

    public boolean checkin(int intervalloCheckinMinuti, LocalDateTime adesso) {
        if (!isCheckinValido(intervalloCheckinMinuti, adesso)) {
            return false;
        }
        conferma();
        return true;
    }

    /**
     * Annulla la prenotazione se il limite temporale non e' ancora superato e
     * libera la postazione.
     */
    public boolean annulla(int limiteAnnullamentoMinuti) {
        return annulla(limiteAnnullamentoMinuti, LocalDateTime.now());
    }

    public boolean annulla(int limiteAnnullamentoMinuti, LocalDateTime adesso) {
        if (!isAnnullabile(limiteAnnullamentoMinuti, adesso)) {
            return false;
        }
        this.stato = ANNULLATA;
        if (postazione != null) {
            postazione.rendiDisponibile();
        }
        return true;
    }

    /**
     * Fa scadere la prenotazione per mancato check-in e libera la postazione,
     * come nel flusso GestioneScadenzaAutomaticaPrenotazione.
     */
    public void scadi() {
        if (!ATTIVA.equals(stato)) {
            return;
        }
        this.stato = SCADUTA;
        if (postazione != null) {
            postazione.rendiDisponibile();
        }
    }

    /**
     * Cambia lo stato della prenotazione. Corrisponde al messaggio
     * aggiornaStatoPrenotazione(azione) del flusso AggiornamentoStatoPrenotazione,
     * che smista verso attiva, conferma, annulla e scadi.
     */
    public void aggiornaStatoPrenotazione(String nuovoStato) {
        switch (normalizza(nuovoStato)) {
            case ATTIVA -> attiva();
            case CONFERMATA -> conferma();
            case ANNULLATA -> {
                this.stato = ANNULLATA;
                if (postazione != null) {
                    postazione.rendiDisponibile();
                }
            }
            case SCADUTA -> scadi();
            default -> throw new BusinessException(
                    "Stato della prenotazione non riconosciuto: " + nuovoStato);
        }
    }

    /**
     * Vero se il check-in ricade nella finestra consentita, che si estende per
     * intervalloCheckinMinuti prima e dopo l'inizio della fascia oraria: lo
     * studente puo' confermare poco prima di entrare e ha ancora un margine dopo
     * l'inizio, superato il quale la prenotazione scade.
     *
     * Il valore dell'intervallo e' configurabile
     */
    public boolean isCheckinValido(int intervalloCheckinMinuti) {
        return isCheckinValido(intervalloCheckinMinuti, LocalDateTime.now());
    }

    public boolean isCheckinValido(int intervalloCheckinMinuti, LocalDateTime adesso) {
        if (!ATTIVA.equals(stato)) {
            return false;
        }
        LocalDateTime inizio = FasceOrarie.inizio(data, fasciaOraria);
        LocalDateTime apertura = inizio.minusMinutes(intervalloCheckinMinuti);
        LocalDateTime chiusura = inizio.plusMinutes(intervalloCheckinMinuti);
        return !adesso.isBefore(apertura) && !adesso.isAfter(chiusura);
    }

    /**
     * Vero se mancano ancora almeno limiteAnnullamentoMinuti all'inizio della
     * fascia. Vale solo per le prenotazioni non ancora confermate.
     */
    public boolean isAnnullabile(int limiteAnnullamentoMinuti) {
        return isAnnullabile(limiteAnnullamentoMinuti, LocalDateTime.now());
    }

    public boolean isAnnullabile(int limiteAnnullamentoMinuti, LocalDateTime adesso) {
        if (!ATTIVA.equals(stato)) {
            return false;
        }
        LocalDateTime inizio = FasceOrarie.inizio(data, fasciaOraria);
        return adesso.isBefore(inizio.minusMinutes(limiteAnnullamentoMinuti));
    }

    /**
     * Vero se l'inizio della fascia e' imminente, cioe' entro minutiPreavviso.
     * Usato da NotificaController per l'invio del promemoria.
     */
    public boolean isInAvvicinamento(int minutiPreavviso) {
        return isInAvvicinamento(minutiPreavviso, LocalDateTime.now());
    }

    public boolean isInAvvicinamento(int minutiPreavviso, LocalDateTime adesso) {
        if (!ATTIVA.equals(stato)) {
            return false;
        }
        LocalDateTime inizio = FasceOrarie.inizio(data, fasciaOraria);
        return !adesso.isAfter(inizio) && adesso.isAfter(inizio.minusMinutes(minutiPreavviso));
    }

    /** Testo con sala, data e fascia oraria da inserire nel promemoria. */
    public String getDatiPromemoria() {
        SalaStudio sala = getSala();
        String nomeSala = sala == null ? "sala non specificata" : sala.getNome();
        return "Prenotazione presso " + nomeSala + " del " + data + ", fascia " + fasciaOraria;
    }

    /** Vero se la prenotazione occupa ancora la postazione. */
    public boolean isOccupante() {
        return ATTIVA.equals(stato) || CONFERMATA.equals(stato);
    }

    /**
     * Vero se la prenotazione impegna la postazione nello slot indicato. Usato da
     * Postazione.isLibera.
     */
    boolean occupa(LocalDate data, String fasciaOraria) {
        return isOccupante()
                && this.data.equals(data)
                && this.fasciaOraria.equals(fasciaOraria);
    }

    private void attiva() {
        this.stato = ATTIVA;
    }

    private void conferma() {
        this.stato = CONFERMATA;
    }

    /** Riporta lo stato ricevuto alla forma usata internamente. */
    private static String normalizza(String stato) {
        if (stato == null) {
            throw new BusinessException("Stato della prenotazione non specificato");
        }
        return stato.trim().toUpperCase();
    }

    @Override
    public String toString() {
        return "Prenotazione " + idPrenotazione + " del " + data + " " + fasciaOraria
                + " [" + stato + "]";
    }
}
