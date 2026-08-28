package database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JpaUtil {

    private static JpaUtil instance;
    private EntityManagerFactory emf;

    private JpaUtil() {
        // Il nome "BibliotecaPU" deve essere identico a quello scritto nel tuo file persistence.xml
        this.emf = Persistence.createEntityManagerFactory("BibliotecaPU");
    }

    public static JpaUtil getInstance() {
        if (instance == null) {
            instance = new JpaUtil();
        }
        return instance;
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
