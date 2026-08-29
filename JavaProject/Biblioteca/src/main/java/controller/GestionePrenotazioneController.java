package controller;

import database.GestorePersistenza;
import entity.Studente;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Area;
import entity.Postazione;

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
}