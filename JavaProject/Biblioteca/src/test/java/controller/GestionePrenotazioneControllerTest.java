package controller;

import database.GestorePersistenza;
import eccezioni.BusinessException;

import entity.Area;
import entity.Postazione;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Studente;
import external.ServizioDiNotificheMock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Suite funzionale Annullamento Prenotazione: i sei casi del piano di test, uno
 * per uno, con gli stessi identificativi.
 *
 * I controlli che il piano non prevede stanno in ControlliAggiuntiviTest.
 *
 * Il livello Database viene sostituito da un finto GestorePersistenza, cosi' i
 * test non hanno bisogno di MySQL acceso e girano sempre allo stesso modo.
 * Anche il servizio di notifiche e' finto, per non stampare nulla durante i test.
 *
 * L'istante della richiesta viene passato esplicitamente: senza, l'esito
 * dipenderebbe da quando si esegue la suite.
 */
class GestionePrenotazioneControllerTest {

    private static final LocalDate DATA_PRENOTAZIONE = LocalDate.of(2026, 9, 7);
    private static final String FASCIA = "14:00-16:00";

    // la fascia inizia alle 14:00, quindi cinque ore prima e' entro il limite
    private static final LocalDateTime IN_TEMPO = LocalDateTime.of(2026, 9, 7, 9, 0);

    // trenta minuti prima: il limite di annullamento e' sessanta
    private static final LocalDateTime TROPPO_TARDI = LocalDateTime.of(2026, 9, 7, 13, 30);

    private GestorePersistenza gestoreDB;
    private GestionePrenotazioneController controller;
    private ServizioDiNotificheMock notifiche;

    private Studente titolare;

    @BeforeEach
    void preparaAmbiente() {
        gestoreDB = mock(GestorePersistenza.class);
        controller = new GestionePrenotazioneController(gestoreDB);

        notifiche = new ServizioDiNotificheMock();
        NotificaController.getInstance().setServizio(notifiche);

        titolare = new Studente("Giulia", "Romano", "n46001234@studenti.unina.it",
                "studente1", "N46001234");
    }

    /** Costruisce una prenotazione completa di sala, area e postazione. */
    private Prenotazione creaPrenotazione(String idPrenotazione, Studente studente) {
        SalaStudio sala = new SalaStudio("S001", "Sala Lettura A", "Piano 1", 3, "08:00 - 20:00");
        Area area = new Area("A001", "silenziosa");
        sala.aggiungiArea(area);

        Postazione postazione = new Postazione("P-A001-01");
        area.aggiungiPostazione(postazione);

        return new Prenotazione(idPrenotazione, DATA_PRENOTAZIONE, FASCIA, studente, postazione);
    }

    @Test
    @DisplayName("TC1 annullamento valido")
    void annullamentoValido() {
        Prenotazione prenotazione = creaPrenotazione("P001", titolare);
        when(gestoreDB.trovaPrenotazionePerId("P001")).thenReturn(prenotazione);

        controller.annullamentoPrenotazione("P001", "N46001234", IN_TEMPO);

        assertEquals(Prenotazione.ANNULLATA, prenotazione.getStato());
        assertTrue(prenotazione.getPostazione().isDisponibile(),
                "la postazione deve tornare disponibile");
        verify(gestoreDB).aggiorna(prenotazione);
        assertEquals(1, notifiche.getNumeroInvii(), "deve partire la notifica di annullamento");
        assertEquals("N46001234", notifiche.getUltimoDestinatario());
    }

    @Test
    @DisplayName("TC2 identificativo di prenotazione inesistente")
    void prenotazioneInesistente() {
        when(gestoreDB.trovaPrenotazionePerId("P999")).thenReturn(null);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.annullamentoPrenotazione("P999", "N46001234", IN_TEMPO));

