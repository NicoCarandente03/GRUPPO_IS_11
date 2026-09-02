package controller;

import database.GestorePersistenza;
import dto.PrenotazioneDTO;
import eccezioni.BusinessException;
import entity.Bibliotecario;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Utente;

import java.util.ArrayList;
import java.util.List;

public class MonitoraggioSaleController {

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static MonitoraggioSaleController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // 3. Sessione, per sapere chi sta lavorando senza farselo dire da fuori
    private AutenticazioneController autenticazione;

    // Costruttore privato
    private MonitoraggioSaleController() {
        this.gestoreDB = new GestorePersistenza();
        this.autenticazione = AutenticazioneController.getInstance();
    }


    /**
     * Costruttore usato dai test, che passano un finto GestorePersistenza e una
     * finta sessione al posto di quelli reali. Non e' pubblico proprio per non
     * essere usato altrove: l'applicazione passa sempre da getInstance().
     */
    MonitoraggioSaleController(GestorePersistenza gestoreDB,
                               AutenticazioneController autenticazione) {
        this.gestoreDB = gestoreDB;
        this.autenticazione = autenticazione;
    }

    public static MonitoraggioSaleController getInstance() {
        if (instance == null) {
            instance = new MonitoraggioSaleController();
        }
        return instance;
    }

    /**
     * Nomi delle sale registrate, per il menu a tendina della finestra.
     */
    public List<String> getElencoNomiSale() {
        List<String> nomi = new ArrayList<>();

        for (SalaStudio sala : gestoreDB.trovaTutteLeSale()) {
            nomi.add(sala.getNome());
        }

        return nomi;
    }

    /**
     * Storico delle prenotazioni, filtrabile per sala e per studente.
     *
     * Entrambi i filtri sono facoltativi: senza nessuno dei due si ottiene lo
     * storico completo. I controlli seguono l'ordine dei casi di test e ogni
     * fallimento solleva una BusinessException con il messaggio previsto.
     *
     * Chi consulta lo storico deve essere un bibliotecario registrato: non lo si
     * chiede a chi invoca il metodo, si legge dalla sessione.
     *
     * Nel diagramma di sequenza il filtro sulla sala e' l'identificativo, mentre
     * il piano di test usa il nome: qui si segue il piano di test, perche' e' il
     * nome quello che il bibliotecario vede nel menu a tendina.
     */
    public List<PrenotazioneDTO> consultazioneStoricoPrenotazioni(String nomeSala,
                                                                  String matricolaStudente) {

        String codiceBibliotecario = codiceBibliotecarioInSessione();

        if (gestoreDB.trovaPerCodiceIdentificativo(codiceBibliotecario) == null) {
            throw new BusinessException("Errore, accesso negato!");
        }

        boolean filtraPerSala = nomeSala != null && !nomeSala.trim().isEmpty();
        boolean filtraPerStudente = matricolaStudente != null && !matricolaStudente.trim().isEmpty();

        if (filtraPerSala && gestoreDB.trovaSalaPerNome(nomeSala.trim()) == null) {
            throw new BusinessException("Errore, la sala studio selezionata non esiste!");
        }

        if (filtraPerStudente && gestoreDB.trovaPerMatricola(matricolaStudente.trim()) == null) {
            throw new BusinessException("Errore, lo studente indicato non esiste!");
        }

        List<PrenotazioneDTO> risultati = new ArrayList<>();

        for (Prenotazione prenotazione :
                gestoreDB.trovaPrenotazioniStoriche(nomeSala, matricolaStudente)) {
            risultati.add(convertiInDTO(prenotazione));
        }

        return risultati;
    }

    /**
     * Codice del bibliotecario che ha effettuato l'accesso.
     *
     * Una sola condizione copre due casi, perche' instanceof e' falso anche per
     * null: nessuna sessione aperta, e sessione aperta da uno studente, che lo
     * storico di tutti non lo puo' vedere.
     */
    private String codiceBibliotecarioInSessione() {
        Utente utente = autenticazione.getUtenteLoggato();

        if (!(utente instanceof Bibliotecario)) {
            throw new BusinessException("Errore, accesso negato!");
        }

        return ((Bibliotecario) utente).getCodiceIdentificativo();
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
