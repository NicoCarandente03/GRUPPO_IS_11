package controller;

import database.GestorePersistenza;
import dto.PrenotazioneDTO;
import eccezioni.BusinessException;
import entity.Area;
import entity.Bibliotecario;
import entity.Postazione;
import entity.Prenotazione;
import entity.SalaStudio;
import entity.Studente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Suite funzionale Consultazione Storico Prenotazioni: i cinque casi del piano
 * di test, con gli stessi identificativi.
 *
 * I controlli che il piano non prevede stanno in ControlliAggiuntiviTest.
 *
 * Il filtro sulla sala usa il nome e non l'identificativo: il diagramma di
 * sequenza indica idSala, ma il piano di test passa il nome, ed e' il nome
 * quello che il bibliotecario sceglie.
 */
class MonitoraggioSaleControllerTest {

    private GestorePersistenza gestoreDB;
    private MonitoraggioSaleController controller;

    private Studente studente;
    private SalaStudio sala;

    @BeforeEach
    void preparaAmbiente() {
        gestoreDB = mock(GestorePersistenza.class);
        controller = new MonitoraggioSaleController(gestoreDB);

        studente = new Studente("Giulia", "Romano", "n46001234@studenti.unina.it",
                "studente1", "N46001234");

        sala = new SalaStudio("S001", "Sala Lettura A", "Piano 1", 3, "08:00 - 20:00");

        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(
                new Bibliotecario("Marco", "Esposito", "m.esposito@unina.it", "x", "B1234"));
        when(gestoreDB.trovaSalaPerNome("Sala Lettura A")).thenReturn(sala);
        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(studente);
    }

    /** Costruisce una prenotazione completa, collegata alla sala di prova. */
    private Prenotazione creaPrenotazione(String id) {
        Area area = new Area("A001", "silenziosa");
        sala.aggiungiArea(area);

        Postazione postazione = new Postazione("P-A001-01");
        area.aggiungiPostazione(postazione);

        return new Prenotazione(id, LocalDate.of(2026, 9, 7), "14:00-16:00",
                studente, postazione);
    }

    @Test
    @DisplayName("TC1 ricerca con entrambi i filtri validi")
    void ricercaConFiltriValidi() {
        when(gestoreDB.trovaPrenotazioniStoriche("Sala Lettura A", "N46001234"))
                .thenReturn(List.of(creaPrenotazione("P001")));

        List<PrenotazioneDTO> risultati = controller.consultazioneStoricoPrenotazioni(
                "B1234", "Sala Lettura A", "N46001234");

        assertEquals(1, risultati.size());
        PrenotazioneDTO prenotazione = risultati.get(0);
        assertEquals("P001", prenotazione.getIdPrenotazione());
        assertEquals("N46001234", prenotazione.getMatricolaStudente());
        assertEquals("Sala Lettura A", prenotazione.getNomeSala());
        assertEquals("14:00-16:00", prenotazione.getFasciaOraria());
        assertEquals(Prenotazione.ATTIVA, prenotazione.getStato());
    }

    @Test
    @DisplayName("TC2 ricerca senza filtri, storico completo")
    void ricercaSenzaFiltri() {
        when(gestoreDB.trovaPrenotazioniStoriche(any(), any()))
                .thenReturn(List.of(creaPrenotazione("P001"), creaPrenotazione("P002")));

        List<PrenotazioneDTO> risultati =
                controller.consultazioneStoricoPrenotazioni("B1234", "", "");

        assertEquals(2, risultati.size());
        // senza filtri non deve verificare ne' sala ne' studente
        verify(gestoreDB, never()).trovaSalaPerNome(anyString());
        verify(gestoreDB, never()).trovaPerMatricola(anyString());
    }

    @Test
    @DisplayName("TC3 matricola richiedente non registrata")
    void richiedenteNonRegistrato() {
        when(gestoreDB.trovaPerCodiceIdentificativo("B9999")).thenReturn(null);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.consultazioneStoricoPrenotazioni(
                        "B9999", "Sala Lettura A", "N46001234"));

        assertEquals("Errore, accesso negato!", errore.getMessage());
        verify(gestoreDB, never()).trovaPrenotazioniStoriche(any(), any());
    }

    @Test
    @DisplayName("TC4 nome della sala inesistente")
    void salaInesistente() {
        when(gestoreDB.trovaSalaPerNome("Sala Bellissima")).thenReturn(null);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.consultazioneStoricoPrenotazioni(
                        "B1234", "Sala Bellissima", "N46001234"));

        assertEquals("Errore, la sala studio selezionata non esiste!", errore.getMessage());
        verify(gestoreDB, never()).trovaPrenotazioniStoriche(any(), any());
    }

    @Test
    @DisplayName("TC5 matricola dello studente inesistente")
    void studenteInesistente() {
        when(gestoreDB.trovaPerMatricola("N46999999")).thenReturn(null);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.consultazioneStoricoPrenotazioni(
                        "B1234", "Sala Lettura A", "N46999999"));

        assertEquals("Errore, lo studente indicato non esiste!", errore.getMessage());
    }
}
