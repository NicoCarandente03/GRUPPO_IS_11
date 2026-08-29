package controller;

import database.GestorePersistenza;
import entity.SalaStudio;
import entity.Prenotazione;

public class MonitoraggioSaleController {

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static MonitoraggioSaleController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private MonitoraggioSaleController() {
        this.gestoreDB = new GestorePersistenza();
    }

    public static MonitoraggioSaleController getInstance() {
        if (instance == null) {
            instance = new MonitoraggioSaleController();
        }
        return instance;
    }
}