        assertEquals("Errore, la prenotazione selezionata è inesistente!", errore.getMessage());
        verify(gestoreDB, never()).aggiorna(any());
    }

    @Test
    @DisplayName("TC3 utente diverso dal proprietario")
    void utenteNonProprietario() {
        Prenotazione prenotazione = creaPrenotazione("P001", titolare);
        when(gestoreDB.trovaPrenotazionePerId("P001")).thenReturn(prenotazione);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.annullamentoPrenotazione("P001", "N46004321", IN_TEMPO));

        assertEquals("Errore, non sei autorizzato ad annullare questa prenotazione!",
                errore.getMessage());
        assertEquals(Prenotazione.ATTIVA, prenotazione.getStato(),
                "lo stato non deve cambiare");
    }

    @Test
    @DisplayName("TC4 prenotazione gia' annullata")
    void prenotazioneGiaAnnullata() {
        Prenotazione prenotazione = creaPrenotazione("P002", titolare);
        prenotazione.aggiornaStatoPrenotazione(Prenotazione.ANNULLATA);
        when(gestoreDB.trovaPrenotazionePerId("P002")).thenReturn(prenotazione);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.annullamentoPrenotazione("P002", "N46001234", IN_TEMPO));

        assertEquals("Errore, la prenotazione risulta già annullata!", errore.getMessage());
        verify(gestoreDB, never()).aggiorna(any());
    }

    @Test
    @DisplayName("TC5 prenotazione scaduta")
    void prenotazioneScaduta() {
        Prenotazione prenotazione = creaPrenotazione("P003", titolare);
        prenotazione.aggiornaStatoPrenotazione(Prenotazione.SCADUTA);
        when(gestoreDB.trovaPrenotazionePerId("P003")).thenReturn(prenotazione);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.annullamentoPrenotazione("P003", "N46001234", IN_TEMPO));

        assertEquals("Errore, la prenotazione risulta scaduta!", errore.getMessage());
    }

    @Test
    @DisplayName("TC6 limite temporale superato")
    void limiteTemporaleSuperato() {
        Prenotazione prenotazione = creaPrenotazione("P004", titolare);
        when(gestoreDB.trovaPrenotazionePerId("P004")).thenReturn(prenotazione);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.annullamentoPrenotazione("P004", "N46001234", TROPPO_TARDI));

        assertEquals("Errore, il tempo limite per l'annullamento è stato superato!",
                errore.getMessage());
        assertEquals(Prenotazione.ATTIVA, prenotazione.getStato());
        assertEquals(0, notifiche.getNumeroInvii(), "nessuna notifica se l'annullamento fallisce");
    }

    @Test
    @DisplayName("TC7 effettua prenotazione scegliendo postazione specifica")
    void effettuaPrenotazioneConPostazione() {
        // Creiamo una finta postazione per il test
        Postazione postazione = new Postazione("P-01");

        // Istruiamo il finto DB di Michela (gestoreDB)
        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(titolare);
        when(gestoreDB.trovaPostazionePerId("P-01")).thenReturn(postazione);

        // Eseguiamo il tuo metodo
        boolean esito = controller.effettuaPrenotazione("N46001234", DATA_PRENOTAZIONE, "S001", FASCIA, "A001", "P-01");

        // Asserzioni
        assertTrue(esito, "La prenotazione deve andare a buon fine e restituire true");
        verify(gestoreDB).salva(any(Prenotazione.class));
    }

    @Test
    @DisplayName("TC8 effettua prenotazione con assegnazione automatica")
    void effettuaPrenotazioneAssegnazioneAutomatica() {
        Postazione postazione = new Postazione("P-01");
        List<Postazione> postazioniLibere = new ArrayList<>();
        postazioniLibere.add(postazione);

        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(titolare);
        // Simuliamo che il DB trovi una sedia libera per l'assegnazione automatica
        when(gestoreDB.trovaPostazioniLibere("S001", DATA_PRENOTAZIONE, FASCIA)).thenReturn(postazioniLibere);

        // Eseguiamo il metodo passando stringhe vuote per area e postazione
        boolean esito = controller.effettuaPrenotazione("N46001234", DATA_PRENOTAZIONE, "S001", FASCIA, "", "");

        assertTrue(esito, "La prenotazione deve riuscire grazie all'assegnazione automatica");
        verify(gestoreDB).salva(any(Prenotazione.class));
    }

    @Test
    @DisplayName("TC9 effettua prenotazione fallita per sala piena")
    void effettuaPrenotazioneSalaPiena() {
        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(titolare);

        // Simuliamo che il DB NON trovi nessuna sedia libera (lista vuota)
        when(gestoreDB.trovaPostazioniLibere("S001", DATA_PRENOTAZIONE, FASCIA)).thenReturn(new ArrayList<>());

        boolean esito = controller.effettuaPrenotazione("N46001234", DATA_PRENOTAZIONE, "S001", FASCIA, "", "");

        assertFalse(esito, "La prenotazione deve fallire restituendo false se non ci sono posti");
        // Verifichiamo che in caso di errore, il metodo salva() NON venga mai chiamato
        verify(gestoreDB, never()).salva(any(Prenotazione.class));
    }
}