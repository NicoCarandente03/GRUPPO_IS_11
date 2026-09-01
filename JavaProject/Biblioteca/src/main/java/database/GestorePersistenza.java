package database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import entity.FasceOrarie;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Studente;
import entity.Postazione;
import entity.Area;
import entity.Bibliotecario;
import entity.Utente;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;

public class GestorePersistenza {

    //
    // --- METODI CRUD BASE ---
    //

    public void salva(Object entity) {
        DBManager.getInstance().eseguiInTransazione(em -> em.persist(entity)); //sintassi a freccia per passare un'azione come parametro
    }


    /**
     * Salva piu' oggetti nella stessa transazione.
     */
    public void salvaTutti(Object... entita) {
        DBManager.getInstance().eseguiInTransazione(em -> {
            for(Object e: entita) {
                em.persist(e);
            }
        });
    }


    //Metodi utilizzati in Registrazione
    public Utente trovaUtentePerEmail(String email) {
        return DBManager.getInstance().esegui(em -> {
            try {
                return em.createQuery("SELECT u FROM Utente WHERE u.email = :email", Utente.class).setParameter("email", email).getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        });
    }

    //Metodi utilizzati in AnnullamentoPrenotazione

    /**
     * Cerca una prenotazione dalla sua chiave primaria.
     *
     * Restituisce null se non esiste: e' il caso di test in cui lo studente
     * indica un identificativo inesistente.
     */
    public Prenotazione trovaPrenotazionePerId(String idPrenotazione) {
        return DBManager.getInstance().esegui(em -> em.find(Prenotazione.class, idPrenotazione));
    }


    /**
     * Elenco delle prenotazioni di uno studente, dalla piu' recente.
     */
    public List<Prenotazione> trovaPrenotazioniPerStudente(String matricola) {
        return DBManager.getInstance().esegui(em -> {
            String jpql="SELECT p FROM Prenotazione p WHERE p.studente.matricola = :matricola ORDER BY p.data DESC, p.fasciaOraria";
            return em.createQuery(jpql, Prenotazione.class).setParameter("matricola", matricola).getResultList();
        });
    }


    //Metodi utilizzati in CreazioneAulaStudio

    /**
     * Cerca una sala dal nome, che il modello di dominio vuole unico.
     *
     * Restituisce null se non esiste: e' il controllo che impedisce di creare
     * due sale con lo stesso nome.
     */
    public SalaStudio trovaSalaPerNome(String nome) {
        return DBManager.getInstance().esegui(em -> {
            try{
                return em.createQuery("SELECT s FROM SalaStudio s WHERE s.nome = :nome", SalaStudio.class).setParameter("nome", nome).getSingleResult();
            } catch (NoResultException e) {
            return null;
            }
        });
    }


    /**
     * Cerca un bibliotecario dalla sua chiave primaria.
     */
    public Bibliotecario trovaPerCodiceIdentificativo(String codiceIdentificativo) {
        return DBManager.getInstance().esegui(em -> em.find(Bibliotecario.class, codiceIdentificativo));
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
        return DBManager.getInstance().esegui(em -> {
            boolean filtraPerSala = nomeSala != null && !nomeSala.trim().isEmpty();
            boolean filtraPerStudente = matricolaStudente != null && !matricolaStudente.trim().isEmpty();
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
            // Binding dinamico dei parametri solo se la stringa è stata effettivamente accodata
            if (filtraPerSala) {
                query.setParameter("nomeSala", nomeSala.trim());
            }
            if (filtraPerStudente) {
                query.setParameter("matricola", matricolaStudente.trim());
            }
            return query.getResultList();
        });
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
    //può essere così?
    /*public List<SalaStudio> trovaTutteLeSale() {
        return DBManager.getInstance().esegui(em -> {
            String jpql = "SELECT s FROM SalaStudio s ORDER BY s.nome";
            return em.createQuery(jpql, SalaStudio.class).getResultList();
        });
    }*/


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
    //può essere fatto così?
    /*public void aggiorna(Object entity) {
        DBManager.getInstance().eseguiInTransazione(em -> em.merge(entity));
    }*/


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
    //può essere fatto così?
    /*public void rimuovi(Object entity) {
        DBManager.getInstance().eseguiInTransazione(em -> {
            Object managedEntity = em.merge(entity);
            em.remove(managedEntity);
        });
    }*/


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
    //può essere fatto così?
    /*public List<SalaStudio> trovaTutteLeSaleDisponibili() {
        return DBManager.getInstance().esegui(em -> {
            return em.createQuery("SELECT s FROM SalaStudio s", SalaStudio.class).getResultList();
            });
    }*/


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
    //può essere fatto così?
    /*public List<String> trovaFasceOrarieDisponibili(String idSala, LocalDate data) {
        return DBManager.getInstance().esegui(em -> {
            List<String> fasceDisponibili = new ArrayList<>(FasceOrarie.getElenco());
            String jpql = "SELECT p.fasciaOraria FROM Prenotazione p WHERE p.postazione.area.salaStudio.idSala = :idSala AND p.data = :data GROUP BY p.fasciaOraria HAVING COUNT(p.idPrenotazione) >= (SELECT s.numPostazioniTotali FROM SalaStudio s WHERE s.idSala = :idSala)";

            List<String> fasceEsaurite = em.createQuery(jpql, String.class).setParameter("idSala", idSala).setParameter("data", data).getResultList();

            fasceDisponibili.removeAll(fasceEsaurite);
            return fasceDisponibili;
        });
    }*/

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
    //può essere fatto così?
    /*public Studente trovaPerMatricola(String matricola) {
        return DBManager.getInstance().esegui(em -> em.find(Studente.class, matricola));
    }*/


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
    //può essere fatto così?
    /*public Postazione trovaPostazionePerId(String idPostazione) {
        return DBManager.getInstance().esegui(em -> em.find(Postazione.class, idPostazione));
    }*/


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
    //può essere fatto così?
    /*public List<Postazione> trovaPostazioniLibere(String idSala, LocalDate data, String fasciaOraria) {
        return DBManager.getInstance().esegui(em -> {
            String jpql = "SELECT p FROM Postazione p WHERE p.area.salaStudio.idSala = :idSala AND p.isDisponibile = true AND NOT EXISTS
            (SELECT 1 FROM Prenotazione pre WHERE pre.postazione = p AND pre.data = :data AND pre.fasciaOraria = :fasciaOraria AND pre.stato IN (:statoAttiva, :statoConfermata))";

            return em.createQuery(jpql, Postazione.class).setParameter("idSala", idSala).setParameter("data", data).setParameter("fasciaOraria", fasciaOraria).setParameter("statoAttiva", Prenotazione.ATTIVA).setParameter("statoConfermata", Prenotazione.CONFERMATA).getResultList();
            });
        }
    }*/
}