package external;

import java.time.LocalDate;

import entity.Prenotazione;
import entity.Postazione;

/**
 * Servizio esterno di invio notifiche, stereotipo external del diagramma di
 * design.
 *
 * E' un'interfaccia per isolare il sistema dal canale reale di invio e per
 * poterlo sostituire con un mock nei test, come chiedono i vincoli
 * architetturali della specifica.
 *
 */
public interface ServizioDiNotifiche {

    void invioPromemoria(String destinatario, String idSala, LocalDate data, String fasciaOraria);

    void invioNotifica(String destinatario, String testo);

    public static void inviaNotificaConferma(String matricola, Prenotazione prenotazione) {

        // Creazione di una stampa a video formattata per simulare il testo di un'email.
        System.out.println("\n=====================================================");
        System.out.println("[SISTEMA] - Connessione al Servizio di Notifiche...");
        System.out.println("-> Invio email a: matricola_" + matricola + "@studenti.unina.it");
        System.out.println("-> Oggetto: Conferma Prenotazione Biblioteca");
        System.out.println("-> Testo:");
        System.out.println("   Gentile Studente,");
        System.out.println("   Ti confermiamo che la tua prenotazione per il giorno " + prenotazione.getData());
        System.out.println("   nella fascia oraria " + prenotazione.getFasciaOraria() + " è andata a buon fine.");

        // Se la prenotazione ha una postazione specifica
        if (prenotazione.getPostazione() != null) {
            System.out.println("   ID Postazione assegnata: " + prenotazione.getPostazione().getIdPostazione());
        }

        System.out.println("   Stato attuale: " + prenotazione.getStato());
        System.out.println("=====================================================\n");
    }
}
