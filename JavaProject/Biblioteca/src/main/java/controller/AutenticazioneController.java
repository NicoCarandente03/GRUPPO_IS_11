package controller;

import database.GestorePersistenza;
import entity.Utente;
import entity.Studente;
import entity.Bibliotecario;

public class AutenticazioneController {

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static AutenticazioneController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private AutenticazioneController() {
        this.gestoreDB = new GestorePersistenza();
    }

    public static AutenticazioneController getInstance() {
        if (instance == null) {
            instance = new AutenticazioneController();
        }
        return instance;
    }
}