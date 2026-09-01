package entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * Superclasse astratta di Studente e Bibliotecario, come da generalizzazione del
 * modello di dominio.
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

    @Transient
    private String ruolo;

    protected Utente(String ruolo) {
        this.ruolo = ruolo;
    }

    protected Utente(String nome, String cognome, String email, String password, String ruolo) {
        this(ruolo);
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.password = password;
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
     */
    public boolean verificaCorrispondenzaCredenziali(String password) {
        return this.password != null && this.password.equals(password);
    }

    @Override
    public String toString() {
        return nome + " " + cognome + " (" + email + ")";
    }
}
