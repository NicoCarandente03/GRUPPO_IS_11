package controller;

import java.util.regex.Pattern;

import database.GestorePersistenza;
import eccezioni.BusinessException;
import entity.Utente;
import entity.Studente;
import entity.Bibliotecario;

public class AutenticazioneController {

    //Espressione per validare matematicamente il formato dell'email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    //Variabile per mantenere in memoria l'utente loggato
    private Utente utenteLoggato;

    // 1. Pattern Singleton richiesto dal diagramma UML
    private static AutenticazioneController instance;

    // 2. Collegamento al livello database
    private GestorePersistenza gestoreDB;

    // Costruttore privato
    private AutenticazioneController() {
        this.gestoreDB = new GestorePersistenza();
    }

    public static AutenticazioneController getInstance() {
        if (instance == null) {
            instance = new AutenticazioneController();
        }
        return instance;
    }

    /**
     * Registra un nuovo utente nel sistema dopo aver effettuato il controllo dei dati.
     *
     * L'attributo parametro corrisponde alla matricola (Studente) o al codice identificativo (Bibliotecario)
     */
    public Utente registrazione(String nome, String cognome, String email, String password, String ruolo, String parametro) {

        //Delega la validazione sintattica
        if (!validaDatiSintatticamente(nome, cognome, email, password, ruolo, parametro)) {
            throw new BusinessException("Dati non validi sintatticamente");
        }

        //Delega il controllo duplicati email
        if (!verificaEmailNonRegistrata(email)) {
            throw new BusinessException("Esiste già un account con questa email");
        }

        //Smistamento basato sull'attributo ruolo e controlli specifici delegati
        Utente nuovoUtente;

        if(ruolo.equalsIgnoreCase("Studente")) {
            if(!verificaMatricola(parametro)) {
                throw new BusinessException("Matricola già presente nel sistema");
            }
            nuovoUtente = new Studente(nome, cognome, email, password, parametro);
        } else if (ruolo.equalsIgnoreCase("Bibliotecario")) {
            if(!verificaCodiceIdentificativo(parametro)) {
                throw new BusinessException("Codice identificativo già presente nel sistema");
            }
            nuovoUtente = new Bibliotecario(nome, cognome, email, password, parametro);
        } else {
            throw new BusinessException("Ruolo non riconosciuto: " + ruolo);
        }

        gestoreDB.salva(nuovoUtente);
        return nuovoUtente;
    }

    //Validazione sintattica dei dati
    private boolean validaDatiSintatticamente(String nome, String cognome, String email, String password, String ruolo, String parametro) {
        if (nome == null || nome.isBlank() || cognome == null || cognome.isBlank() || ruolo == null || ruolo.isBlank() || parametro == null || parametro.isBlank()) {
            return false;
        }

        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        if (password == null || password.length() < 6 || password.length() > 20) {
            return false;
        }

        return true; //test superati
    }

    //Controllo duplicati email
    private boolean verificaEmailNonRegistrata(String email) {
        return gestoreDB.trovaUtentePerEmail(email) == null;
    }

    //Controllo duplicate matricola
    private boolean verificaMatricola(String matricola) {
        return gestoreDB.trovaPerMatricola(matricola) == null;
    }

    //Controllo duplicati codiceIdentificativo
    private boolean verificaCodiceIdentificativo(String codiceIdentificativo) {
        return gestoreDB.trovaPerCodiceIdentificativo(codiceIdentificativo) == null;
    }

    /**
     * Effettua l'accesso al sistema di un utente precedentemente registrato
     */
    public Utente login(String email, String password) {

        //Delega il controllo dei dati validi
        verificaDatiValidi(email, password);

        //Interrogazione del DB per cercare l'utente
        Utente utenteTrovato = gestoreDB.trovaUtentePerEmail(email);

        //Delega la verifica delle credenziali direttamente alla classe
        // Utente, che possiede il dato (password) ed è l'esperta che deve
        //esporre il metodo per verificarlo
        if (utenteTrovato == null || !utenteTrovato.verificaCorrispondenzaCredenziali(password)) {
            throw new BusinessException("Utente non trovato o credenziali non corrette"); //eccezione generica per sicurezza
        }

        //Creazione della sessione
        this.utenteLoggato = utenteTrovato;
        return utenteLoggato;
    }

    /**
     * Verifica i dati inseriti, nell'ordine in cui il piano di test elenca le
     * classi di equivalenza: email, password
     */
    private void verificaDatiValidi(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Errore, l'email non può essere vuota!");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("Errore, la password non può essere vuota!");
        }
    }
}