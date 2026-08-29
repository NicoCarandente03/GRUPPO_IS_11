package controller;

import database.GestorePersistenza;
import entity.SalaStudio;
import entity.Bibliotecario;

public class GestioneSaleController {

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static GestioneSaleController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private GestioneSaleController() {
        this.gestoreDB = new GestorePersistenza();
    }

    public static GestioneSaleController getInstance() {
        if (instance == null) {
            instance = new GestioneSaleController();
        }
        return instance;
    }
}