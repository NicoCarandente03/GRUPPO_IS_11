package entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Studente {

    @Id
    private String matricola;

    private String nome;
    private String cognome;
    private String email;
    private String password;
    private int numAccessiTotali;

    @OneToMany(mappedBy = "studente", cascade = CascadeType.ALL) // Relazione 1-a-molti: uno Studente ha una lista di Prenotazioni
    private List<Prenotazione> prenotazioni = new ArrayList<>();

    public Studente() {
    }

    public Studente(String nome, String cognome, String email, String password, String matricola) {
        this.nome=nome;
        this.cognome=cognome;
        this.email=email;
        this.password=password;
        this.matricola=matricola;
        this.numAccessiTotali=0;
        //this.prenotazioni è un ArrayList vuoto (associazione con molteplicità 0..*)
    }

    public String getMatricola() {
        return matricola;
    }

    public void setMatricola(String matricola) {
        this.matricola = matricola;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getNumAccessiTotali() {
        return numAccessiTotali;
    }

    public void setNumAccessiTotali(int numAccessiTotali) {
        this.numAccessiTotali = numAccessiTotali;
    }

    public List<Prenotazione> getPrenotazioni() {
        return prenotazioni;
    }

    public void setPrenotazioni(List<Prenotazione> prenotazioni) {
        this.prenotazioni = prenotazioni;
    }
}
