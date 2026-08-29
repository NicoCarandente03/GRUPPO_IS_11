package controller;

import database.GestorePersistenza;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Studente;

public class NotificaController {

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static NotificaController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private NotificaController() {
        this.gestoreDB = new GestorePersistenza();
    }

    public static NotificaController getInstance() {
        if (instance == null) {
            instance = new NotificaController();
        }
        return instance;
    }
}