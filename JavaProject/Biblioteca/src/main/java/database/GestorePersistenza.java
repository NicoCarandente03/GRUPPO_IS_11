package database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import entity.FasceOrarie;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Studente;
import entity.Postazione;
import entity.Area;
import entity.Bibliotecario;

import java.util.ArrayList;
import java.util.Date;
import java.time.LocalDate;
import java.util.List;

public class GestorePersistenza {

    //
    // --- METODI CRUD BASE ---
    //

    public void salva(Object entity) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();

            throw new RuntimeException("Errore durante il salvataggio nel database.");
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Salva piu' oggetti nella stessa transazione.
     */
    public void salvaTutti(Object... entita) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            for (Object entita_i : entita) {
                em.persist(entita_i);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();

            throw new RuntimeException("Errore durante il salvataggio multiplo nel database.");
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    //Metodi utilizzati in AnnullamentoPrenotazione

    /**
     * Cerca una prenotazione dalla sua chiave primaria.
     *
     * Restituisce null se non esiste: e' il caso di test in cui lo studente
     * indica un identificativo inesistente.
     */
    public Prenotazione trovaPrenotazionePerId(String idPrenotazione) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        try {
            return em.find(Prenotazione.class, idPrenotazione);
        } catch (Exception e) {
            System.err.println("Errore durante la ricerca della prenotazione: " + e.getMessage());
            return null;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Elenco delle prenotazioni di uno studente, dalla piu' recente.
     */
    public List<Prenotazione> trovaPrenotazioniPerStudente(String matricola) {
        EntityManager em = DBManager.getInstance().getEntityManager();
        List<Prenotazione> prenotazioni = new ArrayList<>();

        try {
            String jpql = "SELECT p FROM Prenotazione p "
                    + "WHERE p.studente.matricola = :matricola "
                    + "ORDER BY p.data DESC, p.fasciaOraria";

            TypedQuery<Prenotazione> query = em.createQuery(jpql, Prenotazione.class);
            query.setParameter("matricola", matricola);

            prenotazioni = query.getResultList();

        } catch (Exception e) {
            System.err.println("Errore durante la ricerca delle prenotazioni: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return prenotazioni;
    }

    //Metodi utilizzati in CreazioneAulaStudio

    /**
     * Cerca una sala dal nome, che il modello di dominio vuole unico.
     *
     * Restituisce null se non esiste: e' il controllo che impedisce di creare
     * due sale con lo stesso nome.
     */
    public SalaStudio trovaSalaPerNome(String nome) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        try {
            String jpql = "SELECT s FROM SalaStudio s WHERE s.nome = :nome";
            TypedQuery<SalaStudio> query = em.createQuery(jpql, SalaStudio.class);
            query.setParameter("nome", nome);

            List<SalaStudio> risultati = query.getResultList();
            return risultati.isEmpty() ? null : risultati.get(0);

        } catch (Exception e) {
            System.err.println("Errore durante la ricerca della sala per nome: " + e.getMessage());
            return null;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Cerca un bibliotecario dalla sua chiave primaria.
     */
    public Bibliotecario trovaPerCodiceIdentificativo(String codiceIdentificativo) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        try {
            return em.find(Bibliotecario.class, codiceIdentificativo);
        } catch (Exception e) {
            System.err.println("Errore durante la ricerca del bibliotecario: " + e.getMessage());
            return null;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    //Metodo utilizzato in ConsultazioneStoricoPrenotazioni

    /**
     * Storico delle prenotazioni, con due filtri entrambi facoltativi.
     *
     * La parte WHERE viene composta solo con i filtri effettivamente
     * valorizzati: se non ne arriva nessuno la query restituisce l'intero
     * storico, come chiede il caso di test senza filtri.
     *
     * Il nome della sala si raggiunge navigando dalla postazione all'area e
     * dall'area alla sala, perche' la prenotazione non tiene un riferimento
     * diretto alla sala.
     */
    public List<Prenotazione> trovaPrenotazioniStoriche(String nomeSala, String matricolaStudente) {
        EntityManager em = DBManager.getInstance().getEntityManager();
        List<Prenotazione> risultati = new ArrayList<>();

        boolean filtraPerSala = nomeSala != null && !nomeSala.trim().isEmpty();
        boolean filtraPerStudente = matricolaStudente != null && !matricolaStudente.trim().isEmpty();

        try {
            StringBuilder jpql = new StringBuilder("SELECT p FROM Prenotazione p");
            String congiunzione = " WHERE ";

            if (filtraPerSala) {
                jpql.append(congiunzione).append("p.postazione.area.sala.nome = :nomeSala");
                congiunzione = " AND ";
            }
            if (filtraPerStudente) {
                jpql.append(congiunzione).append("p.studente.matricola = :matricola");
            }

            jpql.append(" ORDER BY p.data DESC, p.fasciaOraria");

            TypedQuery<Prenotazione> query = em.createQuery(jpql.toString(), Prenotazione.class);
            if (filtraPerSala) {
                query.setParameter("nomeSala", nomeSala.trim());
            }
            if (filtraPerStudente) {
                query.setParameter("matricola", matricolaStudente.trim());
            }

            risultati = query.getResultList();

        } catch (Exception e) {
            System.err.println("Errore durante la ricerca dello storico: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return risultati;
    }

    /**
     * Elenco di tutte le sale registrate, in ordine di nome.
     *
     * E' l'operazione trovaTutteLeSale del diagramma. Serve alla finestra dello
     * storico per proporre le sale in un menu a tendina, invece di far digitare
     * il nome.
     */
    public List<SalaStudio> trovaTutteLeSale() {
        EntityManager em = DBManager.getInstance().getEntityManager();
        List<SalaStudio> sale = new ArrayList<>();

        try {
            String jpql = "SELECT s FROM SalaStudio s ORDER BY s.nome";
            sale = em.createQuery(jpql, SalaStudio.class).getResultList();
        } catch (Exception e) {
            System.err.println("Errore durante la lettura delle sale: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return sale;
    }

    public void aggiorna(Object entity) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            em.merge(entity);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();

            throw new RuntimeException("Errore durante l'aggiornamento nel database.");
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    public void rimuovi(Object entity) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        try {
            em.getTransaction().begin();
            Object managedEntity = em.merge(entity);
            em.remove(managedEntity);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();

            throw new RuntimeException("Errore durante l'eliminazione nel database.");
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    //
    // --- METODI PER I CASI D'USO DI PRENOTAZIONE ---
    //

    //Interroga il database per recuperare l'elenco completo di tutte le sale studio fisicamente presenti.
    public List<SalaStudio> trovaTutteLeSaleDisponibili(LocalDate data, String fasciaOraria) {
        // Si richiede l'EntityManager al Singleton per interagire con Hibernate
        EntityManager em = DBManager.getInstance().getEntityManager();
        List<SalaStudio> listaSale = null;

        try {
            // Formulazione della query in JPQL (Java Persistence Query Language)
            String jpql = "SELECT s FROM SalaStudio s";

            // Creazione di una TypedQuery. Passare la classe SalaStudio.class come secondo parametro
            // garantisce la type-safety a tempo di compilazione, evitando cast espliciti successivi.
            TypedQuery<SalaStudio> query = em.createQuery(jpql, SalaStudio.class);
            listaSale = query.getResultList();

        } catch (Exception e) {
            System.err.println("Errore in trovaTutteLeSaleDisponibili: " + e.getMessage());
        } finally {
            // Garantisce che la connessione al database venga sempre rilasciata prevenendo il Memory Leak
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
        return listaSale;
    }

    // Calcola dinamicamente le fasce orarie in cui la sala specificata dispone ancora di postazioni libere,
    // sottraendo le fasce "sold-out" dall'elenco totale delle fasce ammesse
    public List<String> trovaFasceOrarieDisponibili(String idSala, LocalDate data) {
        EntityManager em = DBManager.getInstance().getEntityManager();

        // Inizializzo la lista con TUTTE le fasce orarie possibili previste dal sistema,
        // per poi procedere per sottrazione eliminando quelle esaurite.
        List<String> fasceDisponibili = new ArrayList<>(FasceOrarie.getElenco());

        try {
            // Navigazione delle relazioni per evitare ridondanze nell'Entity Prenotazione
            // Poiché l'Entity Prenotazione non ha un riferimento diretto alla Sala, bisogna navigare
            // la catena di relazioni: Prenotazione -> Postazione -> Area -> SalaStudio
            String jpql = "SELECT p.fasciaOraria " +
                    "FROM Prenotazione p " +
                    "WHERE p.postazione.area.salaStudio.id = :idSala AND p.data = :data " +
                    "GROUP BY p.fasciaOraria " +
                    "HAVING COUNT(p.idPrenotazione) >= (SELECT s.numeroPostazioni FROM SalaStudio s WHERE s.id = :idSala)";

            TypedQuery<String> query = em.createQuery(jpql, String.class);
            // Binding dei parametri
            query.setParameter("idSala", idSala);
            query.setParameter("data", data);

            List<String> fasceEsaurite = query.getResultList();
            // Rimozione logica delle fasce piene dall'elenco delle fasce totali iniziali
            fasceDisponibili.removeAll(fasceEsaurite);

        } catch (Exception e) {
            System.err.println("Errore in trovaFasceOrarieDisponibili: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
        return fasceDisponibili;
    }

    // Ricerca di uno Studente all'interno del database tramite la sua matricola (chiave primaria)
    public Studente trovaPerMatricola(String matricola) {
        EntityManager em = DBManager.getInstance().getEntityManager();
        Studente studente = null;
        try {
            // Utilizzo il metodo em.find() per la ricerca dell'entità tramite la Primary Key
            studente = em.find(Studente.class, matricola);
        } catch (Exception e) {
            System.err.println("Errore in trovaPerMatricola: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
        return studente;
    }

    // Ricerca di una singola Postazione tramite il suo: "idPostazione" (chiave primaria)
    public Postazione trovaPostazionePerId(String idPostazione) {
        EntityManager em = DBManager.getInstance().getEntityManager();
        Postazione postazione = null;
        try {
            postazione = em.find(Postazione.class, idPostazione);
        } catch (Exception e) {
            System.err.println("Errore in trovaPostazionePerId: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
        return postazione;
    }

    // Trova tutte le postazioni attualmente libere e agibili in una specifica sala,
    // per una determinata data e fascia oraria
    public List<Postazione> trovaPostazioniLibere(String idSala, LocalDate data, String fasciaOraria) {
        EntityManager em = DBManager.getInstance().getEntityManager();
        List<Postazione> postazioniLibere = new ArrayList<>();

        try {
            // JPQL: Seleziono la postazione "p" a condizione che:
            // 1. Appartenga alla sala richiesta
            // 2. Sia agibile
            // 3. Non esiste una prenotazione attiva o confermata per la stessa postazione, data e ora.
            String jpql = "SELECT p FROM Postazione p " +
                    "WHERE p.area.salaStudio.id = :idSala " +
                    "AND p.isDisponibile = true " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM Prenotazione pre " +
                    "    WHERE pre.postazione = p " +
                    "    AND pre.data = :data " +
                    "    AND pre.fasciaOraria = :fasciaOraria " +
                    "    AND pre.stato IN (:statoAttiva, :statoConfermata)" +
                    ")";

            TypedQuery<Postazione> query = em.createQuery(jpql, Postazione.class);

            // Inserimento sicuro dei parametri
            query.setParameter("idSala", idSala);
            query.setParameter("data", data);
            query.setParameter("fasciaOraria", fasciaOraria);
            query.setParameter("statoAttiva", Prenotazione.ATTIVA);
            query.setParameter("statoConfermata", Prenotazione.CONFERMATA);

            postazioniLibere = query.getResultList();

        } catch (Exception e) {
            System.err.println("Errore in trovaPostazioniLibere: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return postazioniLibere;
    }
}