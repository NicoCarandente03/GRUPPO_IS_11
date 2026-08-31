package external;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Finto servizio di notifiche, usato nei test.
 *
 * Non invia nulla: registra i messaggi in una lista, cosi' un test puo'
 * verificare che la notifica sia partita e con quale testo, senza dipendere da
 * un canale esterno.
 */
public class ServizioDiNotificheMock implements ServizioDiNotifiche {

    private final List<String> destinatari = new ArrayList<>();
    private final List<String> messaggi = new ArrayList<>();

    @Override
    public void invioNotifica(String destinatario, String testo) {
        destinatari.add(destinatario);
        messaggi.add(testo);
    }

    @Override
    public void invioPromemoria(String destinatario, String idSala, LocalDate data,
                                String fasciaOraria) {
        destinatari.add(destinatario);
        messaggi.add("Promemoria per la sala " + idSala + " del " + data
                + ", fascia " + fasciaOraria);
    }

    public int getNumeroInvii() {
        return messaggi.size();
    }

    public String getUltimoDestinatario() {
        return destinatari.isEmpty() ? null : destinatari.get(destinatari.size() - 1);
    }

    public String getUltimoMessaggio() {
        return messaggi.isEmpty() ? null : messaggi.get(messaggi.size() - 1);
    }

    public void svuota() {
        destinatari.clear();
        messaggi.clear();
    }
}
