package dto;

import java.time.LocalDate;

/**
 * Dati di una prenotazione nella forma che serve alle finestre.
 *
 * Il package DTO nasce dall'esigenza di mostrare sulla GUI collezioni di
 * elementi: se un controller restituisse un elenco di entity, la finestra
 * dovrebbe conoscere la struttura interna delle Entity per ricavare, ad esempio,
 * il nome della sala navigando postazione, area e sala. Sarebbe un accoppiamento
 * troppo alto fra i due livelli.
 *
 * I campi sono quelli che il piano di test si aspetta di vedere a video:
 * studente, sala, data, fascia oraria e stato.
 */
public class PrenotazioneDTO {

    private final String idPrenotazione;
    private final String matricolaStudente;
    private final String nomeSala;
    private final String tipoArea;
    private final String idPostazione;
    private final LocalDate data;
    private final String fasciaOraria;
    private final String stato;

    public PrenotazioneDTO(String idPrenotazione, String matricolaStudente, String nomeSala,
                           String tipoArea, String idPostazione, LocalDate data,
                           String fasciaOraria, String stato) {
        this.idPrenotazione = idPrenotazione;
        this.matricolaStudente = matricolaStudente;
        this.nomeSala = nomeSala;
        this.tipoArea = tipoArea;
        this.idPostazione = idPostazione;
        this.data = data;
        this.fasciaOraria = fasciaOraria;
        this.stato = stato;
    }

    public String getIdPrenotazione() {
        return idPrenotazione;
    }

    public String getMatricolaStudente() {
        return matricolaStudente;
    }

    public String getNomeSala() {
        return nomeSala;
    }

    public String getTipoArea() {
        return tipoArea;
    }

    public String getIdPostazione() {
        return idPostazione;
    }

    public LocalDate getData() {
        return data;
    }

    public String getFasciaOraria() {
        return fasciaOraria;
    }

    public String getStato() {
        return stato;
    }
}
