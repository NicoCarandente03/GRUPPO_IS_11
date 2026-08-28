package database;

import jakarta.persistence.EntityManager;

/**
 * Scorciatoia per ottenere un EntityManager senza passare esplicitamente da
 * DBManager.
 *
 * Non apre una EntityManagerFactory propria: delega tutto a DBManager, che resta
 * il singleton di accesso ai dati previsto dal diagramma di design. Cosi' i
 * parametri di connessione restano in db.properties invece di stare nel
 * persistence.xml.
 */
public final class JpaUtil {

    private JpaUtil() {
    }

    /** Holder idiom: creazione al primo accesso e thread safe. */
    private static final class Holder {
        private static final JpaUtil ISTANZA = new JpaUtil();
    }

    public static JpaUtil getInstance() {
        return Holder.ISTANZA;
    }

    public EntityManager getEntityManager() {
        return DBManager.getInstance().getEntityManager();
    }

    public void chiudi() {
        DBManager.getInstance().closeConnection();
    }
}
