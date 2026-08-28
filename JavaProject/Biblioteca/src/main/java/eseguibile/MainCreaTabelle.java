package eseguibile;

import database.DBManager;
import jakarta.persistence.EntityManager;

/**
 * Eseguibile di servizio: apre la connessione e lascia che Hibernate crei o
 * aggiorni le tabelle a partire dalle entity.
 *
 * Va lanciato una volta prima del primo avvio dell'applicazione, oppure dopo
 * aver aggiunto o modificato una entity.
 */
public class MainCreaTabelle {

    public static void main(String[] args) {
        System.out.println("Creazione delle tabelle in corso...");

        DBManager dbManager = DBManager.getInstance();
        EntityManager em = dbManager.getEntityManager();

        System.out.println("Tabelle create e mapping delle entity caricato.");

        em.close();
        dbManager.closeConnection();
    }
}
