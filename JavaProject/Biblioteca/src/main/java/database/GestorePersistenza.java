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


    /**
     * Metodo utilizzato in Registrazione e in Login.
     *
     * La ricerca va fatta su Studente e su Bibliotecario separatamente perche'
     * Utente e' annotata @MappedSuperclass: non e' un'entity, non ha una tabella
     * e JPQL non la accetta come radice di una query. Scriverla come
     * "SELECT u FROM Utente u" fa fallire l'accesso con
     * UnknownEntityException: Could not resolve root entity 'Utente'.
     *
     * L'email e' dichiarata unica, quindi al massimo una delle due ricerche
     * restituisce un risultato.
     */
    public Utente trovaUtentePerEmail(String email) {
        Utente studente = cercaPerEmail(email, Studente.class);
        if (studente != null) {
            return studente;
        }
        return cercaPerEmail(email, Bibliotecario.class);
    }


    /** Cerca per email dentro una sola delle due tabelle degli utenti. */
    private <T extends Utente> T cercaPerEmail(String email, Class<T> tipo) {
        return DBManager.getInstance().esegui(em -> {
            try {
                String jpql = "SELECT u FROM " + tipo.getSimpleName() + " u WHERE u.email = :email";
                return em.createQuery(jpql, tipo).setParameter("email", email).getSingleResult();
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
     *
     * Usato anche in checkin
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
     * E' l'operazione trovaTutteLeSale del diagramma.
     * Serve alla finestra dello storico per proporre le sale in un menu a tendina, invece di far digitare il nome.
     */
    public List<SalaStudio> trovaTutteLeSale() {

        // Passo al DBManager solo "cosa" fare (la query),
        // lasciando a lui la responsabilità del "come" gestire la connessione.
        return DBManager.getInstance().esegui(em -> {

            // Formulazione della query JPQL
            String jpql = "SELECT s FROM SalaStudio s ORDER BY s.nome";

            // Creazione della TypedQuery (type-safe) e restituzione immediata del risultato
            return em.createQuery(jpql, SalaStudio.class).getResultList();

        });
    }


    /**
     * Aggiorna un'entità già esistente nel database (Equivalente all'UPDATE in SQL).
     *
     * Utilizzato in checkin.
     */
    public void aggiorna(Object entity) {
        DBManager.getInstance().eseguiInTransazione(em -> em.merge(entity));
    }


    /**
     * Rimuove un'entità dal database (Equivalente al DELETE in SQL).
     */
    public void rimuovi(Object entity) {
        DBManager.getInstance().eseguiInTransazione(em -> {
            // L'entità passata dal Controller potrebbe essere "detached" (scollegata).
            // Usando em.merge(), la "riagganciamo" al contesto di persistenza attuale (managedEntity),
            // rendendo così possibile la sua rimozione sicura senza generare eccezioni IllegalArgumentException.
            Object managedEntity = em.merge(entity);
            em.remove(managedEntity);
        });
    }


    //
    // --- METODI PER I CASI D'USO DI PRENOTAZIONE ---
    //

    /**
     * Interroga il database per recuperare l'elenco completo delle sale studio.
     */
    public List<SalaStudio> trovaTutteLeSaleDisponibili(LocalDate data, String fasciaOraria) {

        return DBManager.getInstance().esegui(em -> {
            // Formulazione della query in JPQL (Java Persistence Query Language)
            // Attualmente la query estrae l'elenco completo senza filtri applicati.
            String jpql = "SELECT s FROM SalaStudio s";

            // Creazione di una TypedQuery. Passare la classe SalaStudio.class come secondo
            // parametro garantisce la type-safety a tempo di compilazione, evitando cast espliciti.
            return em.createQuery(jpql, SalaStudio.class).getResultList();
        });

    }

    /**
     * Calcola dinamicamente le fasce orarie in cui la sala specificata dispone ancora di postazioni libere,
     * sottraendo le fasce "sold-out" dall'elenco totale delle fasce ammesse.
     */
    public List<String> trovaFasceOrarieDisponibili(String idSala, LocalDate data) {

        return DBManager.getInstance().esegui(em -> {

            // Inizializzo la lista con TUTTE le fasce orarie possibili previste dal sistema,
            // per poi procedere per sottrazione logica eliminando quelle esaurite.
            List<String> fasceDisponibili = new ArrayList<>(FasceOrarie.getElenco());

            // Navigazione delle relazioni per evitare ridondanze nell'Entity Prenotazione.
            // Poiché l'Entity Prenotazione non ha un riferimento diretto alla Sala, bisogna navigare
            // la catena di relazioni: Prenotazione -> Postazione -> Area -> SalaStudio.
            String jpql = "SELECT p.fasciaOraria " +
                    "FROM Prenotazione p " +
                    "WHERE p.postazione.area.sala.idSala = :idSala AND p.data = :data " +
                    "GROUP BY p.fasciaOraria " +
                    "HAVING COUNT(p.idPrenotazione) >= (SELECT s.numPostazioniTotali FROM SalaStudio s WHERE s.idSala = :idSala)";

            // Esecuzione compatta con binding dei parametri
            List<String> fasceEsaurite = em.createQuery(jpql, String.class)
                    .setParameter("idSala", idSala)
                    .setParameter("data", data)
                    .getResultList();

            // Rimozione logica delle fasce piene dall'elenco delle fasce totali iniziali
            fasceDisponibili.removeAll(fasceEsaurite);

            return fasceDisponibili;
        });
    }

    /**
     * Ricerca di uno Studente all'interno del database tramite la sua matricola (chiave primaria).
     */
    public Studente trovaPerMatricola(String matricola) {
        // La gestione della connessione è centralizzata. Viene restituito direttamente
        // l'oggetto Studente trovato, oppure null se la matricola non è presente nel DB.
        return DBManager.getInstance().esegui(em -> em.find(Studente.class, matricola));
    }


    /**
     * Ricerca di una singola Postazione all'interno del database tramite il suo identificativo (chiave primaria).
     */
    public Postazione trovaPostazionePerId(String idPostazione) {

        // Ricerca type-safe per chiave primaria, delegando l'apertura e chiusura dell'EntityManager al Singleton
        return DBManager.getInstance().esegui(em -> em.find(Postazione.class, idPostazione));
    }


    /**
     * Trova tutte le postazioni attualmente libere e agibili in una specifica sala,
     * per una determinata data e fascia oraria.
     */
    public List<Postazione> trovaPostazioniLibere(String idSala, LocalDate data, String fasciaOraria) {

        return DBManager.getInstance().esegui(em -> {

            // JPQL: Seleziono la postazione "p" a condizione che:
            // 1. Appartenga alla sala richiesta
            // 2. Sia agibile (isDisponibile = true)
            // 3. Non esista una prenotazione attiva o confermata per la stessa postazione, data e ora.
            String jpql = "SELECT p FROM Postazione p " +
                    "WHERE p.area.sala.idSala = :idSala " +
                    "AND p.isDisponibile = true " +
                    "AND NOT EXISTS (" +
                    "    SELECT 1 FROM Prenotazione pre " +
                    "    WHERE pre.postazione = p " +
                    "    AND pre.data = :data " +
                    "    AND pre.fasciaOraria = :fasciaOraria " +
                    "    AND pre.stato IN (:statoAttiva, :statoConfermata)" +
                    ")";

            // Esecuzione fluida con binding di tutti i parametri in catena
            return em.createQuery(jpql, Postazione.class)
                    .setParameter("idSala", idSala)
                    .setParameter("data", data)
                    .setParameter("fasciaOraria", fasciaOraria)
                    .setParameter("statoAttiva", Prenotazione.ATTIVA)
                    .setParameter("statoConfermata", Prenotazione.CONFERMATA)
                    .getResultList();
        });
    }

    //Metodo utilizzato in checkin
    public List<Prenotazione> trovaPrenotazioniAttivePerStudente (String matricola) {
        return DBManager.getInstance().esegui(em -> {
            String jpql = "SELECT p FROM Prenotazione p WHERE p.studente.matricola = :matricola AND p.stato = :stato";
            return  em.createQuery(jpql, Prenotazione.class).setParameter("matricola", matricola).setParameter("stato", Prenotazione.ATTIVA).getResultList();
        });
    }
}