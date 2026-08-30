package database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import entity.FasceOrarie;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Studente;
import entity.Postazione;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GestorePersistenza {

    public void salva(Object entity) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();
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
            em.close();
        }
    }

    public void aggiorna(Object entity) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();
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
            em.close();
        }
    }

    public void rimuovi(Object entity) {
        EntityManager em = JpaUtil.getInstance().getEntityManager();
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
            em.close();
        }
    }


    // Metodo utilizzato in ConsultazioneDisponibilitaSaleStudio
    public List<SalaStudio> trovaTutteLeSaleDisponibili(Date data, String fasciaOraria) {

        // Si ottiene l'EntityManager dal Singleton DBManager per interagire con il contesto di persistenza
        EntityManager em = DBManager.getInstance().getEntityManager();
        List<SalaStudio> listaSale = null;

        try {
            // Utilizzo di JPQL (Java Persistence Query Language) per interrogare il modello a oggetti
            String jpql = "SELECT s FROM SalaStudio s";

            // Creazione di una query tipizzata per garantire la type-safety ed evitare cast espliciti
            TypedQuery<SalaStudio> query = em.createQuery(jpql, SalaStudio.class);

            listaSale = query.getResultList();

        } catch (Exception e) {
            System.err.println("Errore durante l'esecuzione della query JPQL in trovaTutteLeSaleDisponibili: " + e.getMessage());
        } finally {
            // Chiusura sicura dell'EntityManager per garantire il rilascio delle risorse ed evitare memory leak
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return listaSale;
    }

    //Metodo utilizzato in VisualizzazioneFasceOrarieDisponibili
    public List<String> trovaFasceOrarieDisponibili(String idSala, Date data) {

        EntityManager em = DBManager.getInstance().getEntityManager();
        List<String> fasceDisponibili = new ArrayList<>(entity.FasceOrarie.getElenco());

        try {
            // Query JPQL per trovare le fasce "sold-out" (dove prenotazioni >= posti totali)
            String jpql = "SELECT p.fasciaOraria " + "FROM Prenotazione p " +
                    "WHERE p.sala.id = :idSala AND p.data = :data " + "GROUP BY p.fasciaOraria " +
                    "HAVING COUNT(p.id) >= (SELECT s.numeroPostazioni FROM SalaStudio s WHERE s.id = :idSala)";

            TypedQuery<String> query = em.createQuery(jpql, String.class);
            query.setParameter("idSala", idSala);
            query.setParameter("data", data);

            // Ottengo ora la lista delle fasce orarie in cui non ci sono più posti
            List<String> fasceEsaurite = query.getResultList();
            // Rimuovo dalla lista completa quelle esaurite
            fasceDisponibili.removeAll(fasceEsaurite);

        } catch (Exception e) {
            System.err.println("Errore durante la verifica delle fasce orarie: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return fasceDisponibili;
    }

    //Metodi utilizzati in EffettuaPrenotazione
    public Studente trovaPerMatricola(String matricola) {

        EntityManager em = DBManager.getInstance().getEntityManager();
        Studente studente = null;

        try {
            // con il metodo find cerco direttamente per chiave primaria (ID)
            studente = em.find(Studente.class, matricola);
        } catch (Exception e) {
            System.err.println("Errore nel recupero dello studente: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return studente;
    }

    public Postazione trovaPostazionePerId(String idPostazione) {

        EntityManager em = DBManager.getInstance().getEntityManager();
        Postazione postazione = null;

        try {
            // con il metodo find cerco direttamente per chiave primaria (ID)
            postazione = em.find(Postazione.class, idPostazione);
        } catch (Exception e) {
            System.err.println("Errore nel recupero della postazione: " + e.getMessage());
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }

        return postazione;
    }
}