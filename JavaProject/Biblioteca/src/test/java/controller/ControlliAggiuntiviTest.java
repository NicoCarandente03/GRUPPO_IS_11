package controller;

import database.GestorePersistenza;
import dto.PrenotazioneDTO;
import eccezioni.BusinessException;
import entity.Bibliotecario;
import entity.SalaStudio;
import entity.Studente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Controlli che il piano di test funzionale non elenca.
 *
 * Sono tenuti separati dalle tre suite funzionali proprio per non falsare il
 * conteggio dei casi: quelle contengono solo i casi del documento, con gli
 * stessi identificativi, mentre qui stanno le verifiche che nascono dai
 * diagrammi di sequenza, dal modello di dominio e dai requisiti.
 *
 * Ognuna riporta nel proprio commento la fonte da cui deriva.
 */
class ControlliAggiuntiviTest {

    private static final String DESCRIZIONE = "Sala silenziosa piano 1";
    private static final String ORARI = "08:00 - 20:00";

    private Bibliotecario bibliotecarioDiProva() {
        return new Bibliotecario("Marco", "Esposito", "m.esposito@unina.it", "x", "B1234");
    }

    /**
     * Il diagramma di sequenza della creazione interroga trovaSalaPerNome prima
     * di creare la sala, per non ammettere due sale con lo stesso nome.
     */
    @Test
    @DisplayName("creazione, nome della sala gia' presente")
    void nomeSalaGiaEsistente() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        GestioneSaleController controller = new GestioneSaleController(gestoreDB);

        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecarioDiProva());
        when(gestoreDB.trovaSalaPerNome("Sala Lettura A")).thenReturn(
                new SalaStudio("S001", "Sala Lettura A", "gia' esistente", 10, ORARI));

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "40",
                        ORARI, null, "B1234"));

        assertEquals("Errore, esiste già una sala con questo nome!", errore.getMessage());
        verify(gestoreDB, never()).salva(any());
    }

    /**
     * Il modello di dominio lega ogni area ad almeno una postazione, quindi non
     * si possono richiedere piu' aree di quante siano le postazioni.
     */
    @Test
    @DisplayName("creazione, postazioni insufficienti per le aree richieste")
    void postazioniInsufficientiPerLeAree() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        GestioneSaleController controller = new GestioneSaleController(gestoreDB);

        when(gestoreDB.trovaSalaPerNome(anyString())).thenReturn(null);
        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecarioDiProva());

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "2", ORARI,
                        List.of("silenziosa", "consultazione", "lavoro di gruppo"), "B1234"));

        assertEquals("Errore, le postazioni non bastano per il numero di aree indicate!",
                errore.getMessage());
    }

    /**
     * La sala va collegata al bibliotecario che la gestisce, quindi il codice
     * indicato deve corrispondere a un bibliotecario registrato.
     */
    @Test
    @DisplayName("creazione, bibliotecario non registrato")
    void bibliotecarioNonRegistrato() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        GestioneSaleController controller = new GestioneSaleController(gestoreDB);

        when(gestoreDB.trovaSalaPerNome(anyString())).thenReturn(null);
        when(gestoreDB.trovaPerCodiceIdentificativo("B9999")).thenReturn(null);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "40",
                        ORARI, null, "B9999"));

        assertEquals("Errore, il bibliotecario indicato non esiste!", errore.getMessage());
    }

    /**
     * Il diagramma di sequenza dello storico prevede l'esito "nessuna
     * prenotazione trovata per i filtri inseriti": la ricerca riesce, ma non
     * restituisce righe. Non e' un errore e non deve sollevare eccezioni.
     */
    @Test
    @DisplayName("storico, filtri validi ma nessun risultato")
    void storicoSenzaRisultati() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        MonitoraggioSaleController controller = new MonitoraggioSaleController(gestoreDB);

        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecarioDiProva());
        when(gestoreDB.trovaSalaPerNome("Sala Lettura A")).thenReturn(
                new SalaStudio("S001", "Sala Lettura A", "Piano 1", 3, ORARI));
        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(
                new Studente("Giulia", "Romano", "g.romano@studenti.unina.it", "x", "N46001234"));
        when(gestoreDB.trovaPrenotazioniStoriche("Sala Lettura A", "N46001234"))
                .thenReturn(List.of());

        List<PrenotazioneDTO> risultati = controller.consultazioneStoricoPrenotazioni(
                "B1234", "Sala Lettura A", "N46001234");

        assertTrue(risultati.isEmpty(), "la ricerca riesce ma non trova nulla");
    }

    /**
     * Il requisito RF17 chiede che il tempo limite sia configurabile e non
     * scritto nel codice: qui si verifica che venga letto da config.properties,
     * dove vale il valore fissato dal piano di test.
     */
    @Test
    @DisplayName("il limite di annullamento viene letto dalla configurazione")
    void limiteAnnullamentoDallaConfigurazione() {
        GestionePrenotazioneController controller =
                new GestionePrenotazioneController(mock(GestorePersistenza.class));

        assertEquals(60, controller.getLimiteAnnullamentoMinuti());
        assertEquals(30, controller.getIntervalloCheckInMinuti());
    }
}
