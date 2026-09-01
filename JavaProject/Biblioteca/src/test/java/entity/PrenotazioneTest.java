package entity;

import eccezioni.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test di unita' sulla logica di dominio della Prenotazione.
 *
 * Non serve ne' il database ne' un finto livello di persistenza: la classe
 * contiene solo stato e regole, e questi test le verificano direttamente. E'
 * l'esempio piu' semplice di test di unita' del progetto.
 */
class PrenotazioneTest {

    private static final LocalDate DATA = LocalDate.of(2026, 9, 7);
    private static final String FASCIA = "14:00-16:00";
    private static final int LIMITE_ANNULLAMENTO = 60;
    private static final int INTERVALLO_CHECKIN = 30;

    private Prenotazione prenotazione;
    private Postazione postazione;

    @BeforeEach
    void preparaPrenotazione() {
        SalaStudio sala = new SalaStudio("S001", "Sala Lettura A", "Piano 1", 1, "08:00 - 20:00");
        Area area = new Area("A001", "silenziosa");
        sala.aggiungiArea(area);

        postazione = new Postazione("P-A001-01");
        area.aggiungiPostazione(postazione);

        Studente studente = new Studente("Giulia", "Romano", "g.romano@studenti.unina.it",
                "x", "N46001234");

        prenotazione = new Prenotazione("P001", DATA, FASCIA, studente, postazione);
    }

    @Test
    @DisplayName("una prenotazione nasce nello stato attiva")
    void nasceAttiva() {
        assertEquals(Prenotazione.ATTIVA, prenotazione.getStato());
    }

    @Test
    @DisplayName("una fascia oraria non prevista viene rifiutata")
    void fasciaOrariaNonValida() {
        Studente studente = new Studente("Luca", "Verdi", "l.verdi@studenti.unina.it",
                "x", "N46004321");

        assertThrows(BusinessException.class,
                () -> new Prenotazione("P002", DATA, "13:00-15:00", studente, postazione));
    }

    @Test
    @DisplayName("annullabile se mancano piu' di sessanta minuti")
    void annullabileInAnticipo() {
        LocalDateTime cinqueOrePrima = LocalDateTime.of(2026, 9, 7, 9, 0);
        assertTrue(prenotazione.isAnnullabile(LIMITE_ANNULLAMENTO, cinqueOrePrima));
    }

    @Test
    @DisplayName("non annullabile a ridosso dell'inizio")
    void nonAnnullabileARidosso() {
        LocalDateTime trentaMinutiPrima = LocalDateTime.of(2026, 9, 7, 13, 30);
        assertFalse(prenotazione.isAnnullabile(LIMITE_ANNULLAMENTO, trentaMinutiPrima));
    }

    @Test
    @DisplayName("annullare libera la postazione e cambia lo stato")
    void annullareLiberaLaPostazione() {
        postazione.setDisponibile(false);
        LocalDateTime cinqueOrePrima = LocalDateTime.of(2026, 9, 7, 9, 0);

        assertTrue(prenotazione.annulla(LIMITE_ANNULLAMENTO, cinqueOrePrima));
        assertEquals(Prenotazione.ANNULLATA, prenotazione.getStato());
        assertTrue(postazione.isDisponibile());
    }

    @Test
    @DisplayName("una prenotazione gia' annullata non si annulla due volte")
    void nonSiAnnullaDueVolte() {
        LocalDateTime cinqueOrePrima = LocalDateTime.of(2026, 9, 7, 9, 0);
        prenotazione.annulla(LIMITE_ANNULLAMENTO, cinqueOrePrima);

        assertFalse(prenotazione.annulla(LIMITE_ANNULLAMENTO, cinqueOrePrima));
    }

    @Test
    @DisplayName("il check-in vale nella finestra attorno all'inizio della fascia")
    void finestraDiCheckin() {
        assertTrue(prenotazione.isCheckinValido(INTERVALLO_CHECKIN,
                LocalDateTime.of(2026, 9, 7, 14, 10)), "dieci minuti dopo l'inizio");
        assertTrue(prenotazione.isCheckinValido(INTERVALLO_CHECKIN,
                LocalDateTime.of(2026, 9, 7, 13, 45)), "un quarto d'ora prima");
        assertFalse(prenotazione.isCheckinValido(INTERVALLO_CHECKIN,
                LocalDateTime.of(2026, 9, 7, 12, 0)), "due ore prima");
        assertFalse(prenotazione.isCheckinValido(INTERVALLO_CHECKIN,
                LocalDateTime.of(2026, 9, 7, 15, 0)), "un'ora dopo l'inizio");
    }

    @Test
    @DisplayName("il check-in risulta scaduto oltre l'intervallo di tolleranza")
    void checkinScadutoOltreTolleranza() {
        //la fascia oraria inizia alle 14. Con tolleranza 30 min, chiude alle 14:30
        LocalDateTime orarioRitardo = LocalDateTime.of(2026, 9, 7 ,14, 35);

        assertTrue(prenotazione.isCheckinScaduto(INTERVALLO_CHECKIN, orarioRitardo), "A 35 min dall'inizio, il tempo limite deve risultare superato");
    }

    @Test
    @DisplayName("il check-in non è scaduto se lo studente è in anticipo o in orario")
    void checkinNonScadutoInAnticipo() {
        LocalDateTime orarioAnticipo = LocalDateTime.of(2026, 9, 7, 13, 0);

        assertFalse(prenotazione.isCheckinScaduto(INTERVALLO_CHECKIN, orarioAnticipo), "Se si è in anticipo, il check-in è invalido ma non scaduto");
    }

    @Test
    @DisplayName("far scadere libera la postazione")
    void scadereLiberaLaPostazione() {
        postazione.setDisponibile(false);
        prenotazione.scadi();

        assertEquals(Prenotazione.SCADUTA, prenotazione.getStato());
        assertTrue(postazione.isDisponibile());
    }

    @Test
    @DisplayName("area e sala si risalgono dalla postazione prenotata")
    void navigazioneVersoAreaESala() {
        assertEquals("A001", prenotazione.getArea().getIdArea());
        assertEquals("Sala Lettura A", prenotazione.getSala().getNome());
    }

    @Test
    @DisplayName("uno stato non riconosciuto viene rifiutato")
    void statoNonRiconosciuto() {
        assertThrows(BusinessException.class,
                () -> prenotazione.aggiornaStatoPrenotazione("SOSPESA"));
    }
}
