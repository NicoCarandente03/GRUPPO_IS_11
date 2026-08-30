package controller;

import java.util.List;
import java.util.Date;
import java.time.LocalDate;

import database.GestorePersistenza;
import entity.Studente;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Area;
import entity.Postazione;

import external.ServizioDiNotifiche;

public class GestionePrenotazioneController {
    private int intervalloCheckInMinuti;
    private int limiteAnnullamentoMinuti;

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static GestionePrenotazioneController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private GestionePrenotazioneController() {
        this.gestoreDB = new GestorePersistenza();
    }

    public static GestionePrenotazioneController getInstance() {
        if (instance == null) {
            instance = new GestionePrenotazioneController();
        }
        return instance;
    }

    public List<SalaStudio> consultazioneDisponibilitaSaleStudio(Date data, String fasciaOraria) {
        // Il controller demanda la logica al package database e restituisce il risultato
        return gestoreDB.trovaTutteLeSaleDisponibili(data, fasciaOraria);
    }

    public List<String> visualizzazioneFasceOrarieDisponibili(String idSala, Date data) {
        // Delega totale della logica di estrazione al database
        return gestoreDB.trovaFasceOrarieDisponibili(idSala, data);
    }

    public boolean effettuaPrenotazione(String matricola, LocalDate data, String idSala, String fasciaOraria, String idArea, String idPostazione) {

        boolean esitoCreazione = true;
        Prenotazione nuovaPrenotazione = new Prenotazione();

        try {
            // recupero dello studente dal database
            Studente studente = gestoreDB.trovaPerMatricola(matricola);

            // NOTA: utilizzo UUID per creare una stringa alfanumerica casuale e sicura
            String idUnivoco = java.util.UUID.randomUUID().toString();
            nuovaPrenotazione.setIdPrenotazione(idUnivoco);

            // popolamento dei dati base della prenotazione
            nuovaPrenotazione.setStudente(studente);
            nuovaPrenotazione.setData(data);
            nuovaPrenotazione.setFasciaOraria(fasciaOraria);

            nuovaPrenotazione.setStato(Prenotazione.ATTIVA);

            // popolamento della postazione (solo se è stata effettivamente selezionata)
            if (idPostazione != null && !idPostazione.isEmpty()) {
                Postazione postazione = gestoreDB.trovaPostazionePerId(idPostazione);
                nuovaPrenotazione.setPostazione(postazione);
            }

            gestoreDB.salva(nuovaPrenotazione);

        } catch (Exception e) {
            esitoCreazione = false;
            System.err.println("Errore nel salvataggio della prenotazione: " + e.getMessage());
        }

        // invio finale della notifica solo se il salvataggio è andato a buon fine
        if (esitoCreazione) {
            ServizioDiNotifiche.inviaNotificaConferma(matricola, nuovaPrenotazione);
        }

        return esitoCreazione;
    }
}