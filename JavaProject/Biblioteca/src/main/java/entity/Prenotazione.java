package entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) //UUID (Universally Unique Identifier) per auto-generare una chiave primaria di tipo String
    private String idPrenotazione;

    private LocalDate data;
    private String fasciaOraria;
    private String stato;

    @ManyToOne
    @JoinColumn(name = "matricola_studente")
    private Studente studente;

    @ManyToOne
    @JoinColumn(name = "id_postazione")
    private Postazione postazione;

    public Prenotazione() {
    }

    public Prenotazione(LocalDate data, String fasciaOraria, Studente studente, Postazione postazione) {
        this.data=data;
        this.fasciaOraria=fasciaOraria;
        this.stato="ATTIVA";
        this.studente=studente;
        this.postazione=postazione;
    }

    public String getIdPrenotazione() {
        return idPrenotazione;
    }

    public void setIdPrenotazione(String idPrenotazione) {
        this.idPrenotazione = idPrenotazione;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getFasciaOraria() {
        return fasciaOraria;
    }

    public void setFasciaOraria(String fasciaOraria) {
        this.fasciaOraria = fasciaOraria;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

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
}
