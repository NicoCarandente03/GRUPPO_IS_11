package controller;

import java.util.List;
import java.util.Date;
import java.time.LocalDate;

import database.GestorePersistenza;

import entity.Studente;
import entity.Prenotazione;
import entity.SalaStudio;
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



    public List<SalaStudio> consultazioneDisponibilitaSaleStudio(LocalDate data, String fasciaOraria) {
        // Il controller demanda la logica al package database e restituisce il risultato
        return gestoreDB.trovaTutteLeSaleDisponibili(data, fasciaOraria);
    }

    public List<String> visualizzazioneFasceOrarieDisponibili(String idSala, LocalDate data) {
        // Delega totale della logica di estrazione al database
        return gestoreDB.trovaFasceOrarieDisponibili(idSala, data);
    }

    public boolean effettuaPrenotazione(String matricola, LocalDate data, String idSala, String fasciaOraria, String idArea, String idPostazione) {
        boolean esitoCreazione = true;
        Prenotazione nuovaPrenotazione = new Prenotazione();

        try {
            Studente studente = gestoreDB.trovaPerMatricola(matricola);

            // Creazione UUID univoco per il database
            String idUnivoco = java.util.UUID.randomUUID().toString();
            nuovaPrenotazione.setIdPrenotazione(idUnivoco);

            nuovaPrenotazione.setStudente(studente);
            nuovaPrenotazione.setData(data);
            nuovaPrenotazione.setFasciaOraria(fasciaOraria);

            // Set dello stato a partire dalla costante di classe
            nuovaPrenotazione.setStato(Prenotazione.ATTIVA);

            // Assegnazione della postazione specifica (se prevista)
            if (idPostazione != null && !idPostazione.isEmpty()) {
                Postazione postazione = gestoreDB.trovaPostazionePerId(idPostazione);
                nuovaPrenotazione.setPostazione(postazione);
            } else {
                // Se l'utente ha scelto solo la Sala, assegno in automatico la prima postazione libera trovata
                List<Postazione> postazioniLibere = gestoreDB.trovaPostazioniLibere(idSala, data, fasciaOraria);
                if (postazioniLibere != null && !postazioniLibere.isEmpty()) {
                    nuovaPrenotazione.setPostazione(postazioniLibere.get(0));
                } else {
                    throw new Exception("Nessuna postazione libera in questa sala per l'orario scelto.");
                }
            }

            // Delegazione del salvataggio al DB
            gestoreDB.salva(nuovaPrenotazione);

        } catch (Exception e) {
            esitoCreazione = false;
            System.err.println("Errore nel salvataggio della prenotazione: " + e.getMessage());
        }

        // Se tutto va a buon fine, inviamo la notifica tramite l'attore esterno
        if (esitoCreazione) {
            ServizioDiNotifiche.inviaNotificaConferma(matricola, nuovaPrenotazione);
        }

        return esitoCreazione;
    }
}