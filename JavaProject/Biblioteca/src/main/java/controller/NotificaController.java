package controller;

import database.GestorePersistenza;
import entity.Notifica;
import entity.Prenotazione;
import entity.Studente;
import external.ServizioDiNotifiche;
import external.ServizioDiNotificheAdapter;

public class NotificaController {

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static NotificaController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // 3. Servizio esterno di invio, isolato dietro l'interfaccia
    private ServizioDiNotifiche servizio;

    // Costruttore privato
    private NotificaController() {
        this.gestoreDB = new GestorePersistenza();
        this.servizio = new ServizioDiNotificheAdapter();
    }

    /**
     * Costruttore usato dai test, che passano un servizio finto al posto di
     * quello reale. Non e' pubblico proprio per non essere usato altrove.
     */
    NotificaController(ServizioDiNotifiche servizio) {
        this.gestoreDB = new GestorePersistenza();
        this.servizio = servizio;
    }

    public static NotificaController getInstance() {
        if (instance == null) {
            instance = new NotificaController();
        }
        return instance;
    }

    /**
     * Permette di sostituire il servizio esterno sull'istanza singleton, cosa
     * che serve ai test quando lavorano tramite getInstance().
     */
    void setServizio(ServizioDiNotifiche servizio) {
        this.servizio = servizio;
    }

    /**
     * Avvisa lo studente che la sua prenotazione e' stata annullata
     */
    public void notificaAnnullamento(Prenotazione prenotazione) {
        if (prenotazione == null || prenotazione.getStudente() == null) {
            return;
        }

        Studente destinatario = prenotazione.getStudente();
        String testo = "La tua prenotazione del " + prenotazione.getData()
                + " per la fascia " + prenotazione.getFasciaOraria()
                + " e' stata annullata. La postazione e' tornata disponibile.";

        creaNotifica(testo).invia(destinatario);
    }

    /**
     * Crea la notifica gia' collegata al servizio esterno di invio.
     */
    private Notifica creaNotifica(String testo) {
        return new Notifica(testo, servizio);
    }
}