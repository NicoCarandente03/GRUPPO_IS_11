package controller;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dto.PrenotazioneDTO;
import eccezioni.BusinessException;

import database.GestorePersistenza;

import entity.Studente;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Postazione;

import external.ServizioDiNotifiche;

public class GestionePrenotazioneController {
    private int intervalloCheckInMinuti;
    private int limiteAnnullamentoMinuti;

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static GestionePrenotazioneController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private GestionePrenotazioneController() {
        this.gestoreDB = new GestorePersistenza();
        caricaConfigurazione();
    }

    /**
     * Legge i limiti temporali da config.properties
     */
    private void caricaConfigurazione() {
        this.limiteAnnullamentoMinuti = 60;
        this.intervalloCheckInMinuti = 30;

        Properties configurazione = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in == null) {
                return;
            }
            configurazione.load(in);
            this.limiteAnnullamentoMinuti = Integer.parseInt(
                    configurazione.getProperty("annullamento.limite.minuti", "60").trim());
            this.intervalloCheckInMinuti = Integer.parseInt(
                    configurazione.getProperty("checkin.intervallo.minuti", "30").trim());
        } catch (Exception e) {
            System.err.println("config.properties non leggibile, uso i valori predefiniti: "
                    + e.getMessage());
        }
    }

    public int getLimiteAnnullamentoMinuti() {
        return limiteAnnullamentoMinuti;
    }

    public int getIntervalloCheckInMinuti() {
        return intervalloCheckInMinuti;
    }


    /**
     * Costruttore usato dai test, che passano un finto GestorePersistenza al
     * posto di quello reale. Non e' pubblico proprio per non essere usato
     * altrove: l'applicazione passa sempre da getInstance().
     */
    GestionePrenotazioneController(GestorePersistenza gestoreDB) {
        this.gestoreDB = gestoreDB;
        caricaConfigurazione();
    }

    public static GestionePrenotazioneController getInstance() {
        if (instance == null) {
            instance = new GestionePrenotazioneController();
        }
        return instance;
    }


    public List<SalaStudio> consultazioneDisponibilitaSaleStudio(LocalDate data, String fasciaOraria) {
        // Il controller demanda la logica al package database e restituisce il risultato
        return gestoreDB.trovaTutteLeSaleDisponibili(data, fasciaOraria);
    }

    public List<String> visualizzazioneFasceOrarieDisponibili(String idSala, LocalDate data) {
        // Delega totale della logica di estrazione al database
        return gestoreDB.trovaFasceOrarieDisponibili(idSala, data);
    }

    public boolean effettuaPrenotazione(String matricola, LocalDate data, String idSala, String fasciaOraria, String idArea, String idPostazione) {
        boolean esitoCreazione = true;
        Prenotazione nuovaPrenotazione = new Prenotazione();

        try {
            Studente studente = gestoreDB.trovaPerMatricola(matricola);

            // Creazione UUID univoco per il database
            String idUnivoco = java.util.UUID.randomUUID().toString();
            nuovaPrenotazione.setIdPrenotazione(idUnivoco);

            nuovaPrenotazione.setStudente(studente);
            nuovaPrenotazione.setData(data);
            nuovaPrenotazione.setFasciaOraria(fasciaOraria);

            // Set dello stato a partire dalla costante di classe
            nuovaPrenotazione.setStato(Prenotazione.ATTIVA);

            // Assegnazione della postazione specifica (se prevista)
            if (idPostazione != null && !idPostazione.isEmpty()) {
                Postazione postazione = gestoreDB.trovaPostazionePerId(idPostazione);
                nuovaPrenotazione.setPostazione(postazione);
            } else {
                // Se l'utente ha scelto solo la Sala, assegno in automatico la prima postazione libera trovata
                List<Postazione> postazioniLibere = gestoreDB.trovaPostazioniLibere(idSala, data, fasciaOraria);
                if (postazioniLibere != null && !postazioniLibere.isEmpty()) {
                    nuovaPrenotazione.setPostazione(postazioniLibere.get(0));
                } else {
                    throw new Exception("Nessuna postazione libera in questa sala per l'orario scelto.");
                }
            }

            // Delegazione del salvataggio al DB
            gestoreDB.salva(nuovaPrenotazione);

        } catch (Exception e) {
            esitoCreazione = false;
            System.err.println("Errore nel salvataggio della prenotazione: " + e.getMessage());
        }

        // Se tutto va a buon fine, inviamo la notifica tramite l'attore esterno
        if (esitoCreazione) {
            ServizioDiNotifiche.inviaNotificaConferma(matricola, nuovaPrenotazione);
        }

        return esitoCreazione;
    }

    /**
     * Elenco delle prenotazioni di uno studente, nella forma che serve alla
     * finestra del profilo.
     */
    public List<PrenotazioneDTO> visualizzaPrenotazioniEffettuate(String matricola) {
        List<PrenotazioneDTO> elenco = new ArrayList<>();

        for (Prenotazione prenotazione : gestoreDB.trovaPrenotazioniPerStudente(matricola)) {
            elenco.add(convertiInDTO(prenotazione));
        }

        return elenco;
    }

    /**
     * Annulla una prenotazione e rende di nuovo disponibile la postazione.
     *
     * I controlli sono nell'ordine dei casi di test del piano funzionale, e ogni
     * fallimento solleva una BusinessException con il messaggio previsto: un
     * valore di ritorno booleano non basterebbe a distinguere cinque errori
     * diversi. La firma resta void come nel diagramma delle classi.
     *
     * Il parametro con la matricola del richiedente non compare nel diagramma,
     * ma serve al controllo di proprieta' richiesto dal caso di test 3: uno
     * studente puo' annullare solo le proprie prenotazioni.
     *
     * E' un parametro temporaneo. Quando ci sara' il Log-in, la matricola verra'
     * letta dalla sessione tenuta da AutenticazioneController e il metodo tornera'
     * ad avere la firma del diagramma, con il solo idPrenotazione.
     */
    public void annullamentoPrenotazione(String idPrenotazione, String matricolaRichiedente) {
        annullamentoPrenotazione(idPrenotazione, matricolaRichiedente, LocalDateTime.now());
    }

    /**
     * Variante con l'istante di riferimento esplicito, usata dai test per non
     * dipendere dall'orologio di sistema.
     */
    public void annullamentoPrenotazione(String idPrenotazione, String matricolaRichiedente,
                                         LocalDateTime adesso) {

        Prenotazione prenotazione = gestoreDB.trovaPrenotazionePerId(idPrenotazione);

        if (prenotazione == null) {
            throw new BusinessException("Errore, la prenotazione selezionata è inesistente!");
        }

        if (prenotazione.getStudente() == null
                || !prenotazione.getStudente().getMatricola().equals(matricolaRichiedente)) {
            throw new BusinessException("Errore, non sei autorizzato ad annullare questa prenotazione!");
        }

        if (Prenotazione.ANNULLATA.equals(prenotazione.getStato())) {
            throw new BusinessException("Errore, la prenotazione risulta già annullata!");
        }

        if (Prenotazione.SCADUTA.equals(prenotazione.getStato())) {
            throw new BusinessException("Errore, la prenotazione risulta scaduta!");
        }

        boolean annullata = prenotazione.annulla(limiteAnnullamentoMinuti, adesso);

        if (!annullata) {
            throw new BusinessException("Errore, il tempo limite per l'annullamento è stato superato!");
        }

        // la postazione e' gia' stata liberata dall'entity, qui si rende persistente
        gestoreDB.aggiorna(prenotazione);
        gestoreDB.aggiorna(prenotazione.getPostazione());

        NotificaController.getInstance().notificaAnnullamento(prenotazione);
    }

    /**
     * Recupera l'elenco delle prenotazioni attive per studente da mostrare
     * in tabella, per le quali è possibile effettuare il check-in.
     */
    public List<PrenotazioneDTO> richiediPrenotazioniAttive(String matricola) {
        List<PrenotazioneDTO> attive = new ArrayList<>();

        for (Prenotazione prenotazione : gestoreDB.trovaPrenotazioniAttivePerStudente(matricola)) {
            attive.add(convertiInDTO(prenotazione));
        }

        return attive;
    }

    /**
     * Registra la conferma della presenza dello studente in biblioteca
     * sfruttando i parametri caricati dal file di configurazione.
     *
     * Lo studente non digita a mano il codice di una prenotazione:
     * accede al suo profilo per visualizzare la lista delle prenotazioni
     * attive. Una volta selezionata, il sistema esegue un controllo per
     * decidere se abilitare il bottone del check-in.
     */
    public void checkin(String idPrenotazione) {
        Prenotazione prenotazione = gestoreDB.trovaPrenotazionePerId(idPrenotazione);
        if (prenotazione == null) {
            throw new BusinessException("Nessuna prenotazione esistente");
        }

        //Delega della validazione
        verificaValiditaCheckin(prenotazione);

        boolean esito = prenotazione.checkin(this.intervalloCheckInMinuti);
        if (!esito) {
            throw new BusinessException("Impossibile confermare la presenza");
        }

        gestoreDB.aggiorna(prenotazione);
    }

    //Controllo sulla validità del check-in
    private void verificaValiditaCheckin(Prenotazione prenotazione) {
        LocalDateTime adesso = LocalDateTime.now();

        //intervallo valido
        if (prenotazione.isCheckinValido(this.intervalloCheckInMinuti, adesso)) {
            return;
        }

        //intervallo scaduto
        if (prenotazione.isCheckinScaduto(this.intervalloCheckInMinuti, adesso)) {
            prenotazione.scadi();
            gestoreDB.aggiorna(prenotazione);
            throw new BusinessException("Check-in fallito per intervallo superato. Prenotazione annullata");
        }

        //Se l'intervallo non è valido e non è scaduto, significa che non
        //è ancora iniziato
        throw new BusinessException("Check-in non consentito per intervallo non iniziato");
    }

    /**
     * Traduce una prenotazione nel DTO che il Boundary sa mostrare, risalendo
     * ad area e sala dalla postazione prenotata.
     */
    private PrenotazioneDTO convertiInDTO(Prenotazione prenotazione) {
        String nomeSala = prenotazione.getSala() == null
                ? "" : prenotazione.getSala().getNome();
        String tipoArea = prenotazione.getArea() == null
                ? "" : prenotazione.getArea().getTipo();
        String idPostazione = prenotazione.getPostazione() == null
                ? "" : prenotazione.getPostazione().getIdPostazione();

        return new PrenotazioneDTO(
                prenotazione.getIdPrenotazione(),
                prenotazione.getStudente().getMatricola(),
                nomeSala,
                tipoArea,
                idPostazione,
                prenotazione.getData(),
                prenotazione.getFasciaOraria(),
                prenotazione.getStato());
    }
}
