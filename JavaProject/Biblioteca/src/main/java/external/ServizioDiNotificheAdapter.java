package external;

import java.time.LocalDate;

/**
 * Implementazione concreta del servizio esterno di notifiche.
 *
 * Il canale reale, email o SMS, non fa parte del sistema: qui l'invio viene
 * simulato stampando il messaggio, come si fa nell'esempio del corso. Quello che
 * conta e' che il resto del programma dipenda dall'interfaccia e non da questa
 * classe, cosi' nei test si usa ServizioDiNotificheMock.
 */
public class ServizioDiNotificheAdapter implements ServizioDiNotifiche {

    private static final String DOMINIO = "@studenti.unina.it";

    @Override
    public void invioNotifica(String destinatario, String testo) {
        stampa(destinatario, "Comunicazione dalla biblioteca", testo);
    }

    @Override
    public void invioPromemoria(String destinatario, String idSala, LocalDate data,
                                String fasciaOraria) {
        String testo = "Ti ricordiamo la prenotazione nella sala " + idSala
                + " del " + data + ", fascia " + fasciaOraria + ".";
        stampa(destinatario, "Promemoria prenotazione", testo);
    }

    private void stampa(String destinatario, String oggetto, String testo) {
        System.out.println();
        System.out.println("--- servizio di notifiche ---");
        System.out.println("a:       " + destinatario + DOMINIO);
        System.out.println("oggetto: " + oggetto);
        System.out.println("testo:   " + testo);
        System.out.println("-----------------------------");
    }
}
