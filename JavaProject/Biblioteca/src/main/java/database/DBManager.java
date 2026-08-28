package database;

import eccezioni.DataAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Punto unico di accesso al database, come previsto dal diagramma di design.
 *
 * I parametri di connessione non stanno nel persistence.xml ma in
 * src/main/resources/db.properties, che e' escluso da git: cosi' la password non
 * finisce nella repo e ognuno del gruppo usa le proprie credenziali. Chi clona il
 * progetto deve copiare db.properties.example in db.properties e compilarlo.
 *
 * Tiene una sola EntityManagerFactory, che e' costosa da creare. L'EntityManager
 * invece non e' un singleton: ogni operazione deve usare il proprio e chiuderlo
 * alla fine.
 */
public final class DBManager {

    private static final String NOME_PERSISTENCE_UNIT = "BibliotecaPU";
    private static final String FILE_CONFIGURAZIONE = "/db.properties";

    private final Properties configurazione;
    private final String url;

    private volatile EntityManagerFactory emf;

    private DBManager() {
        this.configurazione = caricaConfigurazione();
        this.url = costruisciUrl();
    }

    /** Holder idiom: l'istanza viene creata al primo accesso ed e' thread safe. */
    private static final class Holder {
        private static final DBManager ISTANZA = new DBManager();
    }

    public static DBManager getInstance() {
        return Holder.ISTANZA;
    }

    /** Crea un nuovo EntityManager, da chiudere a fine operazione. */
    public EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /**
     * Connessione JDBC diretta, come dichiarata nel diagramma delle classi.
     *
     * I DAO lavorano con l'EntityManager e non la usano; resta disponibile per le
     * operazioni che dovessero servire in SQL puro. Il chiamante e' responsabile
     * della chiusura.
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url,
                    configurazione.getProperty("db.user"),
                    configurazione.getProperty("db.password"));
        } catch (SQLException e) {
            throw new DataAccessException("Connessione al database non riuscita: " + url, e);
        }
    }

    /** Esegue un'operazione di sola lettura e chiude l'EntityManager. */
    public <T> T esegui(Function<EntityManager, T> operazione) {
        EntityManager em = getEntityManager();
        try {
            return operazione.apply(em);
        } finally {
            em.close();
        }
    }

    /**
     * Esegue un'operazione di scrittura dentro una transazione, con rollback
     * automatico in caso di errore.
     */
    public void eseguiInTransazione(Consumer<EntityManager> operazione) {
        EntityManager em = getEntityManager();
        EntityTransaction transazione = em.getTransaction();
        try {
            transazione.begin();
            operazione.accept(em);
            transazione.commit();
        } catch (RuntimeException e) {
            if (transazione.isActive()) {
                transazione.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /** Chiude la EntityManagerFactory. Va chiamato alla fine dell'applicazione. */
    public void closeConnection() {
        synchronized (this) {
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
            emf = null;
        }
    }

    private EntityManagerFactory getEntityManagerFactory() {
        EntityManagerFactory locale = emf;
        if (locale == null) {
            synchronized (this) {
                locale = emf;
                if (locale == null) {
                    locale = Persistence.createEntityManagerFactory(
                            NOME_PERSISTENCE_UNIT, proprietaJpa());
                    emf = locale;
                }
            }
        }
        return locale;
    }

    /** Parametri di connessione passati a Hibernate a runtime. */
    private Map<String, Object> proprietaJpa() {
        Map<String, Object> proprieta = new HashMap<>();
        proprieta.put("jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver");
        proprieta.put("jakarta.persistence.jdbc.url", url);
        proprieta.put("jakarta.persistence.jdbc.user", configurazione.getProperty("db.user"));
        proprieta.put("jakarta.persistence.jdbc.password", configurazione.getProperty("db.password"));
        return proprieta;
    }

    private String costruisciUrl() {
        String host = configurazione.getProperty("db.host", "localhost");
        String porta = configurazione.getProperty("db.port", "3306");
        String nome = configurazione.getProperty("db.name");
        String parametri = configurazione.getProperty("db.params", "").trim();
        String base = "jdbc:mysql://" + host + ":" + porta + "/" + nome;
        return parametri.isEmpty() ? base : base + "?" + parametri;
    }

    private Properties caricaConfigurazione() {
        Properties proprieta = new Properties();
        try (InputStream in = DBManager.class.getResourceAsStream(FILE_CONFIGURAZIONE)) {
            if (in == null) {
                throw new DataAccessException("File db.properties non trovato nel classpath. "
                        + "Copiare db.properties.example in db.properties e compilarlo.");
            }
            proprieta.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Lettura di db.properties non riuscita", e);
        }
        verificaValorizzato(proprieta, "db.name");
        verificaValorizzato(proprieta, "db.user");
        verificaValorizzato(proprieta, "db.password");
        return proprieta;
    }

    /** Segnala subito i parametri lasciati con il valore di esempio. */
    private void verificaValorizzato(Properties proprieta, String chiave) {
        String valore = proprieta.getProperty(chiave);
        if (valore == null || valore.isBlank() || valore.startsWith("INSERIRE_")) {
            throw new DataAccessException("Parametro " + chiave
                    + " non compilato in db.properties");
        }
    }
}
