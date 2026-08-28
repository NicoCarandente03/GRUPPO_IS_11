package entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclasse astratta di Studente e Bibliotecario, come da generalizzazione del
 * modello di dominio.
 *
 * E' annotata MappedSuperclass e non Entity perche' la documentazione di
 * progetto sceglie la traduzione con una tabella per ogni sottoclasse: non esiste
 * una tabella Utente, e gli attributi comuni vengono ripetuti nelle tabelle
 * Studente e Bibliotecario. Ogni sottoclasse dichiara la propria chiave primaria.
 *
 * Conseguenza da tenere presente: l'unicita' dell'email vale dentro ciascuna
 * tabella, non fra le due. Il controllo complessivo lo fa UtenteDAO
 * interrogando entrambe.
 *
 * Gli attributi sono protected, come indica il diagramma delle classi.
 */
@MappedSuperclass
public abstract class Utente {

    /** Valori ammessi per l'attributo ruolo. */
    public static final String RUOLO_STUDENTE = "STUDENTE";
    public static final String RUOLO_BIBLIOTECARIO = "BIBLIOTECARIO";

    protected String nome;
    protected String cognome;

    @Column(unique = true)
    protected String email;

    protected String password;

    private String ruolo;

    /** Costruttore senza argomenti richiesto da JPA. */
    protected Utente() {
    }

    protected Utente(String nome, String cognome, String email, String password, String ruolo) {
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.password = password;
        this.ruolo = ruolo;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getEmail() {
        return email;
    }

    public String getRuolo() {
        return ruolo;
    }

    /**
     * Confronta la password ricevuta con quella dell'utente.
     *
     * Nel diagramma il metodo e' dichiarato void, ma il sequence del Log-in
     * mostra che restituisce un esito booleano.
     */
    public boolean verificaPassword(String password) {
        return this.password != null && this.password.equals(password);
    }

    @Override
    public String toString() {
        return nome + " " + cognome + " (" + email + ")";
    }
}
