package controller;

import database.GestorePersistenza;
import dto.PrenotazioneDTO;
import eccezioni.BusinessException;
import entity.Bibliotecario;
import entity.SalaStudio;
import entity.Studente;
import entity.Utente;
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

    /** Sessione finta, con l'utente indicato gia' loggato. */
    private AutenticazioneController sessioneDi(Utente utente) {
        AutenticazioneController autenticazione = mock(AutenticazioneController.class);
        when(autenticazione.getUtenteLoggato()).thenReturn(utente);
        return autenticazione;
    }

    /**
     * Il diagramma di sequenza della creazione interroga trovaSalaPerNome prima
     * di creare la sala, per non ammettere due sale con lo stesso nome.
     */
    @Test
    @DisplayName("creazione, nome della sala gia' presente")
    void nomeSalaGiaEsistente() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        GestioneSaleController controller =
                new GestioneSaleController(gestoreDB, sessioneDi(bibliotecarioDiProva()));

        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecarioDiProva());
        when(gestoreDB.trovaSalaPerNome("Sala Lettura A")).thenReturn(
                new SalaStudio("S001", "Sala Lettura A", "gia' esistente", 10, ORARI));

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "40",
                        ORARI, null));

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
        GestioneSaleController controller =
                new GestioneSaleController(gestoreDB, sessioneDi(bibliotecarioDiProva()));

        when(gestoreDB.trovaSalaPerNome(anyString())).thenReturn(null);
        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecarioDiProva());

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "2", ORARI,
                        List.of("silenziosa", "consultazione", "lavoro di gruppo")));

        assertEquals("Errore, le postazioni non bastano per il numero di aree indicate!",
                errore.getMessage());
    }

    /**
     * La sala va collegata al bibliotecario che la gestisce, quindi quello in
     * sessione deve corrispondere a un bibliotecario registrato.
     */
    @Test
    @DisplayName("creazione, bibliotecario non registrato")
    void bibliotecarioNonRegistrato() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        GestioneSaleController controller = new GestioneSaleController(gestoreDB,
                sessioneDi(new Bibliotecario("Ignoto", "Ignoto", "ignoto@unina.it", "x", "B9999")));

        when(gestoreDB.trovaSalaPerNome(anyString())).thenReturn(null);
        when(gestoreDB.trovaPerCodiceIdentificativo("B9999")).thenReturn(null);

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "40",
                        ORARI, null));

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
        MonitoraggioSaleController controller =
                new MonitoraggioSaleController(gestoreDB, sessioneDi(bibliotecarioDiProva()));

        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecarioDiProva());
        when(gestoreDB.trovaSalaPerNome("Sala Lettura A")).thenReturn(
                new SalaStudio("S001", "Sala Lettura A", "Piano 1", 3, ORARI));
        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(
                new Studente("Giulia", "Romano", "g.romano@studenti.unina.it", "x", "N46001234"));
        when(gestoreDB.trovaPrenotazioniStoriche("Sala Lettura A", "N46001234"))
                .thenReturn(List.of());

        List<PrenotazioneDTO> risultati = controller.consultazioneStoricoPrenotazioni(
                "Sala Lettura A", "N46001234");

        assertTrue(risultati.isEmpty(), "la ricerca riesce ma non trova nulla");
    }

    /**
     * L'identita' arriva dalla sessione e non da chi invoca il metodo: senza
     * accesso effettuato non si crea nessuna sala.
     */
    @Test
    @DisplayName("creazione, nessun accesso effettuato")
    void creazioneSenzaAccessoEffettuato() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        GestioneSaleController controller =
                new GestioneSaleController(gestoreDB, sessioneDi(null));

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "40",
                        ORARI, null));

        assertEquals("Errore, accesso negato!", errore.getMessage());
        verify(gestoreDB, never()).salva(any());
    }

    /**
     * La catena completa, con un AutenticazioneController vero al posto di una
     * sessione finta: il bibliotecario effettua l'accesso e il controller ricava
     * da li' il codice, senza che nessuno glielo passi.
     */
    @Test
    @DisplayName("creazione, il codice arriva dalla sessione aperta dal login")
    void codiceBibliotecarioLettoDallaSessione() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        Bibliotecario bibliotecario = bibliotecarioDiProva();

        AutenticazioneController autenticazione = new AutenticazioneController(gestoreDB);
        when(gestoreDB.trovaUtentePerEmail("m.esposito@unina.it")).thenReturn(bibliotecario);
        autenticazione.login("m.esposito@unina.it", "x");

        GestioneSaleController controller = new GestioneSaleController(gestoreDB, autenticazione);
        when(gestoreDB.trovaSalaPerNome(anyString())).thenReturn(null);
        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecario);

        controller.creazioneAulaStudio("Sala Lettura A", DESCRIZIONE, "40", ORARI, null);

        verify(gestoreDB).trovaPerCodiceIdentificativo("B1234");
        verify(gestoreDB).salva(any(SalaStudio.class));
    }

    /**
     * Cammino 1-2-3 di matricolaInSessione: senza uno studente in sessione
     * l'annullamento non arriva nemmeno a cercare la prenotazione.
     */
    @Test
    @DisplayName("annullamento, nessun accesso effettuato")
    void annullamentoSenzaAccessoEffettuato() {
        GestorePersistenza gestoreDB = mock(GestorePersistenza.class);
        GestionePrenotazioneController controller =
                new GestionePrenotazioneController(gestoreDB, sessioneDi(null));

        BusinessException errore = assertThrows(BusinessException.class,
                () -> controller.annullamentoPrenotazione("P001"));

        assertEquals("Errore, non sei autorizzato ad annullare questa prenotazione!",
                errore.getMessage());
        verify(gestoreDB, never()).trovaPrenotazionePerId(anyString());
    }

    /**
     * Il requisito RF17 chiede che il tempo limite sia configurabile e non
     * scritto nel codice: qui si verifica che venga letto da config.properties,
     * dove vale il valore fissato dal piano di test.
     */
    @Test
    @DisplayName("il limite di annullamento viene letto dalla configurazione")
    void limiteAnnullamentoDallaConfigurazione() {
        GestionePrenotazioneController controller = new GestionePrenotazioneController(
                mock(GestorePersistenza.class), mock(AutenticazioneController.class));

        assertEquals(60, controller.getLimiteAnnullamentoMinuti());
        assertEquals(30, controller.getIntervalloCheckInMinuti());
    }
}
