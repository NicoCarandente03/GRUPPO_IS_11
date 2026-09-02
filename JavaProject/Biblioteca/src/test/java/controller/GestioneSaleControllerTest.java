package controller;

import database.GestorePersistenza;
import dto.SalaStudioDTO;
import eccezioni.BusinessException;
import entity.Bibliotecario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
 * Suite funzionale Creazione Aula Studio: i casi del piano di test, con gli
 * stessi identificativi.
 *
 * Il numero di postazioni arriva al controller come testo, cosi' come l'utente
 * lo ha digitato: il diagramma delle classi non ne dichiara il tipo, e
 * riconoscere che "quaranta" non e' un numero fa parte della validazione.
 *
 * I controlli che il piano non prevede, come il nome gia' esistente, stanno in
 * ControlliAggiuntiviTest.
 */
class GestioneSaleControllerTest {

    private static final String DESCRIZIONE = "Sala silenziosa piano 1";
    private static final String ORARI = "08:00 - 20:00";

    private GestorePersistenza gestoreDB;
    private AutenticazioneController autenticazione;
    private GestioneSaleController controller;

    @BeforeEach
    void preparaAmbiente() {
        gestoreDB = mock(GestorePersistenza.class);
        autenticazione = mock(AutenticazioneController.class);
        controller = new GestioneSaleController(gestoreDB, autenticazione);

        Bibliotecario bibliotecario =
                new Bibliotecario("Marco", "Esposito", "m.esposito@unina.it", "x", "B1234");

        // il bibliotecario ha gia' effettuato l'accesso, salvo dove serve il contrario
        when(autenticazione.getUtenteLoggato()).thenReturn(bibliotecario);

        // nessuna sala omonima e bibliotecario esistente, salvo dove serve il contrario
        when(gestoreDB.trovaSalaPerNome(anyString())).thenReturn(null);
        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(bibliotecario);
    }

    private SalaStudioDTO crea(String nome, String descrizione, String postazioni,
                               String orari, List<String> aree) {
        return controller.creazioneAulaStudio(nome, descrizione, postazioni, orari, aree);
    }

    private String messaggioErrore(String nome, String descrizione, String postazioni,
                                   String orari, List<String> aree) {
        BusinessException errore = assertThrows(BusinessException.class,
                () -> crea(nome, descrizione, postazioni, orari, aree));
        return errore.getMessage();
    }

    @Test
    @DisplayName("TC1 sala senza aree, tutti gli input validi")
    void creazioneSenzaAree() {
        SalaStudioDTO sala = crea("Sala Lettura A", DESCRIZIONE, "40", ORARI, null);

        assertEquals("Sala Lettura A", sala.getNome());
        assertEquals(40, sala.getNumPostazioniTotali());
        assertEquals(1, sala.getAree().size(),
                "senza suddivisione la sala ha comunque un'area che la copre tutta");
        assertEquals("generica", sala.getAree().get(0).getTipo());
        assertEquals(40, sala.getAree().get(0).getNumPostazioni());
        verify(gestoreDB).salva(any());
    }

    @Test
    @DisplayName("TC2 sala con due aree valide")
    void creazioneConDueAree() {
        SalaStudioDTO sala = crea("Sala Studio Group", "Sala mista", "40", ORARI,
                List.of("silenziosa", "consultazione"));

        assertEquals(2, sala.getAree().size());
        assertEquals(20, sala.getAree().get(0).getNumPostazioni());
        assertEquals(20, sala.getAree().get(1).getNumPostazioni());
    }

    @Test
    @DisplayName("TC3 nome vuoto")
    void nomeVuoto() {
        assertEquals("Errore, il nome della sala non può essere vuoto!",
                messaggioErrore("", DESCRIZIONE, "40", ORARI, null));
        verify(gestoreDB, never()).salva(any());
    }

    @Test
    @DisplayName("TC4 nome oltre quaranta caratteri")
    void nomeTroppoLungo() {
        String nome = "Sala di Lettura e Consultazione del Dipartimento di Ingegneria";
        assertEquals("Errore, il nome inserito è troppo lungo!",
                messaggioErrore(nome, DESCRIZIONE, "40", ORARI, null));
    }

    @Test
    @DisplayName("TC5 nome con simboli non ammessi")
    void nomeConSimboli() {
        assertEquals("Errore, il formato del nome inserito non è valido!",
                messaggioErrore("Sala#1@Lettura!", DESCRIZIONE, "40", ORARI, null));
    }

    @Test
    @DisplayName("TC6 descrizione oltre duecento caratteri")
    void descrizioneTroppoLunga() {
        assertEquals("Errore, la descrizione inserita è troppo lunga!",
                messaggioErrore("Sala Lettura A", "x".repeat(250), "40", ORARI, null));
    }

    @Test
    @DisplayName("TC7 numero di postazioni uguale a zero")
    void postazioniZero() {
        assertEquals("Errore, il numero di postazioni deve essere maggiore di zero!",
                messaggioErrore("Sala Lettura A", DESCRIZIONE, "0", ORARI, null));
    }

    @Test
    @DisplayName("TC8 numero di postazioni negativo")
    void postazioniNegative() {
        assertEquals("Errore, il numero di postazioni deve essere maggiore di zero!",
                messaggioErrore("Sala Lettura A", DESCRIZIONE, "-5", ORARI, null));
    }

    @Test
    @DisplayName("TC9 numero di postazioni non numerico")
    void postazioniNonNumeriche() {
        assertEquals("Errore, formato numero postazioni non valido!",
                messaggioErrore("Sala Lettura A", DESCRIZIONE, "quaranta", ORARI, null));
        verify(gestoreDB, never()).salva(any());
    }

    @Test
    @DisplayName("TC10 orari di apertura incoerenti")
    void orariIncoerenti() {
        assertEquals("Errore, orari di apertura non coerenti!",
                messaggioErrore("Sala Lettura A", DESCRIZIONE, "40", "20:00 - 08:00", null));
    }

    @Test
    @DisplayName("TC11 tipo di area non ammesso")
    void tipoAreaNonAmmesso() {
        assertEquals("Errore, tipo di area non riconosciuto",
                messaggioErrore("Sala Studio Group", "Sala mista", "40", ORARI, List.of("musica")));
    }
}
