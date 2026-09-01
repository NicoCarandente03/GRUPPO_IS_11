package controller;

import database.GestorePersistenza;
import eccezioni.BusinessException;
import entity.Bibliotecario;
import entity.Studente;
import entity.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Suite funzionale completa per l'AutenticazioneController che copre i casi di test per:
 * - Registrazione
 * - Log-in
 */
class AutenticazioneControllerTest {

    private GestorePersistenza gestoreDB;
    private AutenticazioneController controller;

    @BeforeEach
    void preparaAmbiente() {
        gestoreDB = mock(GestorePersistenza.class);
        controller = new AutenticazioneController(gestoreDB);
    }

    // TEST REGISTRAZIONE

    @Test
    @DisplayName("TC1 registrazione valida di uno studente")
    void registrazioneStudenteValida() {
        when(gestoreDB.trovaUtentePerEmail("mario.rossi@studenti.unina.it")).thenReturn(null);
        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(null);

        Utente nuovoUtente = controller.registrazione(
                "Mario", "Rossi", "mario.rossi@studenti.unina.it", "Password123", "Studente", "N46001234"
        );

        assertTrue(nuovoUtente instanceof Studente);
        assertEquals("N46001234", ((Studente) nuovoUtente).getMatricola());
        verify(gestoreDB).salva(any(Studente.class));
    }

    @Test
    @DisplayName("TC2 registrazione valida di un bibliotecario")
    void registrazioneBibliotecarioValida() {
        when(gestoreDB.trovaUtentePerEmail("anna.bianchi@biblioteca.it")).thenReturn(null);
        when(gestoreDB.trovaPerCodiceIdentificativo("B1234")).thenReturn(null);

        Utente nuovoUtente = controller.registrazione(
                "Anna", "Bianchi", "anna.bianchi@biblioteca.it", "Password123", "Bibliotecario", "B1234"
        );

        assertTrue(nuovoUtente instanceof Bibliotecario);
        assertEquals("B1234", ((Bibliotecario) nuovoUtente).getCodiceIdentificativo());
        verify(gestoreDB).salva(any(Bibliotecario.class));
    }

    @Test
    @DisplayName("TC3 registrazione fallita: dati sintatticamente non validi")
    void registrazioneDatiSintatticamenteNonValidi() {
        BusinessException eccezione = assertThrows(BusinessException.class, () -> {
            controller.registrazione(
                    "Mario", "Rossi", "mario.rossi@studenti.unina.it", "123", "Studente", "N46001234"
            );
        });

        assertEquals("Dati non validi sintatticamente", eccezione.getMessage());
        verify(gestoreDB, never()).salva(any());
    }

    @Test
    @DisplayName("TC4 registrazione fallita: email già presente nel sistema")
    void registrazioneEmailDuplicata() {
        Studente utenteEsistente = new Studente("Luigi", "Verdi", "mario.rossi@studenti.unina.it", "hash", "N46009999");
        when(gestoreDB.trovaUtentePerEmail("mario.rossi@studenti.unina.it")).thenReturn(utenteEsistente);

        BusinessException eccezione = assertThrows(BusinessException.class, () -> {
            controller.registrazione(
                    "Mario", "Rossi", "mario.rossi@studenti.unina.it", "Password123", "Studente", "N46001234"
            );
        });

        assertEquals("Esiste già un account con questa email", eccezione.getMessage());
        verify(gestoreDB, never()).salva(any());
    }

    @Test
    @DisplayName("TC5 registrazione fallita: matricola già presente")
    void registrazioneMatricolaDuplicata() {
        when(gestoreDB.trovaUtentePerEmail("nuovo@studenti.unina.it")).thenReturn(null);

        Studente utenteEsistente = new Studente("Luigi", "Verdi", "luigi.verdi@email.it", "hash", "N46001234");
        when(gestoreDB.trovaPerMatricola("N46001234")).thenReturn(utenteEsistente);

        BusinessException eccezione = assertThrows(BusinessException.class, () -> {
            controller.registrazione(
                    "Mario", "Rossi", "nuovo@studenti.unina.it", "Password123", "Studente", "N46001234"
            );
        });

        assertEquals("Matricola già presente nel sistema", eccezione.getMessage());
        verify(gestoreDB, never()).salva(any());
    }

    @Test
    @DisplayName("TC6 registrazione fallita: ruolo non riconosciuto")
    void registrazioneRuoloSconosciuto() {
        when(gestoreDB.trovaUtentePerEmail("mario.rossi@email.it")).thenReturn(null);

        BusinessException eccezione = assertThrows(BusinessException.class, () -> {
            controller.registrazione(
                    "Mario", "Rossi", "mario.rossi@email.it", "Password123", "Docente", "DOC-001"
            );
        });

        assertEquals("Ruolo non riconosciuto: Docente", eccezione.getMessage());
        verify(gestoreDB, never()).salva(any());
    }


    // TEST LOGIN

    @Test
    @DisplayName("TC7 Login effettuato con successo")
    void loginValido() {
        // Simuliamo la creazione dell'utente con la password in chiaro.
        Studente studente = new Studente("Mario", "Rossi", "mario.rossi@studenti.unina.it", "Password123", "N46001234");

        when(gestoreDB.trovaUtentePerEmail("mario.rossi@studenti.unina.it")).thenReturn(studente);

        Utente utenteLoggato = controller.login("mario.rossi@studenti.unina.it", "Password123");

        assertNotNull(utenteLoggato);
        assertEquals("mario.rossi@studenti.unina.it", utenteLoggato.getEmail());
    }

    @Test
    @DisplayName("TC8 Login fallito: utente non trovato")
    void loginUtenteInesistente() {
        when(gestoreDB.trovaUtentePerEmail("inesistente@email.it")).thenReturn(null);

        BusinessException eccezione = assertThrows(BusinessException.class, () -> {
            controller.login("inesistente@email.it", "Password123");
        });

        assertEquals("Utente non trovato o credenziali non corrette", eccezione.getMessage());
    }

    @Test
    @DisplayName("TC9 Login fallito: password errata")
    void loginPasswordErrata() {
        Studente studente = new Studente("Mario", "Rossi", "mario.rossi@studenti.unina.it", "PasswordCorretta", "N46001234");
        when(gestoreDB.trovaUtentePerEmail("mario.rossi@studenti.unina.it")).thenReturn(studente);

        BusinessException eccezione = assertThrows(BusinessException.class, () -> {
            controller.login("mario.rossi@studenti.unina.it", "PasswordSbagliata");
        });

        assertEquals("Utente non trovato o credenziali non corrette", eccezione.getMessage());
    }

    @Test
    @DisplayName("TC10 Login fallito: dati di input vuoti")
    void loginDatiVuoti() {
        BusinessException eccezione = assertThrows(BusinessException.class, () -> {
            controller.login("", "Password123");
        });

        assertEquals("Errore, l'email non può essere vuota!", eccezione.getMessage());
    }
}
